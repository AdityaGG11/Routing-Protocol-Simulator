package simulator;

import java.util.Map;

/**
 * Updated test harness for LinkStateEngine using the new API:
 * - runWithLogging()
 * - getLogs()
 * - getEvents()
 * - getRoutingTable(nodeId)
 */
public class MainTestLS {
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

        LinkStateEngine ls = new LinkStateEngine(g);
        System.out.println("Running Link State with logging/events...");
        boolean ok = ls.runWithLogging();

        System.out.println("\n=== Done (ok=" + ok + "), convergedIterations=" + ls.getConvergedIterations() + " ===\n");

        System.out.println("=== Text Logs ===");
        for (String line : ls.getLogs()) {
            System.out.println(line);
        }

        System.out.println("\n=== Events ===");
        for (ProtocolEvent e : ls.getEvents()) {
            System.out.println(e);
        }

        System.out.println("\n=== Final Routing Tables ===");
        for (Node n : g.getNodes()) {
            String id = n.getId();
            System.out.println("Routing table for " + id + ":");
            Map<String, RoutingTableEntry> rt = ls.getRoutingTable(id);
            for (RoutingTableEntry e : rt.values()) {
                String nxt = (e.getNextHop() == null ? "-" : e.getNextHop());
                String cost = (e.isInfinity() ? "INF" : Integer.toString(e.getCost()));
                System.out.printf("  dst=%s next=%s cost=%s%n", e.getDestination(), nxt, cost);
            }
            System.out.println();
        }
    }
}
