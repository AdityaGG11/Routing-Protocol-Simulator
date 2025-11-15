package simulator;

import java.util.*;

/**
 * Simple Node model for the routing simulator.
 * Keeps an id, optional canvas coordinates (x,y), and references to incident edges.
 *
 * Save to: src/simulator/Node.java
 */
public class Node {
    private final String id;
    private int x;
    private int y;
    // map neighborId -> edgeId (we store edges in Graph; this keeps quick neighbor lookup)
    private final Map<String, String> neighborEdges = new LinkedHashMap<>();

    public Node(String id) {
        this(id, 0, 0);
    }

    public Node(String id, int x, int y) {
        this.id = id;
        this.x = x;
        this.y = y;
    }

    public String getId() { return id; }
    public int getX() { return x; }
    public int getY() { return y; }
    public void setPosition(int x, int y) { this.x = x; this.y = y; }

    public void addNeighbor(String neighborId, String edgeId) {
        neighborEdges.put(neighborId, edgeId);
    }

    public void removeNeighbor(String neighborId) {
        neighborEdges.remove(neighborId);
    }

    public boolean hasNeighbor(String neighborId) {
        return neighborEdges.containsKey(neighborId);
    }

    public Set<String> getNeighborIds() {
        return Collections.unmodifiableSet(neighborEdges.keySet());
    }

    public String getEdgeIdForNeighbor(String neighborId) {
        return neighborEdges.get(neighborId);
    }

    @Override
    public String toString() {
        return "Node[" + id + "]";
    }
}


