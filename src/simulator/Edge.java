package simulator;

/**
 * Simple Edge model for the routing simulator.
 * Represents an undirected link between two node IDs, with a cost and an up/down flag.
 *
 * Save to: src/simulator/Edge.java
 */
public class Edge {
    private final String id;
    private final String a;
    private final String b;
    private int cost;
    private boolean isUp;

    public Edge(String id, String a, String b, int cost) {
        this.id = id;
        this.a = a;
        this.b = b;
        this.cost = cost;
        this.isUp = true;
    }

    public String getId() { return id; }
    public String getA() { return a; }
    public String getB() { return b; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public boolean isUp() { return isUp; }
    public void setUp(boolean up) { this.isUp = up; }

    /**
     * Given one endpoint ID, returns the other endpoint ID.
     * Returns null if the provided id is not an endpoint for this edge.
     */
    public String getOther(String nodeId) {
        if (a.equals(nodeId)) return b;
        if (b.equals(nodeId)) return a;
        return null;
    }

    @Override
    public String toString() {
        return "Edge[" + id + ":" + a + "<->" + b + " cost=" + cost + " up=" + isUp + "]";
    }
}
