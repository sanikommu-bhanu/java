import java.awt.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

/*
 * STAGE 1 REQUIREMENTS (testable): R1 a student can register for an event with 9-digit reg no, non-empty name, valid event;
 *   R2 invalid input shows a clear message and saves nothing; R3 the same student cannot register twice for the same event;
 *   R4 the table shows registrations, filterable by event; R5 export writes a CSV; R6 the window stays responsive while loading (SwingWorker).
 * STAGE 2 SCREENS: Main window (JMenuBar: File > Export CSV, Exit; View > Refresh) = form (top) + JTable (centre) + filter combo + status (bottom).
 *   Flow: Main --Register--> message dialog | Main --Export--> JFileChooser --> message dialog | Exit --> confirm dialog.
 * STAGE 3 DECISIONS: validation + duplicate check in RegistrationService (and UNIQUE key in DB as final guard); JDBC only in JdbcRegistrationDao;
 *   loading/saving run in SwingWorker.doInBackground() (off the EDT); results shown in done() (on the EDT).
 * STAGE 4 CLASSES: UI: RegistrationFrame -> Service: RegistrationService -> Data: RegistrationDao (interface) <- JdbcRegistrationDao / InMemoryDao
 *   PSEUDOCODE Register: validate(reg,name,event) -> IF dao.exists(reg,event) THEN throw Duplicate ELSE dao.insert(...) -> refresh table
 *   PSEUDOCODE Filter  : worker <- background: rows <- dao.findPage(event, page, 100) ; done(): table.setRows(rows)
 */
record Registration(String studentReg, String studentName, String event) {}
class RegistrationException extends Exception { RegistrationException(String m) { super(m); } }

interface RegistrationDao {
    boolean exists(String reg, String event) throws SQLException;
    void insert(Registration r) throws SQLException;
    List<Registration> findPage(String event, int limit, int offset) throws SQLException;
}
class JdbcRegistrationDao implements RegistrationDao {
    private final String url, user, pwd;
    JdbcRegistrationDao(String url, String user, String pwd) { this.url = url; this.user = user; this.pwd = pwd; }
    public boolean exists(String reg, String event) throws SQLException {
        try (Connection c = DriverManager.getConnection(url, user, pwd);
             PreparedStatement ps = c.prepareStatement("SELECT 1 FROM registration WHERE student_reg=? AND event_name=?")) {
            ps.setString(1, reg); ps.setString(2, event); try (ResultSet rs = ps.executeQuery()) { return rs.next(); }
        }
    }
    public void insert(Registration r) throws SQLException {
        try (Connection c = DriverManager.getConnection(url, user, pwd);
             PreparedStatement ps = c.prepareStatement("INSERT INTO registration(student_reg, student_name, event_name) VALUES (?,?,?)")) {
            ps.setString(1, r.studentReg()); ps.setString(2, r.studentName()); ps.setString(3, r.event()); ps.executeUpdate();
        }
    }
    public List<Registration> findPage(String event, int limit, int offset) throws SQLException {
        String sql = "SELECT student_reg, student_name, event_name FROM registration" + (event == null ? "" : " WHERE event_name=?") + " ORDER BY id LIMIT ? OFFSET ?";
        List<Registration> out = new ArrayList<>();
        try (Connection c = DriverManager.getConnection(url, user, pwd); PreparedStatement ps = c.prepareStatement(sql)) {
            int i = 1; if (event != null) ps.setString(i++, event); ps.setInt(i++, limit); ps.setInt(i, offset);
            try (ResultSet rs = ps.executeQuery()) { while (rs.next()) out.add(new Registration(rs.getString(1), rs.getString(2), rs.getString(3))); }
        }
        return out;
    }
}
class InMemoryDao implements RegistrationDao {                      // used for the headless demo/tests
    private final List<Registration> rows = new ArrayList<>();
    public boolean exists(String reg, String event) { return rows.stream().anyMatch(r -> r.studentReg().equals(reg) && r.event().equals(event)); }
    public void insert(Registration r) { rows.add(r); }
    public List<Registration> findPage(String event, int limit, int offset) {
        return rows.stream().filter(r -> event == null || r.event().equals(event)).skip(offset).limit(limit).toList();
    }
}
class RegistrationService {
    static final Set<String> EVENTS = new LinkedHashSet<>(List.of("Dance", "Music", "Drama", "Quiz"));
    private final RegistrationDao dao; RegistrationService(RegistrationDao dao) { this.dao = dao; }
    void register(String reg, String name, String event) throws RegistrationException {
        if (reg == null || !reg.trim().matches("\\d{9}")) throw new RegistrationException("Register number must be 9 digits");
        if (name == null || name.isBlank()) throw new RegistrationException("Name is required");
        if (!EVENTS.contains(event)) throw new RegistrationException("Unknown event: " + event);
        try {
            if (dao.exists(reg.trim(), event)) throw new RegistrationException(reg.trim() + " is already registered for " + event);
            dao.insert(new Registration(reg.trim(), name.trim(), event));
        } catch (SQLException e) { throw new RegistrationException("Database error: " + e.getMessage()); }
    }
    List<Registration> page(String event, int limit, int offset) throws RegistrationException {
        try { return dao.findPage(event, limit, offset); } catch (SQLException e) { throw new RegistrationException("Database error: " + e.getMessage()); }
    }
}
public class EventRegistrationApp extends JFrame {
    private final RegistrationService service;
    private final JTextField reg = new JTextField(10), name = new JTextField(14);
    private final JComboBox<String> event = new JComboBox<>(RegistrationService.EVENTS.toArray(new String[0]));
    private final JComboBox<String> filter = new JComboBox<>(new String[]{"All", "Dance", "Music", "Drama", "Quiz"});
    private final DefaultTableModel model = new DefaultTableModel(new String[]{"Reg No", "Name", "Event"}, 0);
    private final JLabel status = new JLabel("Ready");

