import java.awt.*;
import javax.swing.*;

/*
 * TASK 3.1 LAYOUT CHOICES
 *  Root panel .................. BorderLayout : NORTH/SOUTH keep their preferred height, CENTER takes ALL extra space (results area grows)
 *  North container ............. BorderLayout : title (NORTH) above the search row (SOUTH) - both keep their height
 *  Search row .................. FlowLayout   : text field, category choice and Search button keep their preferred size, flow left to right
 *  Results area (CENTER) ....... JScrollPane(JTextArea) : the only region that stretches in both directions
 *  South container ............. BorderLayout : button row (NORTH) above the status message (SOUTH)
 *  Button row .................. FlowLayout   : Prev, Next, Reserve keep their size, centred
 *  (GridLayout would stretch every button; CardLayout is for stacked screens, not needed here.)
 *
 * TASK 3.2 NESTED CONTAINERS
 *  LibraryPanel [BorderLayout]
 *    NORTH  : JPanel [BorderLayout]  -> NORTH: title JLabel | SOUTH: searchRow JPanel [FlowLayout](JTextField, JComboBox, JButton Search)
 *    CENTER : JScrollPane -> JTextArea (results)
 *    SOUTH  : JPanel [BorderLayout]  -> NORTH: buttonRow JPanel [FlowLayout](Prev, Next, Reserve) | SOUTH: status JLabel
 */
class LibraryPanel extends JPanel {
    final JLabel title = new JLabel("Campus Library Search", SwingConstants.CENTER);
    final JPanel searchRow = new JPanel(new FlowLayout(FlowLayout.LEFT));
    final JTextArea resultsArea = new JTextArea("Results appear here...");
    final JScrollPane results = new JScrollPane(resultsArea);
    final JPanel buttonRow = new JPanel(new FlowLayout(FlowLayout.CENTER));
    final JLabel status = new JLabel("Ready");

    LibraryPanel() {
        super(new BorderLayout());
        title.setFont(title.getFont().deriveFont(Font.BOLD, 18f));
        searchRow.add(new JTextField(20));
        searchRow.add(new JComboBox<>(new String[]{"Title", "Author", "ISBN"}));
        searchRow.add(new JButton("Search"));
        JPanel north = new JPanel(new BorderLayout()); north.add(title, BorderLayout.NORTH); north.add(searchRow, BorderLayout.SOUTH);
        buttonRow.add(new JButton("Prev")); buttonRow.add(new JButton("Next")); buttonRow.add(new JButton("Reserve"));
        JPanel south = new JPanel(new BorderLayout()); south.add(buttonRow, BorderLayout.NORTH); south.add(status, BorderLayout.SOUTH);
        add(north, BorderLayout.NORTH); add(results, BorderLayout.CENTER); add(south, BorderLayout.SOUTH);
    }
}
public class LibrarySearch extends JFrame {
    LibrarySearch() { super("Library Search"); add(new LibraryPanel()); setDefaultCloseOperation(EXIT_ON_CLOSE); setSize(640, 420); setLocationRelativeTo(null); }
    public static void main(String[] args) {
        if (GraphicsEnvironment.isHeadless()) { LayoutProbe.main(args); return; }
        SwingUtilities.invokeLater(() -> new LibrarySearch().setVisible(true));
    }
}

/** TASK 3.4 - headless resize test: lays the panel out at three sizes and prints each region's bounds. Run: java LayoutProbe */
class LayoutProbe {
    static void layoutAll(Container c) { c.doLayout(); for (Component k : c.getComponents()) if (k instanceof Container ct) layoutAll(ct); }
    static String r(LibraryPanel root, Component c) { Rectangle b = SwingUtilities.convertRectangle(c.getParent(), c.getBounds(), root); return b.width + " x " + b.height + " at (" + b.x + "," + b.y + ")"; }
    public static void main(String[] args) {
        int[][] sizes = {{640, 420}, {900, 700}, {420, 300}};
        for (int[] s : sizes) {
            LibraryPanel p = new LibraryPanel(); p.setSize(s[0], s[1]); layoutAll(p);
            System.out.println("Window content size " + s[0] + " x " + s[1]);
            System.out.println("   search row : " + r(p, p.searchRow));
            System.out.println("   results    : " + r(p, p.results) + "   <-- grows/shrinks with the window");
            System.out.println("   button row : " + r(p, p.buttonRow));
            System.out.println("   status     : " + r(p, p.status));
        }
        System.out.println("Observation: search row, button row and status keep their height at every size; only the results area changes (width = window width, height = window height - fixed rows).");
        System.out.println("Alternatives: GridBagLayout = same behaviour with weights but more code/constraint objects; setLayout(null)+setBounds = nothing resizes automatically.");
        System.out.println("Layout cost: nested BorderLayout/FlowLayout O(c) per container (c children); containers used here = 7 panels; GridBag adds one constraint object per child.");
    }
}
