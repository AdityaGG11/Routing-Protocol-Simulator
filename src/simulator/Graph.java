package simulator;

import java.util.*;

/**
 * Simple Graph container for Nodes and Edges.
 * Keeps insertion order (LinkedHashMap) so behavior is predictable for tests/screenshots.
 *
 * Save to: src/simulator/Graph.java
 */
public class Graph {
    private final Map<String, Node> nodes = new LinkedHashMap<>();
    private final Map<String, Edge> edges = new LinkedHashMap<>();

    // Node operations
    public boolean addNode(String id, int x, int y) {
        if (id == null || id.isEmpty() || nodes.containsKey(id)) return false;
        nodes.put(id, new Node(id, x, y));
        return true;
    }

    public boolean addNode(String id) {
        return addNode(id, 0, 0);
    }

    public boolean removeNode(String id) {
        if (!nodes.containsKey(id)) return false;
        // remove incident edges
        List<String> toRemove = new ArrayList<>();
        for (Edge e : edges.values()) {
            if (e.getA().equals(id) || e.getB().equals(id)) toRemove.add(e.getId());
        }
        for (String eid : toRemove) removeEdge(eid);
        nodes.remove(id);
        // remove neighbor references in remaining nodes
        for (Node n : nodes.values()) n.removeNeighbor(id);
        return true;
    }

    public Node getNode(String id) {
        return nodes.get(id);
    }

    public Collection<Node> getNodes() {
        return Collections.unmodifiableCollection(nodes.values());
    }

    // Edge operations
    public boolean addEdge(String id, String a, String b, int cost) {
        if (id == null || id.isEmpty() || edges.containsKey(id)) return false;
        if (!nodes.containsKey(a) || !nodes.containsKey(b)) return false;
        Edge e = new Edge(id, a, b, cost);
        edges.put(id, e);
        nodes.get(a).addNeighbor(b, id);
        nodes.get(b).addNeighbor(a, id);
        return true;
    }

    public boolean removeEdge(String id) {
        Edge e = edges.remove(id);
        if (e == null) return false;
        Node na = nodes.get(e.getA());
        Node nb = nodes.get(e.getB());
        if (na != null) na.removeNeighbor(nb.getId());
        if (nb != null) nb.removeNeighbor(na.getId());
        return true;
    }

    public Edge getEdge(String id) {
        return edges.get(id);
    }

    public Collection<Edge> getEdges() {
        return Collections.unmodifiableCollection(edges.values());
    }

    /**
     * Find an edge between two node IDs (undirected).
     * Returns the first matching Edge or null.
     */
    public Edge findEdgeBetween(String a, String b) {
        for (Edge e : edges.values()) {
            if ((e.getA().equals(a) && e.getB().equals(b)) || (e.getA().equals(b) && e.getB().equals(a))) {
                return e;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "Graph[nodes=" + nodes.keySet() + ", edges=" + edges.keySet() + "]";
    }
}
