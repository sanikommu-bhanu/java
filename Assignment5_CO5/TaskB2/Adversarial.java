import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** B2 - adversarial inputs. Part 1 (file store) always runs. Part 2 (login) needs the local MySQL test DB (schema in schema.sql); otherwise it prints the SQL it WOULD send. */
public class Adversarial {
    // ---- Part 1: strict parser for the bus-pass file ----
    static String parse(String line) {
        if (line.isBlank()) return "REJECTED: empty line";
        String[] f = line.split(",", -1);
        if (f.length != 5) return "REJECTED: expected 5 fields, found " + f.length;
        try { double a = Double.parseDouble(f[4]); if (a < 0) return "REJECTED: negative amount"; } catch (NumberFormatException e) { return "REJECTED: amount '" + f[4] + "' is not numeric"; }
        try { Integer.parseInt(f[3]); } catch (NumberFormatException e) { return "REJECTED: route '" + f[3] + "' is not an integer"; }
        return "ACCEPTED: " + f[0];
    }
    // ---- Part 2: unsafe vs safe login ----
    static boolean unsafeLogin(Connection c, String id, String pwd) throws SQLException {
        String sql = "SELECT * FROM users WHERE id='" + id + "' AND pwd='" + pwd + "'";
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(sql)) { return rs.next(); }
    }
    static boolean safeLogin(Connection c, String id, String pwd) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement("SELECT 1 FROM users WHERE id = ? AND pwd = ?")) {
            ps.setString(1, id); ps.setString(2, pwd); try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
    public static void main(String[] args) {
        System.out.println("=== Part 1: damaged bus-pass lines ===");
        String[] lines = { "P1,192311001,Arun,12,4500.0", "P2,192311002,Divya,7", "P3,192311003,Kiran,5,abc", "", "P5,192311005,Meena,x,100", "P6,192311006,Ravi,3,-50" };
        for (String l : lines) System.out.printf("%-34s -> %s%n", "'" + l + "'", parse(l));
        System.out.println("Reveals: a field-count check alone (B1) is NOT enough - non-numeric/negative values pass it; validate types and ranges too.");

        System.out.println("=== Part 2: login injection ===");
        String[][] attempts = { {"arun", "wrong"}, {"admin'--", "anything"}, {"nobody", "' OR '1'='1"} };
        try (Connection c = DriverManager.getConnection(System.getProperty("url", "jdbc:mysql://localhost:3306/campus_test"), System.getProperty("user", "root"), System.getProperty("pwd", "password"))) {
            for (String[] a : attempts) System.out.printf("id=%-12s pwd=%-14s unsafe -> %-5s | prepared -> %s%n", a[0], a[1], unsafeLogin(c, a[0], a[1]), safeLogin(c, a[0], a[1]));
        } catch (SQLException e) {
            System.out.println("(MySQL test database not reachable here: " + e.getClass().getSimpleName() + ") - showing the statements that would be sent:");
            for (String[] a : attempts) System.out.println("unsafe: SELECT * FROM users WHERE id='" + a[0] + "' AND pwd='" + a[1] + "'");
            System.out.println("safe  : SELECT 1 FROM users WHERE id = ? AND pwd = ?   with the three values bound as plain strings");
        }
        System.out.println("Escaping quotes / regex validation instead of PreparedStatement is weaker: every dialect and encoding has its own tricks, one missed character = injection;");
        System.out.println("validation costs O(length) per query on top, while PreparedStatement needs no inspection of the values at all.");
    }
}
