package simulator;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

/**
 * CanvasPanel with per-node inline routing table drawing and:
 *  - moveable routing-table boxes (drag to reposition)
 *  - improved table spacing and reduced fonts
 *  - interactive delete modes: delete-node and delete-edge
 */
public class CanvasPanel extends JPanel {
    private final Graph graph;
    private char nextLabel = 'A';
    private Node draggingNode = null;
    private Point dragOffset = null;

    // delete modes
    private boolean deleteNodeMode = false;
    private boolean deleteEdgeMode = false;

    // table dragging
    private String draggingTableNodeId = null;
    private Point tableDragOffset = null; // offset between mouse and table top-left while dragging
    private final Map<String, Point> tablePositions = new LinkedHashMap<>(); // nodeId -> absolute top-left
    private final Set<String> tableManualPositions = new HashSet<>(); // nodeIds whose table position was manually moved

    // modes
    private boolean addEdgeMode = false;
    private boolean addNodeMode = false;
    private String edgeFirstNode = null;

    // default cost for add-edge (can be overridden by prompt)
    private int defaultLinkCost = 1;

    // flash state for edges/nodes (edgeId/nodeId -> untilMillis)
    private final Map<String, Long> edgeFlashUntil = new HashMap<>();
    private final Map<String, Long> nodeFlashUntil = new HashMap<>();

    // per-node routing tables to draw inline: nodeId -> (dest -> entry)
    private Map<String, Map<String, RoutingTableEntry>> perNodeTables = null;

    // visual constants - adjusted for requested spacing/font
    private static final int NODE_RADIUS = 28; // used for hit detection
    private static final int TABLE_BOX_WIDTH = 240;   // slightly wider
    private static final int TABLE_ROW_HEIGHT = 24;   // taller rows
    private static final int TABLE_HEADER_HEIGHT = 26; // header slightly taller
    private static final int TABLE_MAX_ROWS = 6;
    private static final int TABLE_PADDING = 8;

    public CanvasPanel(Graph graph) {
        this.graph = graph;
        setBackground(new Color(245, 248, 252));

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                Point p = e.getPoint();

                // 1) If table hit -> start dragging table (no delete action)
                String tableHit = findTableAtPoint(p);
                if (tableHit != null) {
                    draggingTableNodeId = tableHit;
                    Point topLeft = tablePositions.get(tableHit);
                    if (topLeft == null) {
                        topLeft = defaultTablePosition(graph.getNode(tableHit));
                        tablePositions.put(tableHit, topLeft);
                    }
                    tableDragOffset = new Point(p.x - topLeft.x, p.y - topLeft.y);
                    tableManualPositions.add(tableHit);
                    return;
                }

                // 2) Delete modes take precedence:
                if (deleteNodeMode) {
                    String clickedNode = findNodeAt(p);
                    if (clickedNode != null) {
                        int confirm = JOptionPane.showConfirmDialog(CanvasPanel.this,
                                "Delete router " + clickedNode + " and its links?", "Confirm Delete",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            graph.removeNode(clickedNode);
                            // clear tables and table positions for that node
                            perNodeTables = null;
                            tablePositions.remove(clickedNode);
                            tableManualPositions.remove(clickedNode);
                            fireNodeListChanged();
                            repaint();
                        }
                    }
                    return; // consume click
                }

                if (deleteEdgeMode) {
                    String edgeId = findEdgeAtPoint(p);
                    if (edgeId != null) {
                        int confirm = JOptionPane.showConfirmDialog(CanvasPanel.this,
                                "Delete link " + edgeId + " ?", "Confirm Delete",
                                JOptionPane.YES_NO_OPTION);
                        if (confirm == JOptionPane.YES_OPTION) {
                            graph.removeEdge(edgeId);
                            perNodeTables = null;
                            repaint();
                        }
                    }
                    return; // consume click
                }

                // 3) Normal behavior (add modes, dragging nodes)
                String clickedNode = findNodeAt(p);
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (addEdgeMode) {
                        handleAddEdgeClick(clickedNode);
                    } else if (addNodeMode) {
                        if (clickedNode == null) {
                            String id = nextNodeLabel();
                            graph.addNode(id, p.x, p.y);
                            fireNodeListChanged();
                            repaint();
                        } else {
                            draggingNode = graph.getNode(clickedNode);
                            dragOffset = new Point(p.x - draggingNode.getX(), p.y - draggingNode.getY());
                        }
                    } else {
                        if (clickedNode != null) {
                            draggingNode = graph.getNode(clickedNode);
                            dragOffset = new Point(p.x - draggingNode.getX(), p.y - draggingNode.getY());
                        }
                    }
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                draggingNode = null;
                dragOffset = null;
                draggingTableNodeId = null;
                tableDragOffset = null;
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                Point p = e.getPoint();
                if (draggingTableNodeId != null && tableDragOffset != null) {
                    int nx = p.x - tableDragOffset.x;
                    int ny = p.y - tableDragOffset.y;
                    nx = Math.max(4, Math.min(nx, getWidth() - TABLE_BOX_WIDTH - 4));
                    ny = Math.max(4, Math.min(ny, getHeight() - calculateTableHeight(draggingTableNodeId) - 4));
                    tablePositions.put(draggingTableNodeId, new Point(nx, ny));
                    repaint();
                    return;
                }

                if (draggingNode != null && dragOffset != null) {
                    int nx = e.getX() - dragOffset.x;
                    int ny = e.getY() - dragOffset.y;
                    draggingNode.setPosition(nx, ny);
                    for (Node n : graph.getNodes()) {
                        String nid = n.getId();
                        if (!tableManualPositions.contains(nid) && perNodeTables != null && perNodeTables.containsKey(nid)) {
                            Point def = defaultTablePosition(n);
                            tablePositions.put(nid, def);
                        }
                    }
                    repaint();
                }
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);

