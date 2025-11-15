package simulator;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.util.Map;

/**
 * Panel that displays routing tables for all routers in a scrollable grid.
 * Each router gets a small JTable showing Destination | Next Hop | Cost.
 */
public class RoutingTablesPanel extends JPanel {

    private final JPanel tablesContainer;

    public RoutingTablesPanel() {
        setLayout(new BorderLayout());
        JLabel title = new JLabel("Routing Tables", SwingConstants.CENTER);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 16f));
        title.setBorder(new EmptyBorder(8, 8, 8, 8));
        add(title, BorderLayout.NORTH);

        tablesContainer = new JPanel();
        // Use FlowLayout so panels wrap automatically
        tablesContainer.setLayout(new FlowLayout(FlowLayout.LEFT, 12, 12));
        JScrollPane sp = new JScrollPane(tablesContainer);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        add(sp, BorderLayout.CENTER);
    }

    /** Replace displayed tables with the given data. */
    public void updateTables(Map<String, Map<String, RoutingTableEntry>> tables) {
        tablesContainer.removeAll();
        if (tables == null || tables.isEmpty()) {
            JLabel blank = new JLabel("No routing tables to show.");
            blank.setBorder(new EmptyBorder(12,12,12,12));
            tablesContainer.add(blank);
        } else {
            for (Map.Entry<String, Map<String, RoutingTableEntry>> e : tables.entrySet()) {
                String nodeId = e.getKey();
                Map<String, RoutingTableEntry> rt = e.getValue();
                tablesContainer.add(createSingleTablePanel(nodeId, rt));
            }
        }
        revalidate();
        repaint();
    }

    private JComponent createSingleTablePanel(String nodeId, Map<String, RoutingTableEntry> rt) {
        JPanel p = new JPanel(new BorderLayout());
        p.setPreferredSize(new Dimension(220, 200));
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.DARK_GRAY),
                BorderFactory.createEmptyBorder(6,6,6,6)
        ));

        JLabel header = new JLabel("Router " + nodeId, SwingConstants.CENTER);
        header.setFont(header.getFont().deriveFont(Font.BOLD, 14f));
        p.add(header, BorderLayout.NORTH);

        String[] cols = {"Destination", "Next Hop", "Cost"};
        DefaultTableModel model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };

        if (rt != null) {
            for (RoutingTableEntry r : rt.values()) {
                String cost = r.isInfinity() ? "INF" : Integer.toString(r.getCost());
                model.addRow(new Object[]{ r.getDestination(), r.getNextHop() == null ? "-" : r.getNextHop(), cost });
            }
        }

        JTable table = new JTable(model);
        table.setFont(table.getFont().deriveFont(12f));
        table.setRowHeight(20);
        table.getTableHeader().setReorderingAllowed(false);

        JScrollPane tsp = new JScrollPane(table);
        p.add(tsp, BorderLayout.CENTER);

        return p;
    }
}
