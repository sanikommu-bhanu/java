import java.util.*;

/**
 * Q1 - String vs StringBuilder vs StringBuffer for railway e-ticket generation.
 * PREDICTION (made before running):
 *  + on String : each concatenation creates a NEW String (immutability) and copies all earlier characters
 *                -> O(n^2) chars copied for n fields; slow for long tickets, almost no difference for 5-10 fields.
 *  StringBuilder: one growing char buffer -> O(n). StringBuffer: same but every append is synchronized -> slightly slower
 *                single-threaded (JIT may remove most of the cost via lock elision).
 */
public class StringBench {
    static String viaPlus(String[] f) { String s = ""; for (String x : f) s = s + x + "|"; return s; }
    static String viaBuilder(String[] f) { StringBuilder sb = new StringBuilder(); for (String x : f) sb.append(x).append('|'); return sb.toString(); }
    static String viaBuffer(String[] f) { StringBuffer sb = new StringBuffer(); for (String x : f) sb.append(x).append('|'); return sb.toString(); }
    static String viaJoin(String[] f) { return String.join("|", f) + "|"; }
    static String viaJoiner(String[] f) { StringJoiner j = new StringJoiner("|", "", "|"); for (String x : f) j.add(x); return j.toString(); }

    interface Builder { String build(String[] f); }
    static String[] fields(int n) { String[] f = new String[n]; for (int i = 0; i < n; i++) f[i] = "FIELD" + i + "=value" + i; return f; }
    static long timeMs(Builder b, String[] f, int tickets, int repeats) {
        long best = 0, sum = 0;
        for (int r = 0; r < repeats; r++) {
            long t0 = System.nanoTime(); int len = 0;
            for (int t = 0; t < tickets; t++) len += b.build(f).length();
            sum += System.nanoTime() - t0; if (len == 0) System.out.print("");
        }
        return sum / repeats / 1_000_000;
    }
    static long usedKb() { Runtime r = Runtime.getRuntime(); return (r.totalMemory() - r.freeMemory()) / 1024; }

    public static void main(String[] args) throws Exception {
        int[][] workloads = {{10, 20_000}, {50, 10_000}, {2000, 100}};   // {fields per ticket, tickets}
        String[] names = {"W1: short ticket (10 fields) x 20,000", "W2: 50 fields x 10,000 tickets", "W3: very long ticket (2,000 fields) x 100"};
        // warm-up
        for (int i = 0; i < 3; i++) { viaPlus(fields(50)); viaBuilder(fields(50)); viaBuffer(fields(50)); }
        System.out.printf("%-44s %10s %10s %10s %12s%n", "Workload (avg of 5 runs, ms)", "String +", "Builder", "Buffer", "est. temp Strings(+)");
        for (int w = 0; w < workloads.length; w++) {
            String[] f = fields(workloads[w][0]); int tickets = workloads[w][1];
            long a = timeMs(StringBench::viaPlus, f, tickets, 5), b = timeMs(StringBench::viaBuilder, f, tickets, 5), c = timeMs(StringBench::viaBuffer, f, tickets, 5);
            System.out.printf("%-44s %10d %10d %10d %12d%n", names[w], a, b, c, (long) f.length * 2 * tickets);
        }
        // memory before / after for W3 with +
        String[] f = fields(2000); System.gc(); long before = usedKb();
        long churn = 0; for (int i = 0; i < 20; i++) churn += viaPlus(f).length();
        long after = usedKb();
        System.out.println("Heap used before/after 20 long tickets built with + : " + before + " KB / " + after + " KB (garbage created, churn chars=" + churn + ")");
        System.out.println("Alternatives, W2 (ms): join=" + timeMs(StringBench::viaJoin, fields(50), 10_000, 5) + " StringJoiner=" + timeMs(StringBench::viaJoiner, fields(50), 10_000, 5)
                + " format=" + timeFormat());

        // ---- Shared booking log: thread safety ----
        final int threads = 4, perThread = 1_000_000;
        System.out.println("Shared log, " + threads + " threads x " + perThread + " appends of 'x' (expected length " + threads * perThread + "):");
        for (int run = 1; run <= 3; run++) {
            System.out.println("  run " + run + " StringBuilder -> " + runShared(new StringBuilder(), threads, perThread));
            System.out.println("  run " + run + " StringBuffer  -> " + runShared(new StringBuffer(), threads, perThread));
        }
        System.out.println("Recommendation: per-ticket text (one thread owns it) = StringBuilder; shared log = StringBuffer (or better: a synchronized/concurrent logger).");
    }
    static long timeFormat() {
        String[] f = fields(50); long t0 = System.nanoTime(); int len = 0;
        for (int i = 0; i < 2000; i++) { String s = ""; for (String x : f) s += String.format("%s|", x); len += s.length(); }   // format per field (slow style)
        return (System.nanoTime() - t0) / 1_000_000 * 5 + 0 * len;      // scaled to 10,000 tickets
    }
    static String runShared(Appendable log, int threads, int per) throws Exception {
        List<Thread> ts = new ArrayList<>(); List<String> errors = Collections.synchronizedList(new ArrayList<>());
        for (int i = 0; i < threads; i++) { Thread t = new Thread(() -> { try { for (int k = 0; k < per; k++) log.append('x'); } catch (Throwable e) { errors.add(e.getClass().getSimpleName()); } }); ts.add(t); t.start(); }
        for (Thread t : ts) t.join();
        return "length " + ((CharSequence) log).length() + (errors.isEmpty() ? "" : "  (exceptions: " + new HashSet<>(errors) + ")");
    }
}
