import java.sql.*;
import java.util.*;

/** B3 - Statement vs PreparedStatement vs batch; auto-commit vs transaction; forced failure.
 *  Run (MySQL + connector JAR needed):  java -cp .:mysql-connector-j.jar LoadBenchmark [url user password] */
public class LoadBenchmark {
    static List<String[]> gen(int n, int badAt) {
        List<String[]> l = new ArrayList<>();
        for (int i = 0; i < n; i++) l.add(new String[]{"P" + i, String.format("%09d", i), "Name" + i, String.valueOf(i % 20), i == badAt ? "abc" : String.valueOf(1000 + i % 500)});
        return l;
    }
    static void clear(Connection c) throws SQLException { try (Statement s = c.createStatement()) { s.execute("DELETE FROM bus_pass"); } }
    static long count(Connection c) throws SQLException { try (Statement s = c.createStatement(); ResultSet r = s.executeQuery("SELECT COUNT(*) FROM bus_pass")) { r.next(); return r.getLong(1); } }
    public static void main(String[] a) throws Exception {
        String url = a.length > 0 ? a[0] : "jdbc:mysql://localhost:3306/campus_test?rewriteBatchedStatements=true", user = a.length > 1 ? a[1] : "root", pwd = a.length > 2 ? a[2] : "password";
        try (Connection c = DriverManager.getConnection(url, user, pwd)) {
            DatabaseMetaData md = c.getMetaData(); System.out.println("Connected to " + md.getDatabaseProductName() + " " + md.getDatabaseProductVersion());
            // B3.1 column types via ResultSetMetaData
            clear(c); PassLoader.loadAll(c, gen(10, -1));
            try (Statement s = c.createStatement(); ResultSet rs = s.executeQuery("SELECT * FROM bus_pass")) {
                ResultSetMetaData m = rs.getMetaData(); for (int i = 1; i <= m.getColumnCount(); i++) System.out.println("  column " + i + ": " + m.getColumnName(i) + " " + m.getColumnTypeName(i));
            }
            System.out.printf("%-10s %14s %18s %16s%n", "rows", "Statement ms", "PreparedStmt ms", "Batch(1000) ms");
            for (int n : new int[]{1_000, 10_000, 100_000}) {
                List<String[]> d = gen(n, -1); long[] t = new long[3];
                clear(c); c.setAutoCommit(true); long t0 = System.nanoTime();
                try (Statement s = c.createStatement()) { for (String[] p : d) s.executeUpdate("INSERT INTO bus_pass VALUES ('" + p[0] + "','" + p[1] + "','" + p[2] + "'," + p[3] + "," + p[4] + ")"); }
                t[0] = (System.nanoTime() - t0) / 1_000_000;
                clear(c); t0 = System.nanoTime();
                try (PreparedStatement ps = c.prepareStatement("INSERT INTO bus_pass VALUES (?,?,?,?,?)")) { for (String[] p : d) { ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]); ps.setInt(4, Integer.parseInt(p[3])); ps.setDouble(5, Double.parseDouble(p[4])); ps.executeUpdate(); } }
                t[1] = (System.nanoTime() - t0) / 1_000_000;
                clear(c); t0 = System.nanoTime(); PassLoader.loadAll(c, d); t[2] = (System.nanoTime() - t0) / 1_000_000;
                System.out.printf("%-10d %14d %18d %16d%n", n, t[0], t[1], t[2]);
            }
            // B3.3 failure half-way: transaction vs auto-commit
            clear(c); try { PassLoader.loadAll(c, gen(2000, 1500)); } catch (RuntimeException | SQLException e) { System.out.println("transaction failed (" + e.getClass().getSimpleName() + "), rows left in table: " + count(c)); }
            clear(c); c.setAutoCommit(true);
            try (PreparedStatement ps = c.prepareStatement("INSERT INTO bus_pass VALUES (?,?,?,?,?)")) { for (String[] p : gen(2000, 1500)) { ps.setString(1, p[0]); ps.setString(2, p[1]); ps.setString(3, p[2]); ps.setInt(4, Integer.parseInt(p[3])); ps.setDouble(5, Double.parseDouble(p[4])); ps.executeUpdate(); } }
            catch (RuntimeException e) { System.out.println("auto-commit failed (" + e.getClass().getSimpleName() + "), rows left in table: " + count(c) + "  <-- half-loaded data"); }
        }
    }
}