        // animation timer to repaint flashing visuals (use Swing Timer)
        javax.swing.Timer animTimer = new javax.swing.Timer(60, ev -> {
            boolean need = false;
            long now = System.currentTimeMillis();
            Iterator<Map.Entry<String, Long>> it = edgeFlashUntil.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Long> en = it.next();
                if (en.getValue() <= now) it.remove();
                else need = true;
            }
            Iterator<Map.Entry<String, Long>> it2 = nodeFlashUntil.entrySet().iterator();
            while (it2.hasNext()) {
                Map.Entry<String, Long> en = it2.next();
                if (en.getValue() <= now) it2.remove();
                else need = true;
            }
            if (need) repaint();
        });
        animTimer.setRepeats(true);
        animTimer.start();
    }

    // ---------- public API ----------
    public void setAddEdgeMode(boolean on) {
        this.addEdgeMode = on;
        this.edgeFirstNode = null;
        setCursor(on ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    public boolean isAddEdgeMode() { return addEdgeMode; }

    public void setAddNodeMode(boolean on) {
        this.addNodeMode = on;
        setCursor(on ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR) : Cursor.getDefaultCursor());
        repaint();
    }

    public boolean isAddNodeMode() { return addNodeMode; }

    public void setDefaultLinkCost(int cost) { this.defaultLinkCost = Math.max(1, cost); }
    public int getDefaultLinkCost() { return defaultLinkCost; }

    public List<String> getNodeIds() {
        List<String> ids = new ArrayList<>();
        for (Node n : graph.getNodes()) ids.add(n.getId());
        return ids;
    }

    public void setDeleteNodeMode(boolean on) {
        this.deleteNodeMode = on;
        if (on) {
            // ensure other modes off
            this.deleteEdgeMode = false;
            this.addEdgeMode = false;
            this.addNodeMode = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public boolean isDeleteNodeMode() { return deleteNodeMode; }

    public void setDeleteEdgeMode(boolean on) {
        this.deleteEdgeMode = on;
        if (on) {
            this.deleteNodeMode = false;
            this.addEdgeMode = false;
            this.addNodeMode = false;
            setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public boolean isDeleteEdgeMode() { return deleteEdgeMode; }

    /** Allow external code to provide per-node tables to draw inline. */
    public void setPerNodeTables(Map<String, Map<String, RoutingTableEntry>> tables) {
        this.perNodeTables = (tables == null) ? null : new LinkedHashMap<>(tables);

        // ensure default positions for new tables
        if (perNodeTables != null) {
            for (Node n : graph.getNodes()) {
                String nid = n.getId();
                if (perNodeTables.containsKey(nid) && !tablePositions.containsKey(nid)) {
                    tablePositions.put(nid, defaultTablePosition(n));
                }
            }
        }
        repaint();
    }

    public Map<String, Map<String, RoutingTableEntry>> getPerNodeTables() {
        return perNodeTables;
    }

    /** Compute routing tables for selected algorithm and return map. */
    public Map<String, Map<String, RoutingTableEntry>> computeRoutingTables(String algorithm) {
        Map<String, Map<String, RoutingTableEntry>> result = new LinkedHashMap<>();
        if ("Distance Vector".equals(algorithm)) {
            DistanceVectorEngine dv = new DistanceVectorEngine(graph);
            dv.runUntilConverged(50);
            for (Node n : graph.getNodes()) {
                result.put(n.getId(), dv.getRoutingTable(n.getId()));
            }
        } else {
            LinkStateEngine ls = new LinkStateEngine(graph);
            for (Node n : graph.getNodes()) {
                result.put(n.getId(), ls.getRoutingTable(n.getId()));
            }
        }
        return result;
    }

    // ---------- internals ----------
    private void handleAddEdgeClick(String clickedNodeId) {
        if (clickedNodeId == null) return;
        if (edgeFirstNode == null) {
            edgeFirstNode = clickedNodeId;
        } else if (edgeFirstNode.equals(clickedNodeId)) {
            edgeFirstNode = null;
        } else {
            String from = edgeFirstNode;
            String to = clickedNodeId;
            String input = JOptionPane.showInputDialog(this,
                    "Enter cost (leave blank to use default " + defaultLinkCost + ") for link " + from + "-" + to,
                    Integer.toString(defaultLinkCost));
            int cost = defaultLinkCost;
            if (input != null && !input.trim().isEmpty()) {
                try {
                    cost = Math.max(1, Integer.parseInt(input.trim()));
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number. Using default cost " + defaultLinkCost);
                    cost = defaultLinkCost;
                }
            }
            String eid = "e" + (graph.getEdges().size() + 1);
            graph.addEdge(eid, from, to, cost);
            edgeFirstNode = null;
            fireNodeListChanged();
            repaint();
        }
    }

    private void fireNodeListChanged() {
        Component top = SwingUtilities.getRoot(this);
        if (top instanceof JFrame) {
            for (Component c : ((JFrame) top).getContentPane().getComponents()) {
                if (c instanceof RightPanel) {
                    ((RightPanel) c).refreshNodeList();
                }
            }
        }
    }

    /** Flash an edge visually for ms milliseconds */
    public void flashEdge(String edgeId, int ms) {
        edgeFlashUntil.put(edgeId, System.currentTimeMillis() + ms);
        repaint();
    }

    /** Flash a node visually for ms milliseconds */
    public void flashNode(String nodeId, int ms) {
        nodeFlashUntil.put(nodeId, System.currentTimeMillis() + ms);
        repaint();
    }

    /** Flash edges in sequence */
    public void flashEdgeSequence(int msPerEdge) {
        java.util.List<Edge> edgeList = new ArrayList<>(graph.getEdges());
        if (edgeList.isEmpty()) return;
        final int total = edgeList.size();
        final int[] idx = {0};
        javax.swing.Timer t = new javax.swing.Timer(msPerEdge, null);
        t.addActionListener(e -> {
            if (idx[0] >= total) {
                ((javax.swing.Timer)e.getSource()).stop();
                return;
            }
            Edge edge = edgeList.get(idx[0]++);
            flashEdge(edge.getId(), msPerEdge);
            flashNode(edge.getA(), msPerEdge);
            flashNode(edge.getB(), msPerEdge);
        });
        t.setInitialDelay(0);
        t.start();
    }

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0.create();
        // anti-alias
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw edges
        g.setStroke(new BasicStroke(3f));
        for (Edge e : graph.getEdges()) {
            Node na = graph.getNode(e.getA());
            Node nb = graph.getNode(e.getB());
            if (na == null || nb == null) continue;
            int x1 = na.getX();
            int y1 = na.getY();
            int x2 = nb.getX();
            int y2 = nb.getY();

            boolean flashing = edgeFlashUntil.containsKey(e.getId());
            Color lineColor = flashing ? new Color(30, 136, 229) : (e.isUp() ? new Color(80, 86, 92) : Color.LIGHT_GRAY);
            g.setColor(lineColor);
            g.drawLine(x1, y1, x2, y2);

            int mx = (x1 + x2) / 2;
            int my = (y1 + y2) / 2;
            g.setColor(new Color(13, 71, 161));
            g.setFont(g.getFont().deriveFont(Font.PLAIN, 13f));
            g.drawString(Integer.toString(e.getCost()), mx + 6, my - 6);
        }

        // draw nodes and tables
        for (Node n : graph.getNodes()) {
            boolean flashing = nodeFlashUntil.containsKey(n.getId());
            drawRouterIcon(g, n.getX(), n.getY(), n.getId(), flashing);
        }

        // draw per-node tables (above nodes so they are readable)
        if (perNodeTables != null) {
            for (Node n : graph.getNodes()) {
                Map<String, RoutingTableEntry> rt = perNodeTables.get(n.getId());
                if (rt != null) {
                    Point pos = tablePositions.get(n.getId());
                    if (pos == null) {
                        pos = defaultTablePosition(n);
                        tablePositions.put(n.getId(), pos);
                    }
                    drawRoutingTableBoxAt(g, n.getId(), n, rt, pos.x, pos.y);
                }
            }
        }

        g.dispose();
    }

    // compute default top-left position for a node's table (top-right)
    private Point defaultTablePosition(Node n) {
        int pad = 8;
        int x = n.getX() + NODE_RADIUS + pad;
        int boxH = calculateTableHeight(n.getId());
        int y = n.getY() - boxH / 2;
        // clamp to panel
        if (x + TABLE_BOX_WIDTH + 6 > getWidth()) x = n.getX() - NODE_RADIUS - pad - TABLE_BOX_WIDTH;
        if (x < 6) x = 6;
        if (y < 6) y = 6;
        if (y + boxH + 6 > getHeight()) y = Math.max(6, getHeight() - boxH - 6);
        return new Point(x, y);
    }

    // calculate height for a node's table (based on its rows)
    private int calculateTableHeight(String nodeId) {
        if (perNodeTables == null) return TABLE_HEADER_HEIGHT + TABLE_ROW_HEIGHT * 1 + 8;
        Map<String, RoutingTableEntry> rt = perNodeTables.get(nodeId);
        int rows = 0;
        if (rt != null) rows = Math.min(rt.size(), TABLE_MAX_ROWS);
        return TABLE_HEADER_HEIGHT + rows * TABLE_ROW_HEIGHT + 8;
    }

    // draw table at absolute position
    private void drawRoutingTableBoxAt(Graphics2D g, String nodeId, Node node, Map<String, RoutingTableEntry> rt, int x, int y) {
        int boxW = TABLE_BOX_WIDTH;
        int boxH = calculateTableHeight(nodeId);

        // background
        g.setColor(new Color(252, 252, 252, 230));
        g.fillRoundRect(x, y, boxW, boxH, 10, 10);
        g.setColor(new Color(140, 140, 140));
        g.setStroke(new BasicStroke(1.2f));
        g.drawRoundRect(x, y, boxW, boxH, 10, 10);

        // header (smaller, cleaner)
        Font headerF = g.getFont().deriveFont(Font.BOLD, 11.0f);
        g.setFont(headerF);
        g.setColor(new Color(25,25,25));

        int headerY = y + 16;
        int colDest = x + 10;
        int colNext = x + 110;
        int colCost = x + 185;

        g.drawString("Destination", colDest, headerY);
        g.drawString("Next Hop",    colNext, headerY);
        g.drawString("Cost",        colCost, headerY);

        // divider line under header
        g.setStroke(new BasicStroke(1.4f));
        g.setColor(new Color(180,180,180));
        g.drawLine(x + 6, y + TABLE_HEADER_HEIGHT, x + boxW - 6, y + TABLE_HEADER_HEIGHT);

        // rows
        Font rowF = g.getFont().deriveFont(Font.PLAIN, 10.0f);
        g.setFont(rowF);
        int ry = y + TABLE_HEADER_HEIGHT + TABLE_ROW_HEIGHT - 6;
        int drawn = 0;
        for (RoutingTableEntry entry : rt.values()) {
            if (drawn >= TABLE_MAX_ROWS) break;
            // alternating background
            if (drawn % 2 == 0) {
                g.setColor(new Color(245, 245, 245, 200));
                g.fillRect(x + 6, ry - TABLE_ROW_HEIGHT + 4, boxW - 12, TABLE_ROW_HEIGHT);
            }
            g.setColor(new Color(30,30,30));
            String dst = entry.getDestination();
            String nxt = entry.getNextHop() == null ? "-" : entry.getNextHop();
            String cost = entry.isInfinity() ? "INF" : Integer.toString(entry.getCost());
            g.drawString(dst,  colDest, ry - 3);
            g.drawString(nxt,  colNext, ry - 3);
            g.drawString(cost, colCost, ry - 3);

            ry += TABLE_ROW_HEIGHT;
            drawn++;
        }

        if (rt.size() > TABLE_MAX_ROWS) {
            g.setColor(new Color(120, 120, 120));
            g.drawString("...", x + boxW - 26, y + boxH - 12);
        }

        // small title with node id at top-left of box
        g.setColor(new Color(60,60,60));
        Font small = g.getFont().deriveFont(Font.BOLD, 11f);
        g.setFont(small);
        g.drawString("Router " + nodeId, x + 8, y - 4 + 12);
    }

    // --- EDGE HIT TEST: returns edge id under point or null ---
    private String findEdgeAtPoint(Point p) {
        final double HIT_DIST = 8.0; // pixels tolerance
        String found = null;
        double best = Double.MAX_VALUE;
        for (Edge e : graph.getEdges()) {
            Node a = graph.getNode(e.getA());
            Node b = graph.getNode(e.getB());
            if (a == null || b == null) continue;
            double d = pointToSegmentDistance(p.x, p.y, a.getX(), a.getY(), b.getX(), b.getY());
            if (d < HIT_DIST && d < best) {
                best = d;
                found = e.getId();
            }
        }
        return found;
    }

    // distance from point (px,py) to segment (x1,y1)-(x2,y2)
    private double pointToSegmentDistance(double px, double py, double x1, double y1, double x2, double y2) {
        double dx = x2 - x1;
        double dy = y2 - y1;
        if (dx == 0 && dy == 0) {
            dx = px - x1;
            dy = py - y1;
            return Math.sqrt(dx*dx + dy*dy);
        }
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx*dx + dy*dy);
        t = Math.max(0, Math.min(1, t));
        double projx = x1 + t * dx;
        double projy = y1 + t * dy;
        double ddx = px - projx;
        double ddy = py - projy;
        return Math.sqrt(ddx*ddx + ddy*ddy);
    }

    private String findTableAtPoint(Point p) {
        if (perNodeTables == null) return null;
        for (Node n : graph.getNodes()) {
            String nid = n.getId();
            if (!perNodeTables.containsKey(nid)) continue;
            Point pos = tablePositions.get(nid);
            if (pos == null) pos = defaultTablePosition(n);
            int w = TABLE_BOX_WIDTH;
            int h = calculateTableHeight(nid);
            Rectangle r = new Rectangle(pos.x, pos.y, w, h);
            if (r.contains(p)) return nid;
        }
        return null;
    }

    private void drawRouterIcon(Graphics2D g, int x, int y, String label, boolean highlight) {
        int w = NODE_RADIUS * 2;
        int h = NODE_RADIUS * 2;
        int left = x - NODE_RADIUS;
        int top = y - NODE_RADIUS;

        // drop shadow
        g.setColor(new Color(0,0,0,40));
        g.fillOval(left + 4, top + h - 6, w - 6, 10);

        // body
        g.setColor(highlight ? new Color(255, 235, 59) : new Color(255, 200, 80));
        g.fillRoundRect(left, top + 6, w, h - 10, 18, 18);
        g.setColor(new Color(80, 80, 80));
        g.setStroke(new BasicStroke(2f));
        g.drawRoundRect(left, top + 6, w, h - 10, 18, 18);

        // top dome
        g.setColor(new Color(255, 220, 100));
        g.fillOval(left + 6, top, w - 12, 24);
        g.setColor(new Color(80, 80, 80));
        g.drawOval(left + 6, top, w - 12, 24);

        // small antenna
        g.drawLine(x - 6, top + 6, x - 12, top - 4);
        g.drawLine(x + 6, top + 6, x + 12, top - 4);

        // label centered
        g.setFont(g.getFont().deriveFont(Font.BOLD, 14f));
        FontMetrics fm = g.getFontMetrics();
        int lw = fm.stringWidth(label);
        g.setColor(Color.BLACK);
        g.drawString(label, x - lw / 2, y + fm.getAscent() / 2 - 2);
    }

    private String findNodeAt(Point p) {
        for (Node n : graph.getNodes()) {
            int dx = p.x - n.getX();
            int dy = p.y - n.getY();
            if (dx * dx + dy * dy <= NODE_RADIUS * NODE_RADIUS) {
                return n.getId();
            }
        }
        return null;
    }

    private String nextNodeLabel() {
        if (nextLabel <= 'Z') {
            char c = nextLabel++;
            return String.valueOf(c);
        } else {
            return "N" + (nextLabel++);
        }
    }

    // save/load helpers unchanged
    public void saveToFile(File file) {
        try {
            TopologyIO.save(graph, file);
            JOptionPane.showMessageDialog(this, "Topology saved to " + file.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Save failed: " + ex.getMessage());
        }
    }

    public void loadFromFile(File file) {
        try {
            Graph loaded = TopologyIO.load(file);
            List<String> nodeIds = new ArrayList<>();
            for (Node n : graph.getNodes()) nodeIds.add(n.getId());
            for (String nid : nodeIds) graph.removeNode(nid);

            for (Node n : loaded.getNodes()) graph.addNode(n.getId(), n.getX(), n.getY());
            for (Edge e : loaded.getEdges()) graph.addEdge(e.getId(), e.getA(), e.getB(), e.getCost());
            fireNodeListChanged();
            repaint();
            JOptionPane.showMessageDialog(this, "Topology loaded from " + file.getName());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Load failed: " + ex.getMessage());
        }
    }
}
