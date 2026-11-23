import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/*
 * TASK 5.1/5.2 - the same Convert action in four styles (all four already include the 5.2 change: Enter key + result dialog).
 * CHANGE EFFORT for 5.2 (Enter key and dialog):
 *   (a) frame implements ActionListener : +1 line (amount.addActionListener(this)) + 1 line in actionPerformed (dialog)  -> 2 lines, but one handler for ALL sources
 *   (b) named inner class               : +1 line (register same listener object) + 1 line (dialog)                    -> 2 lines, handler reusable
 *   (c) anonymous inner class           : cannot be registered twice -> listener must be copied or stored in a variable   -> ~6 lines
 *   (d) lambda                          : store lambda in an ActionListener variable, register twice + 1 dialog line      -> 2 lines
 * COMPARISON: readability (d)>(c)>(b)>(a) for small handlers; field access: (a),(b),(c),(d) all reach the frame's fields (inner classes and lambdas share
 *   the enclosing instance); reuse: (a),(b) best; ease of change: (d)/(b). LARGE application: named inner classes or separate listener/Action classes
 *   (testable, reusable, one responsibility); (a) becomes a giant if-else, (c) is noisy, (d) is fine for one-liners that delegate to a method.
 */
class ConversionLogic {
    static double rate(String c) { return c.equals("USD") ? 0.012 : 0.011; }
    static String convert(String amountText, String currency) {
        try { return String.format("%.2f %s", Double.parseDouble(amountText.trim()) * rate(currency), currency); }
        catch (NumberFormatException e) { return "Invalid amount"; }
    }
}
abstract class BaseConverter extends JFrame {
    final JTextField amount = new JTextField(10);
    final JComboBox<String> target = new JComboBox<>(new String[]{"USD", "EUR"});
    final JLabel result = new JLabel(" "); final JButton button = new JButton("Convert");
    BaseConverter(String title) {
        super(title); setLayout(new FlowLayout()); add(new JLabel("INR")); add(amount); add(target); add(button); add(result);
        addWindowListener(new WindowAdapter() { @Override public void windowClosing(WindowEvent e) { dispose(); } });
    }
    void showResult() {
        String text = ConversionLogic.convert(amount.getText(), (String) target.getSelectedItem());
        result.setText(text); JOptionPane.showMessageDialog(this, text);
    }
}
class ConverterA extends BaseConverter implements ActionListener {                     // (a) the frame is the listener
    ConverterA() { super("A: frame implements ActionListener"); button.addActionListener(this); amount.addActionListener(this); pack(); }
    @Override public void actionPerformed(ActionEvent e) { showResult(); }
}
class ConverterB extends BaseConverter {                                                // (b) named inner class
    class ConvertListener implements ActionListener { @Override public void actionPerformed(ActionEvent e) { showResult(); } }
    ConverterB() { super("B: named inner class"); ConvertListener l = new ConvertListener(); button.addActionListener(l); amount.addActionListener(l); pack(); }
}
class ConverterC extends BaseConverter {                                                // (c) anonymous inner class
    ConverterC() {
        super("C: anonymous inner class");
        button.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { showResult(); } });
        amount.addActionListener(new ActionListener() { @Override public void actionPerformed(ActionEvent e) { showResult(); } });   // duplicated
        pack();
    }
}
class ConverterD extends BaseConverter {                                                // (d) lambda
    ConverterD() { super("D: lambda"); ActionListener l = e -> showResult(); button.addActionListener(l); amount.addActionListener(l); pack(); }
}
public class ConverterStyles {
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) {
            System.out.println("Headless: testing the shared conversion logic used by all four styles");
            System.out.println("1000 INR to USD -> " + ConversionLogic.convert("1000", "USD"));
            System.out.println("1000 INR to EUR -> " + ConversionLogic.convert("1000", "EUR"));
            System.out.println("'abc'           -> " + ConversionLogic.convert("abc", "USD"));
            System.out.println("''              -> " + ConversionLogic.convert("", "USD"));
            System.out.println("Listener objects (space): a=1, b=2 -> 1 shared, c=2 (duplicated), d=1 lambda; method reference this::convert would also be 1 object.");
            return;
        }
        SwingUtilities.invokeLater(() -> { new ConverterA().setVisible(true); new ConverterB().setVisible(true); new ConverterC().setVisible(true); new ConverterD().setVisible(true); });
    }
}
