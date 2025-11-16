package simulator;

import java.util.*;

/**
 * Link State Engine producing structured ProtocolEvent objects.
 *
 * Behavior:
 *  - For each node S in graph: run Dijkstra from S to compute shortest paths to all nodes.
 *  - Emit ITERATION_START for each source S (iteration = source index starting at 1).
 *  - Emit a MESSAGE_SEND event for the source's "LSA flood" (visual hint).
 *  - During Dijkstra, when a node's best distance is finalized (popped from PQ),
 *    emit TABLE_UPDATE events for that destination (with next-hop and cost).
 *  - After processing all sources, emit CONVERGED (iteration=1).
 *
 * Public API:
 *  - boolean runWithLogging()        // runs and fills logs/events
 *  - List<String> getLogs()
 *  - List<ProtocolEvent> getEvents()
 *  - Map<String, RoutingTableEntry> getRoutingTable(String nodeId)
 *  - int getConvergedIterations()
 */
public class LinkStateEngine {
    private final Graph graph;
    private final List<String> logs = new ArrayList<>();
    private final List<ProtocolEvent> events = new ArrayList<>();
    private final Map<String, Map<String, RoutingTableEntry>> tables = new LinkedHashMap<>();
    private int convergedIterations = -1;
    private static final int INF = 1_000_000_000;

    public LinkStateEngine(Graph graph) {
        this.graph = graph;
    }

    /**
     * Run Link-State: for each node as source, run Dijkstra and emit events.
     * Returns true if run successfully (always true here).
     */
    public boolean runWithLogging() {
        logs.clear();
        events.clear();
        tables.clear();
        convergedIterations = -1;

        // Prepare adjacency map
        Map<String, Map<String, Integer>> adj = new HashMap<>();
        for (Edge e : graph.getEdges()) {
            adj.computeIfAbsent(e.getA(), k -> new HashMap<>()).put(e.getB(), e.getCost());
            adj.computeIfAbsent(e.getB(), k -> new HashMap<>()).put(e.getA(), e.getCost());
        }
        for (Node n : graph.getNodes()) adj.computeIfAbsent(n.getId(), k -> new HashMap<>());

        List<String> nodes = new ArrayList<>();
        for (Node n : graph.getNodes()) nodes.add(n.getId());

        logs.add("Link State: starting Dijkstra from every node.");
        events.add(ProtocolEvent.info("Link State: starting Dijkstra from every node."));

        int iter = 0;
        for (String src : nodes) {
            iter++;
            logs.add("Dijkstra source: " + src);
            events.add(ProtocolEvent.iterationStart(iter));

            // For visualization: simulate an LSA "broadcast" from src (single event)
            String randomEdge = findAnyEdgeFrom(src);
            events.add(ProtocolEvent.messageSend(src, null, randomEdge, iter,
                    "LSA flood from " + src));

            // Dijkstra structures
            Map<String, Integer> dist = new HashMap<>();
            Map<String, String> prev = new HashMap<>();
            for (String v : nodes) {
                dist.put(v, INF);
                prev.put(v, null);
            }
            dist.put(src, 0);

            // min-heap of (distance, node)
            PriorityQueue<NodeDist> pq = new PriorityQueue<>();
            pq.add(new NodeDist(src, 0));

            Set<String> visited = new HashSet<>();

            while (!pq.isEmpty()) {
                NodeDist nd = pq.poll();
                String u = nd.node;
                int d = nd.dist;
                if (visited.contains(u)) continue;
                visited.add(u);

                // When node u is finalized, emit TABLE_UPDATE for this destination relative to src:
                // nextHop is the first hop from src to u (reconstruct path)
                if (!u.equals(src)) {
                    String nextHop = computeNextHop(prev, src, u);
                    int cost = d >= INF ? INF : d;
                    // update the tables accumulation for source src
                    tables.computeIfAbsent(src, k -> initEmptyTable(nodes, src));
                    Map<String, RoutingTableEntry> tab = tables.get(src);
                    tab.put(u, new RoutingTableEntry(u, nextHop, cost));

                    String edgeId = findEdgeIdBetweenPrevAnd(u, prev);
                    events.add(ProtocolEvent.tableUpdate(src, u, nextHop,
                            null, cost, edgeId, iter,
                            String.format("%s -> %s: nextHop=%s cost=%d", src, u, nextHop, cost)));
                    logs.add(String.format("Dijkstra[%s]: finalized %s (cost=%d) nextHop=%s", src, u, cost, nextHop));
                } else {
                    // ensure table entry for self
                    tables.computeIfAbsent(src, k -> initEmptyTable(nodes, src));
                    Map<String, RoutingTableEntry> tab = tables.get(src);
                    tab.put(src, new RoutingTableEntry(src, src, 0));
                }

                // relax neighbors
                Map<String, Integer> nbrs = adj.getOrDefault(u, Collections.emptyMap());
                for (Map.Entry<String, Integer> en : nbrs.entrySet()) {
                    String v = en.getKey();
                    int w = en.getValue();
                    if (visited.contains(v)) continue;
                    int cand = (d >= INF) ? INF : d + w;
                    if (cand < dist.get(v)) {
                        dist.put(v, cand);
                        prev.put(v, u);
                        pq.add(new NodeDist(v, cand));
                    }
                }
            }

            events.add(ProtocolEvent.iterationEnd(iter));
        }

        // After all sources processed, mark convergence (single round)
        convergedIterations = 1;
        logs.add("Link State: completed Dijkstra for all nodes.");
        events.add(ProtocolEvent.converged(convergedIterations));
        return true;
    }

