import java.awt.BorderLayout;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Label;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

/*
 * TASK 4.1  Defect: the method is spelled 'mouseclicked' (lower-case c). MouseAdapter declares mouseClicked, so the method is a NEW unrelated
 *           method that is never called -> no dot, count never increments, label never changes.
 * TASK 4.2  Failing interaction: click to draw dots, minimise (or cover) the window and restore it. getGraphics() paints directly on the screen,
 *           nothing is remembered; AWT calls paint() to redraw and paint() draws nothing -> all dots vanish.
 * TASK 4.3  Repair: store each Point in a list, call repaint(), and draw the list in paint().
 * TASK 4.4  With @Override on 'mouseclicked' the compiler reports "method does not override or implement a method from a supertype".
 */
public class DrawPad extends Frame {
    private final List<Point> points = new ArrayList<>();
    private final Label status = new Label("Clicks: 0");

    public DrawPad() {
        super("Draw Pad");
        add(status, BorderLayout.SOUTH);
        addMouseListener(new MouseAdapter() {
            // TODO 1: Override mouseClicked correctly: store the point, update the
            // label and request a repaint
            @Override public void mouseClicked(MouseEvent e) {
                points.add(e.getPoint());
                status.setText("Clicks: " + points.size());
                repaint();
            }
        });
        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) { dispose(); }
        });
        setSize(400, 300);
    }

    @Override public void paint(Graphics g) {
        // TODO 2: Draw every stored point as a 6 × 6 filled circle centred on the point
        for (Point p : points) g.fillOval(p.x - 3, p.y - 3, 6, 6);
    }

    public static void main(String[] args) { new DrawPad().setVisible(true); }
}
