package simulator;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * Right-side panel with:
 * - Show/Hide Routing Tables button
 * - Source-node dropdown
 *
 * Default link cost controls REMOVED.
 */
public class RightPanel extends JPanel {
    private final CanvasPanel canvas;
    private final ControlPanel control;
    private final JComboBox<String> sourceCombo;
    private final JButton showHideTablesBtn;

    public RightPanel(CanvasPanel canvas, ControlPanel control) {
        this.canvas = canvas;
        this.control = control;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        // Title
        add(new JLabel("<html><b>Routing Controls</b></html>"), gbc);

        // Show / Hide tables
        gbc.gridy++;
        showHideTablesBtn = new JButton(
                canvas.getPerNodeTables() == null ? 
                "Show Routing Tables" : 
                "Hide Routing Tables"
        );
        showHideTablesBtn.addActionListener(e -> toggleRoutingTables());
        add(showHideTablesBtn, gbc);

        // Divider
        gbc.gridy++;
        add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        // Node settings header
        gbc.gridy++;
        add(new JLabel("<html><b>Node Settings</b></html>"), gbc);

        // Source node label
        gbc.gridy++;
        add(new JLabel("Select source node:"), gbc);

        // Source node combo box
        gbc.gridy++;
        sourceCombo = new JComboBox<>();
        refreshNodeList();
        add(sourceCombo, gbc);

        // Refresh button
        gbc.gridy++;
        JButton refreshBtn = new JButton("Refresh Nodes");
        refreshBtn.addActionListener(e -> refreshNodeList());
        add(refreshBtn, gbc);

        // Spacer at bottom
        gbc.gridy++;
        gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);

        setPreferredSize(new Dimension(240, 0));
    }

    /** Refresh the nodes shown in the combo box using canvas node list. */
    public void refreshNodeList() {
        java.util.List<String> ids = canvas.getNodeIds();
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        for (String id : ids) m.addElement(id);
        sourceCombo.setModel(m);
    }

    /** Return selected source node id */
    public String getSelectedSource() {
        Object o = sourceCombo.getSelectedItem();
        return o == null ? null : o.toString();
    }

    /** toggle routing tables on/off */
    private void toggleRoutingTables() {
        if (canvas.getPerNodeTables() != null) {
            canvas.setPerNodeTables(null);
            showHideTablesBtn.setText("Show Routing Tables");
        } else {
            String algo = control.getSelectedAlgorithm();
            Map<String, Map<String, RoutingTableEntry>> tables =
                    canvas.computeRoutingTables(algo);
            canvas.setPerNodeTables(tables);
            showHideTablesBtn.setText("Hide Routing Tables");
        }
        canvas.repaint();
    }
}
