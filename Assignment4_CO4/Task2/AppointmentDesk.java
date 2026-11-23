import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;
import javax.swing.*;

/*
 * TASK 2.1 EVENT TABLE
 *  Source               | Event object      | Listener interface | Action
 *  department JComboBox | ItemEvent         | ItemListener       | refresh doctor list from Map (pseudocode a)
 *  doctor JComboBox     | ActionEvent       | ActionListener     | refresh free time slots
 *  Book JButton         | ActionEvent       | ActionListener     | validate and book (pseudocode b)
 *  JFrame               | WindowEvent       | WindowListener     | windowClosing: confirm if a booking is in progress
 *
 * TASK 2.2 COMPONENT HIERARCHY
 *  JFrame
 *   +- JPanel (GridLayout 5x2): JLabel+JTextField(patientId), JLabel+JComboBox(department), JLabel+JComboBox(doctor), JLabel+JComboBox(slot), JButton(Book)
 *   +- JLabel (BorderLayout.SOUTH): status
 *
 * TASK 2.3 PSEUDOCODE
 *  (a) ON department changed: doctorBox.removeAll(); FOR each d IN doctorsByDept.get(selectedDept) DO doctorBox.add(d) END FOR
 *  (b) ON Book clicked: IF patientId is empty OR not numeric THEN status <- "Invalid patient ID"; RETURN
 *                       IF slot already booked for doctor THEN status <- "Slot taken"; RETURN
 *                       bookingInProgress <- TRUE; book(); bookingInProgress <- FALSE; status <- "Confirmed"
 *
 * TASK 2.4 EVENT TRACE (open -> close): constructor -> setVisible(true) -> windowOpened -> windowActivated -> (user events: itemStateChanged,
 *   actionPerformed ...) -> windowDeactivated -> windowClosing -> dispose() -> windowClosed.   Applet life cycle: init() once, start() when shown,
 *   paint() whenever redrawn, stop() when hidden, destroy() at unload; a Frame has no init/start/stop - it is created by main() and driven by window events.
 */
class AppointmentModel {
    private final Map<String, List<String>> doctorsByDept = new LinkedHashMap<>();   // loaded ONCE (alternative: query DB on every change)
    private final Set<String> booked = new HashSet<>();
    AppointmentModel() {
        doctorsByDept.put("Cardiology", List.of("Dr. Rao", "Dr. Iyer"));
        doctorsByDept.put("Orthopaedics", List.of("Dr. Khan", "Dr. Das", "Dr. Paul"));
        doctorsByDept.put("ENT", List.of("Dr. Mehta"));
    }
    Set<String> departments() { return doctorsByDept.keySet(); }
    List<String> doctors(String dept) { return doctorsByDept.getOrDefault(dept, List.of()); }
    String book(String patientId, String doctor, String slot) {
        if (patientId == null || !patientId.trim().matches("\\d+")) return "ERROR: invalid patient ID";
        if (doctor == null || slot == null) return "ERROR: choose a doctor and a slot";
        if (!booked.add(doctor + "@" + slot)) return "ERROR: " + doctor + " is already booked at " + slot;
        return "CONFIRMED: patient " + patientId.trim() + " with " + doctor + " at " + slot;
    }
}
public class AppointmentDesk extends JFrame {
    private final AppointmentModel model = new AppointmentModel();
    private final JTextField patient = new JTextField(10);
    private final JComboBox<String> dept = new JComboBox<>(), doctor = new JComboBox<>(), slot = new JComboBox<>(new String[]{"09:00", "09:30", "10:00", "10:30"});
    private final JLabel status = new JLabel("Ready");
    private boolean bookingInProgress = false;

    AppointmentDesk() {
        super("Outpatient Appointment Desk");
        JPanel form = new JPanel(new GridLayout(5, 2, 4, 4));
        form.add(new JLabel("Patient ID")); form.add(patient);
        form.add(new JLabel("Department")); form.add(dept);
        form.add(new JLabel("Doctor")); form.add(doctor);
        form.add(new JLabel("Time slot")); form.add(slot);
        JButton book = new JButton("Book"); form.add(new JLabel()); form.add(book);
        add(form, BorderLayout.CENTER); add(status, BorderLayout.SOUTH);
        for (String d : model.departments()) dept.addItem(d);
        refreshDoctors();
        dept.addItemListener(e -> { if (e.getStateChange() == ItemEvent.SELECTED) refreshDoctors(); });      // (a)
        book.addActionListener(e -> {                                                                         // (b)
            bookingInProgress = true;
            status.setText(model.book(patient.getText(), (String) doctor.getSelectedItem(), (String) slot.getSelectedItem()));
            bookingInProgress = false;
        });
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                if (bookingInProgress && JOptionPane.showConfirmDialog(AppointmentDesk.this, "A booking is in progress. Close anyway?", "Confirm", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;
                dispose();
            }
        });
        pack();
    }
    private void refreshDoctors() { doctor.removeAllItems(); for (String d : model.doctors((String) dept.getSelectedItem())) doctor.addItem(d); }

    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {          // headless self-test of the model (no window available)
            AppointmentModel m = new AppointmentModel();
            System.out.println("Departments: " + m.departments());
            System.out.println("Doctors in Orthopaedics: " + m.doctors("Orthopaedics"));
            System.out.println(m.book("1001", "Dr. Khan", "09:00"));
            System.out.println(m.book("1002", "Dr. Khan", "09:00"));
            System.out.println(m.book("abc", "Dr. Rao", "10:00"));
            System.out.println(m.book("1003", "Dr. Rao", "10:00"));
            System.out.println("Cached Map lookup per department change = O(1); DB query per change = O(network + m log d); memory of the Map = O(d + m).");
            return;
        }
        SwingUtilities.invokeLater(() -> new AppointmentDesk().setVisible(true));
    }
}
