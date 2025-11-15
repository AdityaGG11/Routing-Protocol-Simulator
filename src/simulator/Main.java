package simulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

/**
 * Main launcher: assembles the frame with ControlPanel (west), CanvasPanel (center), RightPanel (east).
 * Frame will maximize on activation.
 */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Graph graph = new Graph();

            JFrame frame = new JFrame("Routing Protocol Simulator");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(1100, 700);
            frame.setLayout(new BorderLayout());

            CanvasPanel canvas = new CanvasPanel(graph);
            ControlPanel controls = new ControlPanel(graph, canvas);
            RightPanel right = new RightPanel(canvas, controls);

            frame.add(controls, BorderLayout.WEST);
            frame.add(new JScrollPane(canvas), BorderLayout.CENTER);
            frame.add(right, BorderLayout.EAST);

            // maximize initially
            frame.setExtendedState(JFrame.MAXIMIZED_BOTH);

            // also maximize whenever window is activated (handles focus/resume)
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowActivated(WindowEvent e) {
                    frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                }
            });

            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
