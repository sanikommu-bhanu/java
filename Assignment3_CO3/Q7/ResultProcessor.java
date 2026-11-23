import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.LockSupport;

/**
 * Q7 - Concurrent, fault-tolerant semester result processor.
 * REQUIREMENTS (measurable): R1 process 10,000 files; R2 a corrupt/missing file never stops the run; R3 summary: success + corrupt + missing = 10,000;
 *   R4 every failure is listed with its file number and reason; R5 report total time for pools of 1, 2, 4, 8 threads.
 * PARALLEL TASKS: one task per file (independent). SHARED DATA: results map + failure map -> ConcurrentHashMap (lock-striped, no global lock).
 * FAILURES: missing file -> NoSuchFileException (checked, IOException); corrupt content -> BadScoreException (checked, user-defined).
 * DESIGN:  ExecutorService(p) + Callable<Double> per file -> Future; main thread calls future.get(); ExecutionException.getCause() classifies the
 *          failure, so ONE failing task only fails its own Future, never the pool.
 *   PSEUDOCODE:  FOR each file i: futures[i] <- pool.submit(Callable{ read(i); validate(i); RETURN average })
 *                FOR each future: TRY value <- future.get(); results.put(i, value) CATCH ExecutionException e: failures.put(i, cause(e)) END
 *                PRINT counts
 */
public class ResultProcessor {
    static class BadScoreException extends Exception { BadScoreException(String m) { super(m); } }
    static final int LATENCY_NANOS = 100_000;                         // simulated per-file work (network/disk wait)

    static double process(Path dir, int i) throws IOException, BadScoreException {
        List<String> lines = Files.readAllLines(dir.resolve("score" + i + ".txt"));        // NoSuchFileException if missing
        LockSupport.parkNanos(LATENCY_NANOS);
        String[] f = lines.get(0).split(",");
        if (f.length != 4) throw new BadScoreException("expected 4 fields, found " + f.length);
        double sum = 0;
        for (int k = 1; k < f.length; k++) {
            try { double v = Double.parseDouble(f[k].trim()); if (v < 0 || v > 100) throw new BadScoreException("score out of range: " + v); sum += v; }
            catch (NumberFormatException e) { throw new BadScoreException("non-numeric score '" + f[k] + "'"); }
        }
        return sum / 3;
    }
    record Summary(int success, int corrupt, int missing, int other, long ms) {}

    static Summary run(Path dir, int n, int threads) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        ConcurrentHashMap<Integer, Double> results = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, String> failures = new ConcurrentHashMap<>();
        List<Future<Double>> fs = new ArrayList<>(n); long t0 = System.nanoTime();
        for (int i = 0; i < n; i++) { final int id = i; fs.add(pool.submit(() -> process(dir, id))); }
        int missing = 0, corrupt = 0, other = 0;
        for (int i = 0; i < n; i++) {
            try { results.put(i, fs.get(i).get()); }
            catch (ExecutionException e) {
                Throwable c = e.getCause();
                if (c instanceof NoSuchFileException) { missing++; failures.put(i, "missing"); }
                else if (c instanceof BadScoreException) { corrupt++; failures.put(i, "corrupt: " + c.getMessage()); }
                else { other++; failures.put(i, "other: " + c); }
            }
        }
        pool.shutdown(); long ms = (System.nanoTime() - t0) / 1_000_000;
        return new Summary(results.size(), corrupt, missing, other, ms);
    }
    static Path makeFiles(int n) throws IOException {
        Path dir = Files.createTempDirectory("scores");
        for (int i = 0; i < n; i++) {
            if (i % 101 == 0) continue;                                                    // missing
            String body = (i % 97 == 0) ? "REG" + i + ",ab,70,80" : "REG" + i + "," + (50 + i % 50) + "," + (40 + i % 60) + "," + (30 + i % 70);   // corrupt every 97th
            Files.writeString(dir.resolve("score" + i + ".txt"), body);
        }
        return dir;
    }
    static void delete(Path dir) throws IOException { try (var s = Files.list(dir)) { for (Path p : (Iterable<Path>) s::iterator) Files.delete(p); } Files.delete(dir); }

    public static void main(String[] args) throws Exception {
        // ---- small hand-checked set: 10 files: 0 missing, 3 corrupt (non-numeric / 3 fields / out of range), rest valid ----
        Path small = Files.createTempDirectory("small");
        String[] body = { null, "R1,80,90,100", "R2,50,60,70", "R3,xx,60,70", "R4,10,20", "R5,100,100,100", "R6,30,30,30", "R7,70,80,90", "R8,120,50,50", "R9,0,0,0" };
        for (int i = 0; i < body.length; i++) if (body[i] != null) Files.writeString(small.resolve("score" + i + ".txt"), body[i]);
        Summary s = run(small, body.length, 2);
        System.out.println("Hand-checked set of 10 files: expected success=6 (files 1,2,5,6,7,9), corrupt=3 (files 3,4,8), missing=1 (file 0)");
        System.out.println("Program summary            : success=" + s.success + " corrupt=" + s.corrupt + " missing=" + s.missing + " other=" + s.other + "  -> " + (s.success == 6 && s.corrupt == 3 && s.missing == 1 ? "MATCH" : "MISMATCH"));
        delete(small);

        int n = 10_000; Path dir = makeFiles(n);
        int expMissing = 0, expCorrupt = 0; for (int i = 0; i < n; i++) { if (i % 101 == 0) expMissing++; else if (i % 97 == 0) expCorrupt++; }
        System.out.println("10,000 files: expected missing=" + expMissing + ", corrupt=" + expCorrupt + ", success=" + (n - expMissing - expCorrupt));
        System.out.printf("%-10s %9s %9s %9s %9s %10s%n", "threads", "success", "corrupt", "missing", "other", "time (ms)");
        for (int p : new int[]{1, 2, 4, 8}) {
            Summary r = run(dir, n, p);
            System.out.printf("%-10d %9d %9d %9d %9d %10d   %s%n", p, r.success, r.corrupt, r.missing, r.other, r.ms,
                    (r.success == n - expMissing - expCorrupt && r.corrupt == expCorrupt && r.missing == expMissing) ? "summary correct" : "SUMMARY WRONG");
        }
        // alternative: parallel stream
        long t0 = System.nanoTime(); java.util.concurrent.atomic.AtomicInteger ok = new java.util.concurrent.atomic.AtomicInteger(), bad = new java.util.concurrent.atomic.AtomicInteger();
        java.util.stream.IntStream.range(0, n).parallel().forEach(i -> { try { process(dir, i); ok.incrementAndGet(); } catch (Exception e) { bad.incrementAndGet(); } });
        System.out.println("parallelStream (common pool, " + ForkJoinPool.commonPool().getParallelism() + " worker thread(s) on this machine): ok=" + ok + " failed=" + bad + " in " + (System.nanoTime() - t0) / 1_000_000 + " ms");
        System.out.println("Thread-per-file would need 10,000 threads x ~1 MB stack = ~10 GB of address space: rejected. Pool of p threads uses p x stack only.");
        System.out.println("Limitation: results live in memory; a crash mid-run loses them (a checkpoint file/DB would fix this).");
        delete(dir);
    }
}
