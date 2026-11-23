import java.io.*;
import java.nio.file.*;
import java.util.*;

/** A1 - text vs binary record layouts, damaged-line handling, and the RandomAccessFile alternative. */
public class RecordLayouts {
    static String esc(String s) { return s.replace("\\", "\\\\").replace(",", "\\,"); }
    static List<String> split(String line) {                      // split on commas that are not escaped
        List<String> out = new ArrayList<>(); StringBuilder cur = new StringBuilder();
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '\\' && i + 1 < line.length()) cur.append(line.charAt(++i));
            else if (c == ',') { out.add(cur.toString()); cur.setLength(0); } else cur.append(c);
        }
        out.add(cur.toString()); return out;
    }
    static String[] find(Path f, String id) throws IOException {
        if (!Files.exists(f)) return null;
        try (BufferedReader r = Files.newBufferedReader(f)) {
            String line; int n = 0;
            while ((line = r.readLine()) != null) {
                n++; List<String> p = split(line);
                try { if (p.size() != 7) throw new IllegalArgumentException("fields=" + p.size()); Double.parseDouble(p.get(6)); }
                catch (RuntimeException e) { System.out.println("   LOG damaged line " + n + ": " + e.getMessage()); continue; }
                if (p.get(0).equals(id)) return p.toArray(new String[0]);
            }
        }
        return null;
    }
    public static void main(String[] args) throws Exception {
        Path text = Files.createTempFile("passes", ".csv"), bin = Files.createTempFile("passes", ".bin"), ra = Files.createTempFile("passes", ".dat");
        String[][] recs = { {"P1001", "192311001", "Arun", "12", "Main Gate", "2026-12-31", "4500.0"},
                            {"P1002", "192311002", "Smith, John", "7", "Library", "2026-12-31", "3800.0"} };      // name contains the delimiter
        try (BufferedWriter w = Files.newBufferedWriter(text)) {
            for (String[] r : recs) { StringJoiner j = new StringJoiner(","); for (String f : r) j.add(esc(f)); w.write(j.toString()); w.newLine(); }
            w.write("P1003,BROKEN"); w.newLine();                                        // damaged line
        }
        System.out.println("--- text file content ---"); for (String l : Files.readAllLines(text)) System.out.println(l);
        System.out.println("find P1002 -> " + Arrays.toString(find(text, "P1002")));
        System.out.println("find P1003 (damaged) -> " + Arrays.toString(find(text, "P1003")));
        System.out.println("find P9999 -> " + Arrays.toString(find(text, "P9999")));
        System.out.println("find in missing file -> " + Arrays.toString(find(Path.of("no-such-file.csv"), "P1")));

        try (DataOutputStream o = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(bin)))) {
            for (String[] r : recs) { o.writeUTF(r[0]); o.writeUTF(r[1]); o.writeUTF(r[2]); o.writeInt(Integer.parseInt(r[3])); o.writeUTF(r[4]); o.writeLong(java.time.LocalDate.parse(r[5]).toEpochDay()); o.writeDouble(Double.parseDouble(r[6])); }
        }
        try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(bin)))) {
            while (in.available() > 0) System.out.println("binary record: " + in.readUTF() + " " + in.readUTF() + " " + in.readUTF() + " route " + in.readInt() + " " + in.readUTF() + " " + java.time.LocalDate.ofEpochDay(in.readLong()) + " Rs" + in.readDouble());
        }
        System.out.println("text bytes=" + Files.size(text) + ", binary bytes=" + Files.size(bin));

        // Alternative: fixed-length records, record size 64 bytes: id(8) reg(10) name(30) route(4) amount(8) padding(4)
        final int SIZE = 64;
        try (RandomAccessFile f = new RandomAccessFile(ra.toFile(), "rw")) {
            for (int i = 0; i < 1000; i++) { f.seek((long) i * SIZE); byte[] b = new byte[SIZE]; byte[] id = String.format("P%04d", i).getBytes(); System.arraycopy(id, 0, b, 0, id.length); f.write(b); }
            f.seek(777L * SIZE); byte[] got = new byte[8]; f.readFully(got);
            System.out.println("RandomAccessFile: record 777 read with ONE seek (O(1)) -> " + new String(got).trim() + "; file size " + f.length() + " bytes, padding wasted per record is up to " + (SIZE - 33) + " bytes");
        }
        Files.delete(text); Files.delete(bin); Files.delete(ra);
    }
}
