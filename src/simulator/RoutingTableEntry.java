package simulator;

/**
 * Simple routing table entry used by routing engines.
 * Represents one row: destination, next hop, cost.
 *
 * Save to: src/simulator/RoutingTableEntry.java
 */
public class RoutingTableEntry {
    private final String destination;
    private String nextHop; // null or destination itself for direct
    private int cost;       // Integer.MAX_VALUE means unreachable

    public static final int INFINITY = Integer.MAX_VALUE / 4;

    public RoutingTableEntry(String destination, String nextHop, int cost) {
        this.destination = destination;
        this.nextHop = nextHop;
        this.cost = cost;
    }

    public String getDestination() { return destination; }
    public String getNextHop() { return nextHop; }
    public void setNextHop(String nextHop) { this.nextHop = nextHop; }

    public int getCost() { return cost; }
    public void setCost(int cost) { this.cost = cost; }

    public boolean isInfinity() {
        return cost >= INFINITY;
    }

    @Override
    public String toString() {
        String costStr = isInfinity() ? "INF" : Integer.toString(cost);
        return "RTEntry[dst=" + destination + ", next=" + nextHop + ", cost=" + costStr + "]";
    }
}

