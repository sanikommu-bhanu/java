import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Q3 - Daily sales report: manual close() vs try-with-resources, leaks, suppressed exceptions. */
public class SalesReport {
    static int open = 0;                                    // number of files currently open
    static class TrackedReader extends BufferedReader {
        private boolean closed;
        TrackedReader(String f) throws IOException { super(new FileReader(f)); open++; }
        @Override public void close() throws IOException { if (!closed) { closed = true; open--; } super.close(); }
    }
    static double sum(BufferedReader r, String name) throws IOException {
        double total = 0; String line;
        try { while ((line = r.readLine()) != null) { String[] p = line.split(","); total += Integer.parseInt(p[1].trim()) * Double.parseDouble(p[2].trim()); } }
        catch (NumberFormatException e) { System.out.println("   LOG: failure while reading " + name + " : " + e.getMessage()); throw e; }
        return total;
    }
    // VERSION A - manual close, NOT in finally: all files opened first, close() skipped when an exception is thrown (leak)
    static double leaky(List<String> files) throws IOException {
        List<TrackedReader> rs = new ArrayList<>(); for (String f : files) rs.add(new TrackedReader(f));
        double total = 0; for (int i = 0; i < rs.size(); i++) total += sum(rs.get(i), files.get(i));
        for (TrackedReader r : rs) r.close();
        return total;
    }
    // VERSION B - try-catch-finally with manual close (correct but verbose)
    static double manualFinally(List<String> files) throws IOException {
        List<TrackedReader> rs = new ArrayList<>(); double total = 0;
        try { for (String f : files) rs.add(new TrackedReader(f)); for (int i = 0; i < rs.size(); i++) total += sum(rs.get(i), files.get(i)); }
        finally { for (TrackedReader r : rs) { try { r.close(); } catch (IOException ignored) { } } }
        return total;
    }
    // VERSION C - try-with-resources, one file at a time
    static double twr(List<String> files) throws IOException {
        double total = 0;
        for (String f : files) { try (TrackedReader r = new TrackedReader(f)) { total += sum(r, f); } }
        return total;
    }
    static class Flaky implements AutoCloseable {
        final String name; Flaky(String n) { name = n; }
        @Override public void close() throws IOException { throw new IOException("close() failed for " + name); }
    }
    interface Report { double run(List<String> f) throws IOException; }
    static void attempt(String label, Report r, List<String> files) {
        open = 0;
        try { System.out.println(label + " -> total " + r.run(files)); } catch (Exception e) { System.out.println(label + " -> FAILED with " + e.getClass().getSimpleName()); }
        System.out.println("   files still open after the run: " + open);
    }
    public static void main(String[] args) throws Exception {
        Path dir = Files.createTempDirectory("stores");
        Path s1 = dir.resolve("store1.csv"), s2 = dir.resolve("store2.csv"), s3 = dir.resolve("store3.csv"), bad = dir.resolve("store2_bad.csv");
        Files.writeString(s1, "pen,10,5.5\nbook,2,100\n"); Files.writeString(s2, "bag,1,800\n"); Files.writeString(s3, "ink,4,25\n");
        Files.writeString(bad, "bag,ONE,800\n");
        List<String> good = List.of(s1.toString(), s2.toString(), s3.toString()), failing = List.of(s1.toString(), bad.toString(), s3.toString());
        System.out.println("=== same good input: all three versions must agree ===");
        attempt("A leaky       ", SalesReport::leaky, good); attempt("B finally     ", SalesReport::manualFinally, good); attempt("C try-with-res", SalesReport::twr, good);
        System.out.println("=== failing input (file 2 is corrupt) ===");
        attempt("A leaky       ", SalesReport::leaky, failing); attempt("B finally     ", SalesReport::manualFinally, failing); attempt("C try-with-res", SalesReport::twr, failing);
        System.out.println("=== suppressed exception (body fails AND close() fails) ===");
        try (Flaky a = new Flaky("res-A"); Flaky b = new Flaky("res-B")) { throw new IllegalStateException("body failed"); }
        catch (Exception e) {
            System.out.println("primary  : " + e);
            for (Throwable t : e.getSuppressed()) System.out.println("suppressed: " + t);
            System.out.println("(resources are closed in REVERSE order: res-B first, then res-A)");
        }
        for (Path p : List.of(s1, s2, s3, bad)) Files.delete(p); Files.delete(dir);
    }
}