    EventRegistrationApp(RegistrationService service) {
        super("Campus Event Registration"); this.service = service;
        JMenuBar bar = new JMenuBar(); JMenu file = new JMenu("File"), view = new JMenu("View");
        JMenuItem export = new JMenuItem("Export CSV"), exit = new JMenuItem("Exit"), refresh = new JMenuItem("Refresh");
        file.add(export); file.addSeparator(); file.add(exit); view.add(refresh); bar.add(file); bar.add(view); setJMenuBar(bar);
        JPanel form = new JPanel(new FlowLayout(FlowLayout.LEFT)); JButton register = new JButton("Register");
        form.add(new JLabel("Reg No")); form.add(reg); form.add(new JLabel("Name")); form.add(name); form.add(event); form.add(register);
        JPanel top = new JPanel(new BorderLayout()); top.add(form, BorderLayout.NORTH);
        JPanel fp = new JPanel(new FlowLayout(FlowLayout.LEFT)); fp.add(new JLabel("Filter by event")); fp.add(filter); top.add(fp, BorderLayout.SOUTH);
        add(top, BorderLayout.NORTH); add(new JScrollPane(new JTable(model)), BorderLayout.CENTER); add(status, BorderLayout.SOUTH);
        register.addActionListener(e -> doRegister()); filter.addActionListener(e -> load()); refresh.addActionListener(e -> load());
        export.addActionListener(e -> doExport()); exit.addActionListener(e -> { if (JOptionPane.showConfirmDialog(this, "Exit?", "Confirm", JOptionPane.YES_NO_OPTION) == 0) dispose(); });
        setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(640, 400); load();
    }
    private void doRegister() {
        try { service.register(reg.getText(), name.getText(), (String) event.getSelectedItem()); status.setText("Registered " + reg.getText().trim()); load(); }
        catch (RegistrationException ex) { JOptionPane.showMessageDialog(this, ex.getMessage(), "Cannot register", JOptionPane.WARNING_MESSAGE); }
    }
    private void load() {                       // paging with SwingWorker: DB work off the EDT
        String f = (String) filter.getSelectedItem(); status.setText("Loading...");
        new SwingWorker<List<Registration>, Void>() {
            @Override protected List<Registration> doInBackground() throws Exception { return service.page("All".equals(f) ? null : f, 100, 0); }
            @Override protected void done() {
                try { model.setRowCount(0); for (Registration r : get()) model.addRow(new Object[]{r.studentReg(), r.studentName(), r.event()}); status.setText(model.getRowCount() + " rows (page 1)"); }
                catch (Exception ex) { status.setText("Load failed: " + ex.getMessage()); }
            }
        }.execute();
    }
    private void doExport() {
        JFileChooser fc = new JFileChooser(); if (fc.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;
        try (PrintWriter w = new PrintWriter(fc.getSelectedFile())) {
            for (int i = 0; i < model.getRowCount(); i++) w.println(model.getValueAt(i, 0) + "," + model.getValueAt(i, 1) + "," + model.getValueAt(i, 2));
            status.setText("Exported " + model.getRowCount() + " rows");
        } catch (IOException ex) { JOptionPane.showMessageDialog(this, "Export failed: " + ex.getMessage()); }
    }
    public static void main(String[] args) throws Exception {
        if (GraphicsEnvironment.isHeadless()) {                 // headless demo of the service layer with the in-memory DAO
            RegistrationService s = new RegistrationService(new InMemoryDao());
            String[][] in = { {"192311001", "Arun", "Dance"}, {"192311002", "Divya", "Music"}, {"192311001", "Arun", "Dance"}, {"19231", "Kiran", "Quiz"}, {"192311003", "", "Quiz"}, {"192311001", "Arun", "Music"} };
            String[] kind = {"normal", "normal", "DUPLICATE", "invalid reg no", "invalid name", "same student, other event"};
            for (int i = 0; i < in.length; i++) {
                try { s.register(in[i][0], in[i][1], in[i][2]); System.out.println(kind[i] + ": registered " + in[i][0] + " for " + in[i][2]); }
                catch (RegistrationException e) { System.out.println(kind[i] + ": REJECTED - " + e.getMessage()); }
            }
            System.out.println("Filter Dance: " + s.page("Dance", 100, 0)); System.out.println("All: " + s.page(null, 100, 0));
            System.out.println("Paged loading (LIMIT 100) keeps memory O(page) and the window responsive; loading 100,000 rows at once = O(n) memory and a long wait.");
            return;
        }
        SwingUtilities.invokeLater(() -> new EventRegistrationApp(new RegistrationService(new JdbcRegistrationDao("jdbc:mysql://localhost:3306/campus_events", "root", "password"))).setVisible(true));
    }
}
