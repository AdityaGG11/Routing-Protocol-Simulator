package simulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

/**
 * Right-side panel with input fields and the Show/Hide Routing Tables button.
 * Now accepts the ControlPanel reference to read the selected algorithm.
 */
public class RightPanel extends JPanel {
    private final CanvasPanel canvas;
    private final ControlPanel control;
    private final JTextField costField;
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
        gbc.gridx = 0; gbc.gridy = 0;

        add(new JLabel("<html><b>Link Settings</b></html>"), gbc);

        gbc.gridy++;
        add(new JLabel("Default link cost:"), gbc);
        gbc.gridy++;
        costField = new JTextField(Integer.toString(canvas.getDefaultLinkCost()), 8);
        add(costField, gbc);

        gbc.gridy++;
        JButton applyCost = new JButton("Apply Cost");
        applyCost.addActionListener(e -> applyCost());
        add(applyCost, gbc);

        // Show/Hide routing tables button placed near the top
        gbc.gridy++;
        showHideTablesBtn = new JButton(canvas.getPerNodeTables() == null ? "Show Routing Tables" : "Hide Routing Tables");
        showHideTablesBtn.addActionListener(e -> toggleRoutingTables());
        add(showHideTablesBtn, gbc);

        gbc.gridy++;
        add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        gbc.gridy++;
        add(new JLabel("<html><b>Node Settings</b></html>"), gbc);

        gbc.gridy++;
        add(new JLabel("Select source node:"), gbc);

        gbc.gridy++;
        sourceCombo = new JComboBox<>();
        refreshNodeList();
        add(sourceCombo, gbc);

        gbc.gridy++;
        JButton refreshBtn = new JButton("Refresh Nodes");
        refreshBtn.addActionListener(e -> refreshNodeList());
        add(refreshBtn, gbc);

        // spacer to push content to top
        gbc.gridy++;
        gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);

        setPreferredSize(new Dimension(240, 0));
    }

    private void applyCost() {
        try {
            int v = Integer.parseInt(costField.getText().trim());
            canvas.setDefaultLinkCost(Math.max(1, v));
            JOptionPane.showMessageDialog(this, "Default link cost set to " + canvas.getDefaultLinkCost());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Enter a valid integer cost.");
        }
    }

    /** Refresh the nodes shown in the combo box using canvas node list. */
    public void refreshNodeList() {
        java.util.List<String> ids = canvas.getNodeIds();
        DefaultComboBoxModel<String> m = new DefaultComboBoxModel<>();
        for (String id : ids) m.addElement(id);
        sourceCombo.setModel(m);
    }

    /** Return selected source node id (or null). */
    public String getSelectedSource() {
        Object o = sourceCombo.getSelectedItem();
        return o == null ? null : o.toString();
    }

    // Show/hide routing tables using the algorithm selected in ControlPanel
    private void toggleRoutingTables() {
        if (canvas.getPerNodeTables() != null) {
            canvas.setPerNodeTables(null);
            showHideTablesBtn.setText("Show Routing Tables");
        } else {
            // get algorithm from left control
            String algo = control.getSelectedAlgorithm();
            Map<String, Map<String, RoutingTableEntry>> tables = canvas.computeRoutingTables(algo);
            canvas.setPerNodeTables(tables);
            showHideTablesBtn.setText("Hide Routing Tables");
        }
        canvas.repaint();
    }
}
