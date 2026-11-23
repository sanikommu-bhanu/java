import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** B4 - migrate the bus-pass text file into MySQL with batches + one transaction; report failed records; print DatabaseMetaData.
 *  Usage: java -cp .:mysql-connector-j.jar Migration <passes.csv> [url user password]   (no database reachable -> dry run: validation + failure report only) */
public class Migration {
    static String validate(String[] f) {
        if (f.length != 5) return "expected 5 fields, found " + f.length;
        if (f[0].isBlank() || !f[1].matches("\\d{9}")) return "invalid pass id or register number";
        try { Integer.parseInt(f[3]); } catch (NumberFormatException e) { return "route not an integer"; }
        try { if (Double.parseDouble(f[4]) < 0) return "negative amount"; } catch (NumberFormatException e) { return "amount not numeric"; }
        return null;
    }
    public static void main(String[] args) throws Exception {
        Path src;
        if (args.length > 0) src = Path.of(args[0]);
        else {                                                      // demo data: 8 lines, 3 bad
            src = Files.createTempFile("passes", ".csv");
            Files.write(src, List.of("P1,192311001,Arun,12,4500", "P2,192311002,Divya,7,3800", "P3,BAD,Kiran,5,100", "P4,192311004,Meena,x,100", "P5,192311005,Ravi,3", "",
                    "P7,192311007,Asha,9,2500", "P8,192311008,Omar,2,-5"));
        }
        List<String[]> good = new ArrayList<>(); List<String> failed = new ArrayList<>(); int lineNo = 0;
        try (BufferedReader r = Files.newBufferedReader(src)) {
            String line; while ((line = r.readLine()) != null) { lineNo++; String[] f = line.split(",", -1); String err = line.isBlank() ? "empty line" : validate(f); if (err == null) good.add(f); else failed.add("line " + lineNo + ": " + err + "  [" + line + "]"); }
        }
        System.out.println("Read " + lineNo + " lines: " + good.size() + " valid, " + failed.size() + " failed validation");
        for (String s : failed) System.out.println("  FAILED " + s);
        String url = args.length > 1 ? args[1] : "jdbc:mysql://localhost:3306/campus_test", user = args.length > 2 ? args[2] : "root", pwd = args.length > 3 ? args[3] : "password";
        try (Connection c = DriverManager.getConnection(url, user, pwd)) {
            int loaded = PassLoader.loadAll(c, good);
            System.out.println("Loaded " + loaded + " rows in one transaction (batches of 1,000).");
            DatabaseMetaData md = c.getMetaData();
            System.out.println("Database : " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            System.out.println("Driver   : " + md.getDriverName() + " " + md.getDriverVersion());
            try (ResultSet t = md.getTables(c.getCatalog(), null, "%", new String[]{"TABLE"})) { StringBuilder sb = new StringBuilder(); while (t.next()) sb.append(t.getString("TABLE_NAME")).append(' '); System.out.println("Tables   : " + sb); }
        } catch (SQLException e) {
            System.out.println("DRY RUN - database not reachable (" + e.getClass().getSimpleName() + "): " + good.size() + " valid rows would be loaded in one transaction.");
        }
        System.out.println("Metadata is useful when moving servers: it confirms the product, driver version and that the expected tables exist before loading.");
    }
}
