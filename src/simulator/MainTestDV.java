package simulator;

import java.util.Map;

/**
 * Updated test harness for DistanceVectorEngine using the new API:
 * - runWithLogging(maxIter)
 * - getLogs()
 * - getEvents()
 * - getRoutingTable(nodeId)
 */
public class MainTestDV {
    public static void main(String[] args) {
        // Build sample graph (A,B,C,D)
        Graph g = new Graph();
        g.addNode("A", 100, 100);
        g.addNode("B", 200, 100);
        g.addNode("C", 300, 100);
        g.addNode("D", 400, 100);

        g.addEdge("e1", "A", "B", 1);
        g.addEdge("e2", "B", "C", 3);
        g.addEdge("e3", "C", "D", 2);
        g.addEdge("e4", "B", "D", 5);

        DistanceVectorEngine dv = new DistanceVectorEngine(g);
        System.out.println("Running Distance Vector with logging/events...");
        boolean converged = dv.runWithLogging(50);

        System.out.println("\n=== Converged: " + converged + " (after " + dv.getConvergedIterations() + " iterations) ===\n");

        System.out.println("=== Text Logs ===");
        for (String line : dv.getLogs()) {
            System.out.println(line);
        }

        System.out.println("\n=== Events ===");
        for (ProtocolEvent ev : dv.getEvents()) {
            System.out.println(ev);
        }

        System.out.println("\n=== Final Routing Tables ===");
        for (Node n : g.getNodes()) {
            System.out.println("Routing table for " + n.getId() + ":");
            Map<String, RoutingTableEntry> rt = dv.getRoutingTable(n.getId());
            for (RoutingTableEntry e : rt.values()) {
                String nxt = e.getNextHop() == null ? "-" : e.getNextHop();
                String cost = e.isInfinity() ? "INF" : Integer.toString(e.getCost());
                System.out.printf("  dst=%s next=%s cost=%s%n", e.getDestination(), nxt, cost);
            }
            System.out.println();
        }
    }
}
