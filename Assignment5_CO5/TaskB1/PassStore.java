import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

public class PassStore {
    private final Path file;
    PassStore(Path file) { this.file = file; }

    public void add(String passId, String regNo, String name, int route, double amount)
            throws IOException {
        try (BufferedWriter w = Files.newBufferedWriter(file,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            w.write(String.join(",", passId, regNo, name,
                    String.valueOf(route), String.valueOf(amount)));
            w.newLine();
        }
    }

    /** Returns the five fields of the pass with the given ID, or null if not found. */
    public String[] find(String passId) throws IOException {
        // TODO 1: Read the file line by line with a BufferedReader inside try-with-
        // resources; skip damaged lines (not exactly 5 fields); return the fields of
        // the matching pass
        try (BufferedReader r = Files.newBufferedReader(file)) {
            String line;
            while ((line = r.readLine()) != null) {
                String[] f = line.split(",", -1);
                if (f.length != 5) continue;              // damaged line: skipped, search continues
                if (f[0].equals(passId)) return f;
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException {
        Path f = Files.createTempFile("passes", ".csv");
        PassStore store = new PassStore(f);
        store.add("P1001", "192311001", "Arun", 12, 4500);
        Files.writeString(f, "P1002,DAMAGED LINE\n", StandardOpenOption.APPEND);
        store.add("P1003", "192311003", "Divya", 7, 3800);
        System.out.println(Arrays.toString(store.find("P1003")));
        System.out.println(Arrays.toString(store.find("P1002")));
        Files.delete(f);
    }
}

/** B1.1 test table + B1.3 buffering measurements + B1.4 growth + HashMap alternative. Run: java PassStoreBench */
class PassStoreBench {
    static long countUnbuffered(Path p) throws IOException { long n = 0; try (java.io.FileInputStream in = new java.io.FileInputStream(p.toFile())) { int b; while ((b = in.read()) != -1) if (b == '\n') n++; } return n; }
    static long countBuffered(Path p) throws IOException { long n = 0; try (java.io.BufferedInputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(p.toFile()), 65536)) { int b; while ((b = in.read()) != -1) if (b == '\n') n++; } return n; }
    static Path makeFile(int mb) throws IOException {
        Path p = Files.createTempFile("log", ".txt"); byte[] line = "2026-01-01 10:00:00 student 192311001 present\n".getBytes();
        try (java.io.OutputStream o = new java.io.BufferedOutputStream(Files.newOutputStream(p))) { for (long w = 0; w < mb * 1024L * 1024L; w += line.length) o.write(line); }
        return p;
    }
    public static void main(String[] args) throws Exception {
        Path f = Files.createTempFile("passes", ".csv"); PassStore s = new PassStore(f);
        s.add("P1001", "192311001", "Arun", 12, 4500); Files.writeString(f, "P1002,DAMAGED LINE\n\nP1004,1,x,notanumber\n", StandardOpenOption.APPEND); s.add("P1003", "192311003", "Divya", 7, 3800);
        System.out.println("B1.1 test table");
        System.out.println(" normal   find P1003 -> " + Arrays.toString(s.find("P1003")) + "   (verifies add + find skip over damaged lines)");
        System.out.println(" missing  find P9999 -> " + Arrays.toString(s.find("P9999")) + "   (verifies not-found returns null, no exception)");
        System.out.println(" damaged  find P1002 -> " + Arrays.toString(s.find("P1002")) + "   (2-field line skipped, never returned)");
        System.out.println(" empty line is skipped (1 field); 'P1004,1,x,notanumber' has 4 fields -> skipped -> find P1004 -> " + Arrays.toString(s.find("P1004")));
        Files.delete(f);

        System.out.println("B1.3 line counting (ms)      size   unbuffered   buffered");
        for (int mb : new int[]{1, 10, 50}) {
            Path p = makeFile(mb); long t0 = System.nanoTime(); countBuffered(p); long tb = (System.nanoTime() - t0) / 1_000_000;
            String tu; if (mb <= 10) { t0 = System.nanoTime(); countUnbuffered(p); tu = String.valueOf((System.nanoTime() - t0) / 1_000_000); }
            else { tu = "~" + (unbufPerMb * 50) + " (extrapolated from 10 MB)"; }
            if (mb == 1) unbufPerMb = Long.parseLong(tu); if (mb == 10) unbufPerMb = Long.parseLong(tu) / 10;
            System.out.printf("                        %5d MB   %10s   %8d%n", mb, tu, tb); Files.delete(p);
        }
        System.out.println("(prediction A2.1: one read() per byte => ~1 million / 10 million / 52 million calls; buffered = size/64 KB calls)");

        System.out.println("B1.4 find() growth: time to find the LAST pass (average of 20 searches)");
        PassStore g; HashMapStore hm = null;
        for (int n : new int[]{1_000, 10_000, 50_000, 100_000}) {
            Path p = Files.createTempFile("grow", ".csv"); g = new PassStore(p);
            try (BufferedWriter w = Files.newBufferedWriter(p)) { for (int i = 0; i < n; i++) { w.write("P" + i + ",19231" + i + ",Name" + i + "," + (i % 20) + ",100.0"); w.newLine(); } }
            long t0 = System.nanoTime(); for (int k = 0; k < 20; k++) g.find("P" + (n - 1)); long scan = (System.nanoTime() - t0) / 20 / 1000;
            long t1 = System.nanoTime(); java.util.Map<String, String[]> map = new java.util.HashMap<>();
            try (BufferedReader r = Files.newBufferedReader(p)) { String line; while ((line = r.readLine()) != null) { String[] x = line.split(","); map.put(x[0], x); } }
            long load = (System.nanoTime() - t1) / 1000; t1 = System.nanoTime(); for (int k = 0; k < 20; k++) map.get("P" + (n - 1)); long look = (System.nanoTime() - t1) / 20 / 1000;
            System.out.printf("  n=%,7d records: file scan %,8d us per find (O(n));  HashMap: one-time load %,8d us, then %d us per find (O(1))%n", n, scan, load, look);
            Files.delete(p);
        }
    }
    static long unbufPerMb = 0;
    static class HashMapStore { }
}
