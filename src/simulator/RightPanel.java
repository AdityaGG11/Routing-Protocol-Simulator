package simulator;

import java.awt.*;
import java.util.Map;
import javax.swing.*;

/**
 * Right-side panel with:
 *  - Show/Hide Routing Tables button
 *  - Show/Hide Steps (collapsible large area)
 *  - Source-node dropdown
 *  - Playback controls: Prepare Events, Play, Pause, Step, Stop, Speed
 *
 * This panel assumes CanvasPanel exposes:
 *  - List<String> getNodeIds()
 *  - Map<String,Map<String,RoutingTableEntry>> computeRoutingTables(String)
 *  - List<ProtocolEvent> fetchEventsForAlgorithm(String)
 *  - void playEvents(List<ProtocolEvent>, int delayMs, boolean resetTables)
 *  - void pausePlayback(), void stopPlayback(), void stepPlayback()
 *  - int getPlaybackIndex(), int getPlaybackTotal()
 *  - List<String> getLastStepLogs()
 */
public class RightPanel extends JPanel {
    private final CanvasPanel canvas;
    private final ControlPanel control;
    private final JComboBox<String> sourceCombo;
    private final JButton showHideTablesBtn;

    // Steps UI (enlarged)
    private final JButton showHideStepsBtn;
    private final JTextArea stepsTextArea;
    private final JScrollPane stepsScrollPane;
    private boolean stepsVisible = false;

    // Playback controls
    private final JButton prepareBtn;
    private final JButton playBtn;
    private final JButton pauseBtn;
    private final JButton stepBtn;
    private final JButton stopBtn;
    private final JSlider speedSlider;
    private final JLabel playbackStatusLabel;

    // Last fetched events (kept so we can show counts)
    private java.util.List<ProtocolEvent> latestEvents = java.util.Collections.emptyList();

    public RightPanel(CanvasPanel canvas, ControlPanel control) {
        this.canvas = canvas;
        this.control = control;

        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 8, 8, 8);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0; gbc.gridy = 0;

        // Title
        add(new JLabel("<html><b>Routing Controls</b></html>"), gbc);

        // Show / Hide tables
        gbc.gridy++;
        showHideTablesBtn = new JButton(canvas.getPerNodeTables() == null ? "Show Routing Tables" : "Hide Routing Tables");
        showHideTablesBtn.addActionListener(e -> toggleRoutingTables());
        add(showHideTablesBtn, gbc);

        // Show / Hide Steps button
        gbc.gridy++;
        showHideStepsBtn = new JButton("Show Steps");
        showHideStepsBtn.addActionListener(e -> toggleStepsArea());
        add(showHideStepsBtn, gbc);

        // Steps text area (initially hidden; made large)
        stepsTextArea = new JTextArea(20, 70); // LARGE text area
        stepsTextArea.setEditable(false);
        stepsTextArea.setLineWrap(true);
        stepsTextArea.setWrapStyleWord(true);
        stepsTextArea.setFont(new Font("Consolas", Font.PLAIN, 13));