    /** Return logs copy. */
    public List<String> getLogs() {
        return new ArrayList<>(logs);
    }

    /** Return events copy. */
    public List<ProtocolEvent> getEvents() {
        return new ArrayList<>(events);
    }

    /** Return routing table for a node (the table produced when that node was used as source). */
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

    public int getConvergedIterations() {
        return convergedIterations;
    }

    // ---------- helpers ----------

    private Map<String, RoutingTableEntry> initEmptyTable(List<String> nodes, String self) {
        Map<String, RoutingTableEntry> t = new LinkedHashMap<>();
        for (String v : nodes) {
            if (v.equals(self)) t.put(v, new RoutingTableEntry(v, self, 0));
            else t.put(v, new RoutingTableEntry(v, null, INF));
        }
        return t;
    }

    /** Find any edge id that originates or touches node (used for LSA message visualization). */
    private String findAnyEdgeFrom(String node) {
        for (Edge e : graph.getEdges()) {
            if (e.getA().equals(node) || e.getB().equals(node)) return e.getId();
        }
        return null;
    }

    /** Find edge id used when finalizing node u using prev map: edge between u and prev[u] (if exists). */
    private String findEdgeIdBetweenPrevAnd(String u, Map<String, String> prev) {
        String p = prev.get(u);
        if (p == null) return null;
        return findEdgeId(u, p);
    }

    /** Find edge id connecting a and b (both directions). Returns null if not found. */
    private String findEdgeId(String a, String b) {
        for (Edge e : graph.getEdges()) {
            if ((e.getA().equals(a) && e.getB().equals(b)) || (e.getA().equals(b) && e.getB().equals(a))) {
                return e.getId();
            }
        }
        return null;
    }

    /** Compute the next hop from source -> dest, using prev map built by Dijkstra.
     *  It walks backward from dest to source via prev[] and returns the immediate neighbor of source.
     */
    private String computeNextHop(Map<String, String> prev, String source, String dest) {
        if (source.equals(dest)) return source;
        // reconstruct path backwards
        LinkedList<String> path = new LinkedList<>();
        String cur = dest;
        while (cur != null) {
            path.addFirst(cur);
            if (cur.equals(source)) break;
            cur = prev.get(cur);
        }
        if (path.isEmpty()) return null;
        // path[0] == source, path[1] is next hop
        if (path.size() >= 2) return path.get(1);
        return null;
    }

    // small helper to represent PQ entries
    private static class NodeDist implements Comparable<NodeDist> {
        final String node;
        final int dist;
        NodeDist(String node, int dist) { this.node = node; this.dist = dist; }
        @Override
        public int compareTo(NodeDist o) { return Integer.compare(this.dist, o.dist); }
    }
}
