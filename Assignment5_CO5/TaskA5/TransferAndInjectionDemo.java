import java.util.*;

/** A5 - (1) simulated wallets: failure between two updates with auto-commit vs one transaction; (2) the SQL text produced by injection. JDBC version: see pseudocode. */
public class TransferAndInjectionDemo {
    static Map<String, Integer> wallets = new LinkedHashMap<>(Map.of("hostel", 1000, "canteen", 200));
    static void update(String w, int delta, boolean fail) { if (fail) throw new IllegalStateException("crash before second update"); wallets.merge(w, delta, Integer::sum); }

    static void autoCommit(boolean crash) {                     // each statement is permanent immediately
        update("hostel", -500, false);
        update("canteen", +500, crash);
    }
    static void transaction(boolean crash) {                    // snapshot = what rollback restores
        Map<String, Integer> before = new LinkedHashMap<>(wallets);
        try { update("hostel", -500, false); update("canteen", +500, crash); }
        catch (RuntimeException e) { wallets = before; throw e; }          // rollback
    }
    public static void main(String[] args) {
        System.out.println("start: " + wallets + " total=" + wallets.values().stream().mapToInt(i -> i).sum());
        try { autoCommit(true); } catch (RuntimeException e) { System.out.println("AUTO-COMMIT, failure between updates: " + wallets + " total=" + wallets.values().stream().mapToInt(i -> i).sum() + "   <-- Rs 500 vanished"); }
        wallets = new LinkedHashMap<>(Map.of("hostel", 1000, "canteen", 200));
        try { transaction(true); } catch (RuntimeException e) { System.out.println("TRANSACTION, failure between updates : " + wallets + " total=" + wallets.values().stream().mapToInt(i -> i).sum() + "   <-- rolled back, nothing lost"); }
        transaction(false); System.out.println("TRANSACTION, no failure              : " + wallets + " total=" + wallets.values().stream().mapToInt(i -> i).sum());

        String[][] logins = { {"arun", "secret"}, {"admin'--", "x"}, {"nobody", "' OR '1'='1"} };
        for (String[] l : logins) {
            String unsafe = "SELECT * FROM users WHERE id='" + l[0] + "' AND pwd='" + l[1] + "'";
            System.out.println("UNSAFE SQL: " + unsafe);
        }
        System.out.println("SAFE  SQL : SELECT 1 FROM users WHERE id = ? AND pwd_hash = ?   (values bound separately: id=[admin'--] is just a string that matches no user)");
        System.out.println("Cost: a transaction adds one commit (one disk flush) instead of two - usually CHEAPER than two auto-commits; the risk of two auto-commits is inconsistent data.");
    }
}
