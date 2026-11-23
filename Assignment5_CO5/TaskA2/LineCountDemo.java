import java.io.*;
import java.nio.file.*;

/** A2 - counts the read() calls that reach the underlying stream (system-call proxy) and the time, for unbuffered vs buffered. */
public class LineCountDemo {
    static class Counting extends FilterInputStream {
        long calls = 0; Counting(InputStream in) { super(in); }
        @Override public int read() throws IOException { calls++; return super.read(); }
        @Override public int read(byte[] b, int o, int l) throws IOException { calls++; return super.read(b, o, l); }
    }
    public static void main(String[] args) throws Exception {
        Path f = Files.createTempFile("log", ".txt");
        try (BufferedWriter w = Files.newBufferedWriter(f)) { for (int i = 0; i < 100_000; i++) w.write("2026-01-01 10:00:00 student " + i + " present\n"); }
        long size = Files.size(f); System.out.println("File size: " + size + " bytes");
        long t0 = System.nanoTime(); Counting c1 = new Counting(new FileInputStream(f.toFile())); long lines1 = 0; int b;
        while ((b = c1.read()) != -1) if (b == '\n') lines1++; c1.close();
        System.out.printf("Unbuffered read()      : lines=%d, read calls=%,d, time=%d ms%n", lines1, c1.calls, (System.nanoTime() - t0) / 1_000_000);
        t0 = System.nanoTime(); Counting c2 = new Counting(new FileInputStream(f.toFile())); long lines2 = 0;
        try (BufferedInputStream bi = new BufferedInputStream(c2)) { while ((b = bi.read()) != -1) if (b == '\n') lines2++; }
        System.out.printf("BufferedInputStream    : lines=%d, calls to the file stream=%,d, time=%d ms%n", lines2, c2.calls, (System.nanoTime() - t0) / 1_000_000);
        t0 = System.nanoTime(); long lines3 = 0, calls3 = 0; byte[] buf = new byte[8192];
        try (InputStream in = new FileInputStream(f.toFile())) { int n; while ((n = in.read(buf)) != -1) { calls3++; for (int i = 0; i < n; i++) if (buf[i] == '\n') lines3++; } }
        System.out.printf("byte[8192] buffer      : lines=%d, read calls=%,d, time=%d ms%n", lines3, calls3, (System.nanoTime() - t0) / 1_000_000);
        t0 = System.nanoTime(); long lines4 = Files.readAllLines(f).size();
        System.out.printf("Files.readAllLines     : lines=%d, memory ~ O(file size) (all lines kept), time=%d ms%n", lines4, (System.nanoTime() - t0) / 1_000_000);
        System.out.println("Predicted calls for the unbuffered version = file size (" + size + "); buffered = size/8192 = " + (size / 8192 + 1));
        Files.delete(f);
    }
}
