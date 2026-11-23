import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.concurrent.*;

/*
 * TASK 1.1 - DIAGNOSIS of the junior developer's FeeWindow (>= 5 deficiencies, 10 listed)
 *  1. UI and logic mixed: validation, SQL and UI updates sit inside actionPerformed -> cannot be tested without a window/database.
 *  2. e.getActionCommand() == "Pay" compares references, not text -> may never be true; Pay silently does nothing (use equals / separate listeners).
 *  3. No validation: empty or non-numeric amount -> NumberFormatException thrown inside the listener; user sees nothing, console shows a stack trace.
 *  4. JDBC call and Thread.sleep(3000) run on the event-dispatch thread -> the window FREEZES (no repaint, cannot close) for 3+ s.
 *  5. SQL built by string concatenation -> SQL injection (regNo = "x'); DROP TABLE fees;--").
 *  6. Connection/Statement never closed -> connection leak; each click leaves an open connection.
 *  7. Checked SQLException / InterruptedException are not handled (code would not even compile as written).
 *  8. No feedback: "Paid" goes to System.out, the user never sees success or failure.
 *  9. The Receipt button is registered nowhere / handled by the 'else' branch -> clicking Receipt CLEARS the fields.
 * 10. No WindowListener -> the close button does nothing; the connection and JVM are never released.
 *
 * TASK 1.2 - REPAIRED DESIGN:  FeeWindow (view) --> FeeService (validation + payment) --> FeeRepository (JDBC)
 * TASK 1.3 - DEFENCE (<=100 words): FeeService has no AWT imports, so it is tested with a plain main() and a fake repository - no window, no database.
 *  The window only collects input and shows results; changing the database or the rules never touches the UI. Each button has its own small
 *  listener, long work runs on a worker thread, and SQL is parameterised, so the code is shorter per method, safer and easier to maintain.
 */
class ValidationException extends Exception { ValidationException(String m) { super(m); } }
class PaymentException extends Exception { PaymentException(String m, Throwable c) { super(m, c); } }

interface FeeRepository { void save(String regNo, double amount) throws SQLException; }

class JdbcFeeRepository implements FeeRepository {
    private final String url, user, pwd;
    JdbcFeeRepository(String url, String user, String pwd) { this.url = url; this.user = user; this.pwd = pwd; }
    @Override public void save(String regNo, double amount) throws SQLException {
        try (Connection c = DriverManager.getConnection(url, user, pwd);                       // closed automatically
             PreparedStatement ps = c.prepareStatement("INSERT INTO fees(reg_no, amount) VALUES (?, ?)")) {   // parameterised: no injection
            ps.setString(1, regNo); ps.setDouble(2, amount); ps.executeUpdate();
        }
    }
}

class FeeService {
    private final FeeRepository repo; private String lastReceipt = "No payment yet";
    FeeService(FeeRepository repo) { this.repo = repo; }
    String pay(String regNo, String amountText) throws ValidationException, PaymentException {
        if (regNo == null || !regNo.trim().matches("\\d{9}")) throw new ValidationException("Register number must be 9 digits");
        double amount;
        try { amount = Double.parseDouble(amountText.trim()); } catch (RuntimeException e) { throw new ValidationException("Amount must be a number"); }
        if (amount <= 0) throw new ValidationException("Amount must be greater than zero");
        try { repo.save(regNo.trim(), amount); }
        catch (SQLException e) { throw new PaymentException("Payment could not be recorded", e); }
        return lastReceipt = "Receipt: " + regNo.trim() + " paid Rs " + String.format("%.2f", amount);
    }
    String lastReceipt() { return lastReceipt; }
}

public class FeeWindow extends Frame {
    private final TextField regNo = new TextField(12), amount = new TextField(8);
    private final Button pay = new Button("Pay"), clear = new Button("Clear"), receipt = new Button("Receipt");
    private final Label status = new Label("Enter details");
    private final FeeService service;
    private final ExecutorService worker = Executors.newSingleThreadExecutor();     // keeps JDBC off the event-dispatch thread

    public FeeWindow(FeeService service) {
        super("Fee Payment");
        this.service = service;
        setLayout(new FlowLayout());
        add(new Label("Reg No")); add(regNo); add(new Label("Amount")); add(amount); add(pay); add(clear); add(receipt); add(status);
        pay.addActionListener(e -> onPay());                         // one small listener per button
        clear.addActionListener(e -> { regNo.setText(""); amount.setText(""); status.setText("Cleared"); });
        receipt.addActionListener(e -> status.setText(service.lastReceipt()));
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { worker.shutdown(); dispose(); }
        });
        setSize(520, 130);
    }
    private void onPay() {
        pay.setEnabled(false); status.setText("Processing...");
        String r = regNo.getText(), a = amount.getText();
        worker.submit(() -> {                                        // background thread
            String msg;
            try { msg = service.pay(r, a); }
            catch (ValidationException e) { msg = "Invalid input: " + e.getMessage(); }
            catch (PaymentException e) { msg = "Error: " + e.getMessage(); }
            final String shown = msg;
            EventQueue.invokeLater(() -> { status.setText(shown); pay.setEnabled(true); });   // UI update back on the EDT
        });
    }
    public static void main(String[] args) {
        FeeService svc = new FeeService(new JdbcFeeRepository("jdbc:mysql://localhost:3306/campus", "root", "password"));
        if (GraphicsEnvironment.isHeadless()) { FeeServiceTest.main(args); return; }
        EventQueue.invokeLater(() -> new FeeWindow(svc).setVisible(true));
    }
}

/** Headless test of the service layer (no window, no database needed). Run: java FeeServiceTest */
class FeeServiceTest {
    public static void main(String[] args) {
        java.util.List<String> saved = new java.util.ArrayList<>();
        FeeService ok = new FeeService((r, a) -> saved.add(r + ":" + a));
        FeeService broken = new FeeService((r, a) -> { throw new SQLException("DB down"); });
        String[][] cases = { {"192311001", "4500"}, {"", "4500"}, {"192311001", "abc"}, {"192311001", "-10"}, {"19231", "100"}, {"192311001", "0"} };
        for (String[] c : cases) {
            try { System.out.println("pay(" + c[0] + ", " + c[1] + ") -> " + ok.pay(c[0], c[1])); }
            catch (ValidationException e) { System.out.println("pay(" + c[0] + ", " + c[1] + ") -> Invalid input: " + e.getMessage()); }
            catch (PaymentException e) { System.out.println("unexpected"); }
        }
        try { broken.pay("192311001", "100"); } catch (Exception e) { System.out.println("repository failure -> " + e.getClass().getSimpleName() + ": " + e.getMessage() + " (cause: " + e.getCause().getMessage() + ")"); }
        System.out.println("Rows saved by the fake repository: " + saved);
        System.out.println("Last receipt: " + ok.lastReceipt());
        System.out.println("Dispatch alternative: one listener with an if-else chain = up to k comparisons per event, one object; one listener per button = O(1) dispatch, k small objects.");
    }
}
