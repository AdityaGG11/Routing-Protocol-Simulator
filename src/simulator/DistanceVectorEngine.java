package simulator;

import java.util.*;

/**
 * Distance Vector Engine with structured ProtocolEvent emission.
 *
 * Public API:
 *   - boolean runWithLogging(int maxIter)  // runs DV, collects logs and events
 *   - List<String> getLogs()
 *   - List<ProtocolEvent> getEvents()
 *   - Map<String, RoutingTableEntry> getRoutingTable(String nodeId)
 *   - int getConvergedIterations()
 *
 * Assumes RoutingTableEntry has constructor: RoutingTableEntry(String dest, String nextHop, int cost)
 * and methods: getDestination(), getNextHop(), getCost(), isInfinity() (optional).
 */
public class DistanceVectorEngine {
    private final Graph graph;
    private final Map<String, Map<String, RoutingTableEntry>> tables = new LinkedHashMap<>();
    private final List<String> logs = new ArrayList<>();
    private final List<ProtocolEvent> events = new ArrayList<>();
    private int convergedIterations = -1;
    private static final int INF = 1_000_000_000;

    public DistanceVectorEngine(Graph graph) {
        this.graph = graph;
    }

    /**
     * Run the distance-vector algorithm with logging and event emission.
     * Returns true if converged before maxIter, false otherwise.
     */
    public boolean runWithLogging(int maxIter) {
        logs.clear();
        events.clear();
        tables.clear();
        convergedIterations = -1;

        // build adjacency: node -> (neighbor -> cost)
        Map<String, Map<String, Integer>> adj = buildAdjacency();

        // stable node order
        List<String> nodes = new ArrayList<>();
        for (Node n : graph.getNodes()) nodes.add(n.getId());

        // initialize tables
        for (String u : nodes) {
            Map<String, RoutingTableEntry> t = new LinkedHashMap<>();
            for (String v : nodes) {
                if (u.equals(v)) {
                    t.put(v, new RoutingTableEntry(v, u, 0));
                } else {
                    t.put(v, new RoutingTableEntry(v, null, INF));
                }
            }
            Map<String, Integer> nbrs = adj.getOrDefault(u, Collections.emptyMap());
            for (Map.Entry<String, Integer> e : nbrs.entrySet()) {
                t.put(e.getKey(), new RoutingTableEntry(e.getKey(), e.getKey(), e.getValue()));
            }
            tables.put(u, t);
        }

        logs.add("Distance Vector: initialization complete.");
        events.add(ProtocolEvent.info("Distance Vector: initialization complete."));

        // main iterative exchange
        for (int iter = 1; iter <= maxIter; iter++) {
            boolean anyChange = false;
            logs.add("Iteration " + iter + " begins.");
            events.add(ProtocolEvent.iterationStart(iter));

            // for each node u, simulate receiving neighbor tables and update
            for (String u : nodes) {
                Map<String, RoutingTableEntry> tU = tables.get(u);
                Map<String, Integer> nbrs = adj.getOrDefault(u, Collections.emptyMap());

                // For visualization: each neighbor "sends" its vector to u
                for (String v : nbrs.keySet()) {
                    String edgeId = findEdgeId(u, v);
                    // message send event from v -> u
                    events.add(ProtocolEvent.messageSend(v, u, edgeId, iter,
                            String.format("Routing vector from %s to %s", v, u)));
                }

                // Actually apply Bellman-Ford-like updates using neighbors' tables
                for (String v : nbrs.keySet()) {
                    Map<String, RoutingTableEntry> tV = tables.get(v);
                    int costUV = nbrs.get(v);

                    for (String dest : tV.keySet()) {
                        RoutingTableEntry entryV = tV.get(dest);
                        int vToDest = entryV.getCost();
                        if (vToDest >= INF) continue; // neighbor doesn't reach dest
                        long candidate = (long) costUV + (long) vToDest;
                        RoutingTableEntry cur = tU.get(dest);
                        if (candidate < cur.getCost()) {
                            int newCost = (int) Math.min(candidate, INF);
                            int oldCost = cur.getCost();
                            String oldNext = cur.getNextHop();
                            String newNext = v; // next-hop becomes neighbor v

                            // update the table for u
                            tU.put(dest, new RoutingTableEntry(dest, newNext, newCost));
                            anyChange = true;

                            // create a log line
                            logs.add(String.format("Node %s: updated route to %s via %s (cost %s -> %d).",
                                    u, dest, v, (oldCost >= INF ? "INF" : Integer.toString(oldCost)), newCost));

                            // create a structured event
                            String edgeId = findEdgeId(u, v);
                            events.add(ProtocolEvent.tableUpdate(u, dest, newNext,
                                    (oldCost >= INF ? null : oldCost),
                                    newCost, edgeId, iter,
                                    String.format("%s updated: %s via %s (%s -> %d)", u, dest, newNext,
                                            (oldCost >= INF ? "INF" : oldCost), newCost)));
                        }
                    }
                }
            }

            logs.add("Iteration " + iter + " ends. " + (anyChange ? "Changes occurred." : "No changes."));
            events.add(ProtocolEvent.iterationEnd(iter));

            if (!anyChange) {
                convergedIterations = iter;
                logs.add("Converged after " + iter + " iterations.");
                events.add(ProtocolEvent.converged(iter));
                return true;
            }
        }

        logs.add("Reached max iterations (" + maxIter + ") without full convergence.");
        events.add(ProtocolEvent.info("Reached max iterations (" + maxIter + ") without full convergence."));
        return false;
    }

    /** Return logs (copy). */
    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    /** Return events (copy). */
    public List<ProtocolEvent> getEvents() {
        return new ArrayList<>(events);
    }

    /** Return routing table for a node (copy). */
    public Map<String, RoutingTableEntry> getRoutingTable(String nodeId) {
        Map<String, RoutingTableEntry> t = tables.get(nodeId);
        if (t == null) return Collections.emptyMap();
        Map<String, RoutingTableEntry> copy = new LinkedHashMap<>();
        for (Map.Entry<String, RoutingTableEntry> e : t.entrySet()) {
            RoutingTableEntry r = e.getValue();
            copy.put(e.getKey(), new RoutingTableEntry(r.getDestination(), r.getNextHop(), r.getCost()));
        }
        return copy;
    }

    /** Return iteration at which convergence happened or -1. */
    public int getConvergedIterations() {
        return convergedIterations;
    }

    // ---------- helpers ----------

    /** Build adjacency map node -> (neighbor -> cost) */
    private Map<String, Map<String, Integer>> buildAdjacency() {
        Map<String, Map<String, Integer>> adj = new HashMap<>();
        for (Edge e : graph.getEdges()) {
            adj.computeIfAbsent(e.getA(), k -> new HashMap<>()).put(e.getB(), e.getCost());
            adj.computeIfAbsent(e.getB(), k -> new HashMap<>()).put(e.getA(), e.getCost());
        }
        for (Node n : graph.getNodes()) adj.computeIfAbsent(n.getId(), k -> new HashMap<>());
        return adj;
    }

    /** Find the edge id connecting a and b (both directions). Returns null if not found. */
    private String findEdgeId(String a, String b) {
        for (Edge e : graph.getEdges()) {
            if ((e.getA().equals(a) && e.getB().equals(b)) || (e.getA().equals(b) && e.getB().equals(a))) {
                return e.getId();
            }
        }
        return null;
    }
}
