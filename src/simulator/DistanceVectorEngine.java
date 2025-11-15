package simulator;

import java.util.*;

/**
 * Simple Distance Vector engine for the simulator.
 *
 * Behavior:
 * - Each node maintains a routing table (destination -> RoutingTableEntry).
 * - Initialization: direct neighbors set cost=edge.cost, nextHop=neighbor.
 * - On each iteration, every node "sends" its distance vector to every neighbor.
 * - When a neighbor receives a vector, it updates its table using:
 *     cost(v -> dest) = cost(v -> neighbor) + cost(neighbor -> dest)
 *   If smaller, update nextHop to the neighbor and cost accordingly.
 *
 * This is synchronous (round-based) and deterministic for easy testing.
 */
public class DistanceVectorEngine {
    private final Graph graph;
    // tables: nodeId -> (destinationId -> RoutingTableEntry)
    private final Map<String, Map<String, RoutingTableEntry>> tables = new LinkedHashMap<>();

    public DistanceVectorEngine(Graph graph) {
        this.graph = graph;
        initTables();
    }

    /** Initialize routing tables for all nodes. */
    private void initTables() {
        tables.clear();
        // collect all node ids
        List<String> nodeIds = new ArrayList<>();
        for (Node n : graph.getNodes()) nodeIds.add(n.getId());

        for (String nodeId : nodeIds) {
            Map<String, RoutingTableEntry> rt = new LinkedHashMap<>();
            // initialize all destinations
            for (String dst : nodeIds) {
                if (dst.equals(nodeId)) {
                    rt.put(dst, new RoutingTableEntry(dst, nodeId, 0));
                } else {
                    rt.put(dst, new RoutingTableEntry(dst, null, RoutingTableEntry.INFINITY));
                }
            }
            tables.put(nodeId, rt);
        }

        // set direct neighbors
        for (Node n : graph.getNodes()) {
            String u = n.getId();
            for (String neighborId : n.getNeighborIds()) {
                String edgeId = n.getEdgeIdForNeighbor(neighborId);
                Edge e = graph.getEdge(edgeId);
                if (e != null && e.isUp()) {
                    Map<String, RoutingTableEntry> rt = tables.get(u);
                    rt.put(neighborId, new RoutingTableEntry(neighborId, neighborId, e.getCost()));
                }
            }
        }
    }

    /**
     * Perform one synchronous DV iteration (one round of exchange).
     * Returns true if any routing table changed during this iteration.
     */
    public boolean runIteration() {
        boolean anyChange = false;
        // snapshot of current tables to simulate simultaneous send/receive
        Map<String, Map<String, Integer>> snapshot = new HashMap<>();
        for (Map.Entry<String, Map<String, RoutingTableEntry>> e : tables.entrySet()) {
            Map<String, Integer> vec = new HashMap<>();
            for (RoutingTableEntry r : e.getValue().values()) {
                vec.put(r.getDestination(), r.getCost());
            }
            snapshot.put(e.getKey(), vec);
        }

        // For each node v, consider each neighbor u and try updates using u's vector
        for (Node vNode : graph.getNodes()) {
            String v = vNode.getId();
            Map<String, RoutingTableEntry> vTable = tables.get(v);

            for (String u : vNode.getNeighborIds()) {
                // skip if edge down
                Edge vuEdge = graph.getEdge(vNode.getEdgeIdForNeighbor(u));
                if (vuEdge == null || !vuEdge.isUp()) continue;
                int costVtoU = vuEdge.getCost();

                Map<String, Integer> uVector = snapshot.get(u);
                if (uVector == null) continue;

                // Attempt to relax each destination via neighbor u
                for (Map.Entry<String, Integer> entry : uVector.entrySet()) {
                    String dest = entry.getKey();
                    int costUtoDest = entry.getValue();
                    // if neighbor reports unreachable, skip
                    if (costUtoDest >= RoutingTableEntry.INFINITY) continue;

                    long possible = (long) costVtoU + (long) costUtoDest;
                    int possibleCost = (possible >= RoutingTableEntry.INFINITY) ? RoutingTableEntry.INFINITY : (int) possible;

                    RoutingTableEntry current = vTable.get(dest);
                    int currentCost = (current == null) ? RoutingTableEntry.INFINITY : current.getCost();

                    if (possibleCost < currentCost) {
                        // update with nextHop = u (we choose neighbor as next hop)
                        vTable.put(dest, new RoutingTableEntry(dest, u, possibleCost));
                        anyChange = true;
                    }
                }
            }
        }

        return anyChange;
    }

    /**
     * Run iterations until convergence or until maxRounds reached.
     * Returns number of rounds executed.
     */
    public int runUntilConverged(int maxRounds) {
        int rounds = 0;
        while (rounds < maxRounds) {
            rounds++;
            boolean changed = runIteration();
            if (!changed) break;
        }
        return rounds;
    }

    /** Get a copy of the routing table for a node. */
    public Map<String, RoutingTableEntry> getRoutingTable(String nodeId) {
        Map<String, RoutingTableEntry> src = tables.get(nodeId);
        if (src == null) return null;
        Map<String, RoutingTableEntry> copy = new LinkedHashMap<>();
        for (Map.Entry<String, RoutingTableEntry> e : src.entrySet()) {
            RoutingTableEntry r = e.getValue();
            copy.put(e.getKey(), new RoutingTableEntry(r.getDestination(), r.getNextHop(), r.getCost()));
        }
        return copy;
    }

    /** Utility: print all routing tables to stdout (for quick tests). */
    public void printAllTables() {
        for (String nodeId : tables.keySet()) {
            System.out.println("Routing table for " + nodeId + ":");
            Map<String, RoutingTableEntry> rt = tables.get(nodeId);
            for (RoutingTableEntry r : rt.values()) {
                String cost = r.isInfinity() ? "INF" : Integer.toString(r.getCost());
                System.out.printf("  dst=%s next=%s cost=%s%n", r.getDestination(),
                        (r.getNextHop() == null ? "-" : r.getNextHop()), cost);
            }
            System.out.println();
        }
    }
}