        stepsScrollPane = new JScrollPane(stepsTextArea,
        JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

// Make steps area BIG and force layout to expand it
stepsScrollPane.setPreferredSize(new Dimension(700, 350));
stepsScrollPane.setMinimumSize(new Dimension(500, 250));
stepsScrollPane.setVisible(false);

gbc.gridy++;
gbc.fill = GridBagConstraints.BOTH;
gbc.weightx = 1.0;
gbc.weighty = 1.0;        // THIS is what was missing!
add(stepsScrollPane, gbc);

// reset for next controls
gbc.weighty = 0.0;
gbc.fill = GridBagConstraints.HORIZONTAL;


        // Divider
        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 0.0;
        add(new JSeparator(SwingConstants.HORIZONTAL), gbc);

        // Playback controls header
        gbc.gridy++;
        add(new JLabel("<html><b>Playback Controls</b></html>"), gbc);

        // Prepare Events button (runs engine and fetches events, but does not auto-play)
        gbc.gridy++;
        prepareBtn = new JButton("Prepare Events");
        prepareBtn.setToolTipText("Run chosen algorithm to generate structured events (for playback).");
        prepareBtn.addActionListener(e -> prepareEvents());
        add(prepareBtn, gbc);

        // Play / Pause / Step / Stop buttons (in a row)
        gbc.gridy++;
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        playBtn = new JButton("Play");
        pauseBtn = new JButton("Pause");
        stepBtn = new JButton("Step");
        stopBtn = new JButton("Stop");

        playBtn.addActionListener(e -> doPlay());
        pauseBtn.addActionListener(e -> canvas.pausePlayback());
        stepBtn.addActionListener(e -> canvas.stepPlayback());
        stopBtn.addActionListener(e -> canvas.stopPlayback());

        row.add(playBtn);
        row.add(pauseBtn);
        row.add(stepBtn);
        row.add(stopBtn);

        add(row, gbc);

        // Speed slider
        gbc.gridy++;
        JPanel speedRow = new JPanel(new BorderLayout(6, 0));
        speedRow.add(new JLabel("Speed (ms/event):"), BorderLayout.WEST);
        speedSlider = new JSlider(50, 1200, 400);
        speedSlider.setMajorTickSpacing(300);
        speedSlider.setMinorTickSpacing(50);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(false);
        speedRow.add(speedSlider, BorderLayout.CENTER);
        add(speedRow, gbc);

        // Playback status label
        gbc.gridy++;
        playbackStatusLabel = new JLabel("No events prepared");
        add(playbackStatusLabel, gbc);

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

        // Refresh button (also refreshes steps area)
        gbc.gridy++;
        JButton refreshBtn = new JButton("Refresh Nodes / Steps");
        refreshBtn.addActionListener(e -> {
            refreshNodeList();
            if (stepsVisible) loadStepsIntoArea();
            updatePlaybackStatus();
        });
        add(refreshBtn, gbc);

        // Spacer at bottom
        gbc.gridy++;
        gbc.weighty = 1.0;
        add(Box.createVerticalGlue(), gbc);

        setPreferredSize(new Dimension(360, 0));
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

    /** toggle routing tables on/off (existing functionality) */
    private void toggleRoutingTables() {
        if (canvas.getPerNodeTables() != null) {
            canvas.setPerNodeTables(null);
            showHideTablesBtn.setText("Show Routing Tables");
        } else {
            String algo = control.getSelectedAlgorithm();
            Map<String, Map<String, RoutingTableEntry>> tables = canvas.computeRoutingTables(algo);
            canvas.setPerNodeTables(tables);
            showHideTablesBtn.setText("Hide Routing Tables");
        }
        canvas.repaint();
    }

    /** Toggle the visibility of the steps/logs area. */
    private void toggleStepsArea() {
        stepsVisible = !stepsVisible;
        stepsScrollPane.setVisible(stepsVisible);
        showHideStepsBtn.setText(stepsVisible ? "Hide Steps" : "Show Steps");
        if (stepsVisible) loadStepsIntoArea();
        revalidate();
        repaint();
    }

    /** Load latest logs from canvas into the text area. */
    private void loadStepsIntoArea() {
        java.util.List<String> logs = canvas.getLastStepLogs();
        StringBuilder sb = new StringBuilder();
        if (logs == null || logs.isEmpty()) {
            sb.append("No step logs available. Run an algorithm (Run Algorithm) to populate step logs.\n");
            sb.append("Tip: implement logging methods (getLogs or runWithLogging) in your engines for full details.");
        } else {
            for (String line : logs) {
                sb.append(line).append("\n");
            }
        }
        stepsTextArea.setText(sb.toString());
        stepsTextArea.setCaretPosition(0);
    }

    /** Prepare events by running the selected algorithm on the canvas/engine. */
        /** Prepare events by running the selected algorithm on the canvas/engine. (NOW runs in background) */
    private void prepareEvents() {
        String algo = control.getSelectedAlgorithm();
        if (algo == null) {
            JOptionPane.showMessageDialog(this, "Please select an algorithm in the control panel first.");
            return;
        }

        // disable UI controls while preparing
        prepareBtn.setEnabled(false);
        playBtn.setEnabled(false);
        pauseBtn.setEnabled(false);
        stepBtn.setEnabled(false);
        stopBtn.setEnabled(false);

        // show a simple modal progress dialog
        final JDialog progress = new JDialog(SwingUtilities.getWindowAncestor(this), "Preparing events...", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel p = new JPanel(new BorderLayout(8,8));
        p.setBorder(BorderFactory.createEmptyBorder(12,12,12,12));
        p.add(new JLabel("Running algorithm and generating events. Please wait..."), BorderLayout.NORTH);
        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        p.add(bar, BorderLayout.CENTER);
        progress.getContentPane().add(p);
        progress.pack();
        progress.setLocationRelativeTo(this);

        // background worker
        SwingWorker<java.util.List<ProtocolEvent>, Void> worker = new SwingWorker<>() {
            @Override
            protected java.util.List<ProtocolEvent> doInBackground() {
                // run engine off-EDT
                try {
                    return canvas.fetchEventsForAlgorithm(algo);
                } catch (Throwable ex) {
                    ex.printStackTrace();
                    return java.util.Collections.emptyList();
                }
            }

            @Override
            protected void done() {
                try {
                    java.util.List<ProtocolEvent> evs = get();
                    if (evs == null) evs = java.util.Collections.emptyList();
                    latestEvents = evs;
                    // populate steps text area with event lines
                    java.util.List<String> lines = new java.util.ArrayList<>();
                    for (ProtocolEvent e : latestEvents) lines.add(e.toString());
                    stepsTextArea.setText(String.join("\n", lines));
                    stepsTextArea.setCaretPosition(0);
                    canvas.stopPlayback(); // ensure old playback stopped
                    updatePlaybackStatus();

                    JOptionPane.showMessageDialog(RightPanel.this, "Prepared " + latestEvents.size() + " events for algorithm: " + algo);
                } catch (Exception ex) {
                    ex.printStackTrace();
                    JOptionPane.showMessageDialog(RightPanel.this, "Failed to prepare events: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                } finally {
                    // re-enable controls
                    prepareBtn.setEnabled(true);
                    playBtn.setEnabled(true);
                    pauseBtn.setEnabled(true);
                    stepBtn.setEnabled(true);
                    stopBtn.setEnabled(true);
                    progress.dispose();
                }
            }
        };

        // start worker and show progress dialog
        worker.execute();
        progress.setVisible(true); // will block modal until progress.dispose() in done()
    }


    /** Called when Play pressed — uses prepared events (if any), else warns. */
    private void doPlay() {
        if (latestEvents == null || latestEvents.isEmpty()) {
            // try to auto-prepare
            int opt = JOptionPane.showConfirmDialog(this, "No events prepared. Run and prepare now?", "No events", JOptionPane.YES_NO_OPTION);
            if (opt != JOptionPane.YES_OPTION) return;
            prepareEvents();
            if (latestEvents == null || latestEvents.isEmpty()) {
                JOptionPane.showMessageDialog(this, "No events produced by the algorithm.");
                return;
            }
        }
        int delay = speedSlider.getValue();
        canvas.playEvents(latestEvents, delay, true);
        updatePlaybackStatusPeriodically();
    }

    /** Update the playback status label to reflect index/total. */
    private void updatePlaybackStatus() {
        int idx = canvas.getPlaybackIndex();
        int total = canvas.getPlaybackTotal();
        if (total <= 0) {
            playbackStatusLabel.setText("No events prepared");
        } else {
            if (idx < 0) playbackStatusLabel.setText("Ready — " + total + " events (not started)");
            else playbackStatusLabel.setText(String.format("Event %d / %d", idx + 1, total));
        }
    }

    /** Continuously update playback status while playing (simple Swing Timer). */
    private void updatePlaybackStatusPeriodically() {
        Timer t = new Timer(200, null);
        t.addActionListener(e -> {
            updatePlaybackStatus();
            // highlight current event in steps area by moving caret
            int idx = canvas.getPlaybackIndex();
            if (idx >= 0) {
                // find the start offset of the idx-th line
                String[] lines = stepsTextArea.getText().split("\\n");
                if (idx < lines.length) {
                    int pos = 0;
                    for (int i = 0; i < idx; ++i) pos += lines[i].length() + 1;
                    stepsTextArea.setCaretPosition(Math.min(pos, stepsTextArea.getText().length()));
                }
            }
            if (!canvas.isDisplayable() || (canvas.getPlaybackTotal() > 0 && canvas.getPlaybackIndex() >= canvas.getPlaybackTotal() - 1)) {
                ((Timer)e.getSource()).stop();
                updatePlaybackStatus();
            }
        });
        t.setInitialDelay(0);
        t.start();
    }

    /** Expose latest events so other UI pieces can inspect them. */
    public java.util.List<ProtocolEvent> getLatestEvents() {
        return latestEvents == null ? java.util.Collections.emptyList() : new java.util.ArrayList<>(latestEvents);
    }
}
