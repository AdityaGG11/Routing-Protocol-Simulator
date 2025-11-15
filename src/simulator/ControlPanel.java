package simulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Collection;
import java.util.Map;

/**
 * Control panel with interactive delete modes.
 * No Show/Hide button here anymore. Exposes getSelectedAlgorithm().
 */
public class ControlPanel extends JPanel {
    private final CanvasPanel canvas;
    private final Graph graph;

    private final JButton addEdgeBtn;
    private final JButton addRouterBtn;
    private final JButton runBtn;
    private final JButton deleteRouterBtn;
    private final JButton deleteLinkBtn;
    private final JComboBox<String> algoCombo;

    public ControlPanel(Graph graph, CanvasPanel canvas) {
        this.canvas = canvas;
        this.graph = graph;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 0.0;

        addRouterBtn = new JButton("Add Router");
        addRouterBtn.addActionListener(e -> toggleAddRouter());
        add(addRouterBtn, gbc);

        gbc.gridy++;
        addEdgeBtn = new JButton("Add Link");
        addEdgeBtn.addActionListener(e -> toggleAddEdge());
        add(addEdgeBtn, gbc);

        gbc.gridy++;
        algoCombo = new JComboBox<>(new String[]{"Distance Vector", "Link State"});
        add(algoCombo, gbc);

        gbc.gridy++;
        runBtn = new JButton("Run Algorithm");
        runBtn.addActionListener(e -> runSelectedAlgorithm());
        add(runBtn, gbc);

        // Delete router (interactive)
        gbc.gridy++;
        deleteRouterBtn = new JButton("Delete Router");
        deleteRouterBtn.addActionListener(e -> toggleDeleteRouterMode());
        add(deleteRouterBtn, gbc);

        // Delete link (interactive)
        gbc.gridy++;
        deleteLinkBtn = new JButton("Delete Link");
        deleteLinkBtn.addActionListener(e -> toggleDeleteLinkMode());
        add(deleteLinkBtn, gbc);

        gbc.gridy++;
        JButton saveBtn = new JButton("Save Topology");
        saveBtn.addActionListener(e -> saveTopology());
        add(saveBtn, gbc);

        gbc.gridy++;
        JButton loadBtn = new JButton("Load Topology");
        loadBtn.addActionListener(e -> loadTopology());
        add(loadBtn, gbc);

        gbc.gridy++;
        JButton clearBtn = new JButton("Clear");
        clearBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this, "Clear all nodes and edges?", "Confirm", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                for (Node n : new java.util.ArrayList<>(graph.getNodes())) {
                    graph.removeNode(n.getId());
                }
                canvas.repaint();
                canvas.setPerNodeTables(null);
            }
        });
        add(clearBtn, gbc);

        gbc.gridy++;
        gbc.weighty = 1.0;
        JPanel spacer = new JPanel();
        spacer.setOpaque(false);
        add(spacer, gbc);

        setPreferredSize(new Dimension(200, 0));

        // bind ESC to exit delete modes
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"), "exitDeleteModes");
        getActionMap().put("exitDeleteModes", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exitDeleteModes();
            }
        });
    }

    // allow RightPanel to see which algorithm is selected
    public String getSelectedAlgorithm() {
        Object sel = algoCombo.getSelectedItem();
        return sel == null ? "Distance Vector" : sel.toString();
    }

    private void toggleAddEdge() {
        boolean on = !canvas.isAddEdgeMode();
        // turning on add-edge cancels delete-modes and add-node mode
        if (on) {
            exitDeleteModes();
            canvas.setAddNodeMode(false);
            addRouterBtn.setText("Add Router");
        }
        canvas.setAddEdgeMode(on);
        addEdgeBtn.setText(on ? "Exit Add Link" : "Add Link");
    }

    private void toggleAddRouter() {
        boolean on = !canvas.isAddNodeMode();
        if (on) {
            exitDeleteModes();
            canvas.setAddEdgeMode(false);
            addEdgeBtn.setText("Add Link");
        }
        canvas.setAddNodeMode(on);
        addRouterBtn.setText(on ? "Exit Add Router" : "Add Router");
    }

    private void runSelectedAlgorithm() {
        exitDeleteModes();
        String algo = getSelectedAlgorithm();
        Map<String, Map<String, RoutingTableEntry>> tables = canvas.computeRoutingTables(algo);
        canvas.setPerNodeTables(tables);

        int msPerEdge = Math.max(150, 400 - graph.getEdges().size() * 10);
        canvas.flashEdgeSequence(msPerEdge);
    }

    // Toggle delete-node mode on canvas
    private void toggleDeleteRouterMode() {
        boolean on = !canvas.isDeleteNodeMode();
        // Turn off other conflicting modes
        if (on) {
            canvas.setDeleteEdgeMode(false);
            deleteLinkBtn.setText("Delete Link");
            canvas.setAddEdgeMode(false);
            addEdgeBtn.setText("Add Link");
            canvas.setAddNodeMode(false);
            addRouterBtn.setText("Add Router");
        }
        canvas.setDeleteNodeMode(on);
        deleteRouterBtn.setText(on ? "Exit Delete Router" : "Delete Router");
    }

    // Toggle delete-edge mode on canvas
    private void toggleDeleteLinkMode() {
        boolean on = !canvas.isDeleteEdgeMode();
        if (on) {
            canvas.setDeleteNodeMode(false);
            deleteRouterBtn.setText("Delete Router");
            canvas.setAddEdgeMode(false);
            addEdgeBtn.setText("Add Link");
            canvas.setAddNodeMode(false);
            addRouterBtn.setText("Add Router");
        }
        canvas.setDeleteEdgeMode(on);
        deleteLinkBtn.setText(on ? "Exit Delete Link" : "Delete Link");
    }

    private void exitDeleteModes() {
        if (canvas.isDeleteNodeMode()) {
            canvas.setDeleteNodeMode(false);
            deleteRouterBtn.setText("Delete Router");
        }
        if (canvas.isDeleteEdgeMode()) {
            canvas.setDeleteEdgeMode(false);
            deleteLinkBtn.setText("Delete Link");
        }
    }

    private void saveTopology() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        int ret = chooser.showSaveDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            canvas.saveToFile(f);
        }
    }

    private void loadTopology() {
        JFileChooser chooser = new JFileChooser();
        chooser.setCurrentDirectory(new File("."));
        int ret = chooser.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            File f = chooser.getSelectedFile();
            canvas.loadFromFile(f);
            canvas.setPerNodeTables(null);
        }
    }
}
