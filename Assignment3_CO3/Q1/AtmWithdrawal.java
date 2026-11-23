import java.io.*;
import java.nio.file.*;
import java.util.*;

/**
 * Q1 - ATM withdrawal: failure points, classification and handling.
 *   FAILURE POINT                | EXCEPTION                    | KIND                | CAUGHT
 *   card-reader file missing     | FileNotFoundException        | checked (IOException)| catch (IOException)
 *   letters instead of number    | NumberFormatException        | unchecked           | catch (NumberFormatException)
 *   negative amount              | IllegalArgumentException     | unchecked           | multi-catch
 *   amount > balance             | InsufficientFundsException   | checked (user-defined)| multi-catch
 *   JVM out of memory / stack    | OutOfMemoryError (Error)     | Error - not caught  | -
 *   anything unexpected          | Exception                    | -                   | last catch (logging only)
 * Catch order: narrowest first (NumberFormatException before its parent IllegalArgumentException, IOException before Exception).
 */
class InsufficientFundsException extends Exception {
    InsufficientFundsException(String m) { super(m); }
}
public class AtmWithdrawal {
    static int balance = 5000;

    static void readCard(String path) throws IOException { try (FileReader r = new FileReader(path)) { r.read(); } }

    static String withdraw(String account, String amountText, String cardFile) {
        String path;
        try {
            readCard(cardFile);
            int amount = Integer.parseInt(amountText.trim());                  // may throw NumberFormatException
            if (amount < 0) throw new IllegalArgumentException("Amount cannot be negative");
            if (amount > balance) throw new InsufficientFundsException("Balance " + balance + " < requested " + amount);
            balance -= amount;
            return "DISPENSED " + amount + ", new balance " + balance;
        } catch (NumberFormatException e) {
            return "CAUGHT NumberFormatException: " + e.getMessage();
        } catch (InsufficientFundsException | IllegalArgumentException e) {   // multi-catch of unrelated types
            return "CAUGHT " + e.getClass().getSimpleName() + ": " + e.getMessage();
        } catch (IOException e) {
            return "CAUGHT IOException (" + e.getClass().getSimpleName() + "): card reader unavailable";
        } catch (Exception e) {
            return "CAUGHT unexpected " + e;
        } finally {
            System.out.print("   [finally] card returned for account " + account + " -> ");   // always runs: card must be ejected
        }
    }
    static int level3(String s) { return Integer.parseInt(s); }
    static int level2(String s) { return level3(s) + 1; }
    static int level1(String s) { return level2(s) + 1; }

    // Alternative: no exceptions, error codes
    static int withdrawCode(String amountText) {
        if (amountText == null || amountText.isEmpty()) return -1;
        for (int i = 0; i < amountText.length(); i++) { char c = amountText.charAt(i); if (!(Character.isDigit(c) || (i == 0 && c == '-' && amountText.length() > 1))) return -1; }
        int a = Integer.parseInt(amountText); if (a < 0) return -2; if (a > balance) return -3; return a;
    }
    static long tExc, tCode;
    public static void main(String[] args) throws Exception {
        Path card = Files.createTempFile("card", ".dat"); Files.writeString(card, "CARD-1001");
        String ok = card.toString(), missing = "no_such_card.dat";
        String[][] tests = { {"1001", "2000", ok}, {"1001", "abc", ok}, {"1001", "-500", ok}, {"1001", "999999", ok}, {"1001", "100", missing} };
        String[] labels = {"valid withdrawal", "letters typed", "negative amount", "more than balance", "card file missing"};
        for (int i = 0; i < tests.length; i++) {
            System.out.println("Test " + (i + 1) + " (" + labels[i] + "): input amount='" + tests[i][1] + "'");
            String r = withdraw(tests[i][0], tests[i][1], tests[i][2]); System.out.println(r);
        }
        System.out.println("--- Stack trace of an exception that is not handled in the calling methods (printed by the top level) ---");
        try { level1("12x"); } catch (NumberFormatException e) { e.printStackTrace(System.out); }
        System.out.println("(read bottom-up: main called level1, level1 called level2, level2 called level3, level3 called Integer.parseInt which threw)");
        Files.delete(card);

        // Cost: exception vs error code
        int n = 200_000; long sink = 0; String[] in = new String[n]; for (int i = 0; i < n; i++) in[i] = "x" + i;
        for (int w = 0; w < 2; w++) {   // second pass = warmed up
            long t0 = System.nanoTime(); for (int i = 0; i < n; i++) { try { sink += Integer.parseInt(in[i]); } catch (NumberFormatException e) { sink++; } }
            tExc = (System.nanoTime() - t0) / n;
            t0 = System.nanoTime(); for (int i = 0; i < n; i++) { sink += withdrawCode(in[i]); }
            tCode = (System.nanoTime() - t0) / n;
        }
        System.out.println("Per failing input: exception create+throw+catch ~ " + tExc + " ns (stack trace captured, O(depth) time/space); error-code check ~ " + tCode + " ns (O(1))");
    }
}
