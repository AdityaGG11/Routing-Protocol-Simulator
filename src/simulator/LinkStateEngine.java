package simulator;

import java.util.*;

/**
 * Simple Link State engine.
 *
 * Behavior (simplified):
 * - Each node's LSA is simply its adjacency list with link costs.
 * - We build a global LSDB by collecting LSAs for all nodes (no sequence numbers or flooding simulation here).
 * - For each node, run Dijkstra on the graph represented by the LSDB to compute shortest paths and next-hops.
 *
 * Note: This is a simplified, local LS implementation intended for visualization and testing.
 */
public class LinkStateEngine {
    private final Graph graph;
    // LSDB: nodeId -> map(neighborId -> cost)
    private final Map<String, Map<String, Integer>> lsdb = new LinkedHashMap<>();
    // computed routing tables: nodeId -> (destination -> RoutingTableEntry)
    private final Map<String, Map<String, RoutingTableEntry>> tables = new LinkedHashMap<>();

    public LinkStateEngine(Graph graph) {
        this.graph = graph;
        buildLsdb();
        computeAllRoutingTables();
    }

    /** Build LSDB from current graph state. */
    public void buildLsdb() {
        lsdb.clear();
        for (Node n : graph.getNodes()) {
            Map<String, Integer> nbrs = new LinkedHashMap<>();
            for (String nb : n.getNeighborIds()) {
                Edge e = graph.getEdge(n.getEdgeIdForNeighbor(nb));
                if (e != null && e.isUp()) {
                    nbrs.put(nb, e.getCost());
                }
            }
            lsdb.put(n.getId(), nbrs);
        }
    }

    /** Compute Dijkstra for every node and populate tables map. */
    public void computeAllRoutingTables() {
        tables.clear();
        // prepare node list
        List<String> nodes = new ArrayList<>(lsdb.keySet());
        for (String src : nodes) {
            Map<String, RoutingTableEntry> rt = computeRoutingTableFor(src);
            tables.put(src, rt);
        }
    }

    /** Compute routing table for a single source using Dijkstra over lsdb. */
    private Map<String, RoutingTableEntry> computeRoutingTableFor(String src) {
        // Dijkstra: dist and prev
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        Set<String> visited = new HashSet<>();

        for (String n : lsdb.keySet()) {
            dist.put(n, RoutingTableEntry.INFINITY);
            prev.put(n, null);
        }
        dist.put(src, 0);

        // Priority queue (nodeId, dist)
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.add(src);

        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (visited.contains(u)) continue;
            visited.add(u);

            Map<String, Integer> neighbors = lsdb.get(u);
            if (neighbors == null) continue;
            for (Map.Entry<String, Integer> e : neighbors.entrySet()) {
                String v = e.getKey();
                int costUV = e.getValue();
                if (!lsdb.containsKey(v)) continue; // skip unknown
                int alt = (dist.get(u) >= RoutingTableEntry.INFINITY) ? RoutingTableEntry.INFINITY : dist.get(u) + costUV;
                if (alt < dist.get(v)) {
                    dist.put(v, alt);
                    prev.put(v, u);
                    pq.add(v);
                }
            }
        }

        // Build routing table entries with next-hop resolution
        Map<String, RoutingTableEntry> rt = new LinkedHashMap<>();
        for (String dst : lsdb.keySet()) {
            if (dst.equals(src)) {
                rt.put(dst, new RoutingTableEntry(dst, src, 0));
            } else if (dist.getOrDefault(dst, RoutingTableEntry.INFINITY) >= RoutingTableEntry.INFINITY) {
                rt.put(dst, new RoutingTableEntry(dst, null, RoutingTableEntry.INFINITY));
            } else {
                // find next hop by walking prev[] from dst back to src
                String cur = dst;
                String prevNode = prev.get(cur);
                String nextHop = null;
                // walk until prevNode is src, then cur is nextHop (if path exists)
                while (prevNode != null && !prevNode.equals(src)) {
                    cur = prevNode;
                    prevNode = prev.get(cur);
                }
                if (prevNode == null) {
                    // direct neighbor?
                    // if prev.get(dst) == src then nextHop = dst's immediate neighbor cur
                    if (prev.get(dst) != null && prev.get(dst).equals(src)) nextHop = dst;
                    else {
                        // fallback: find neighbor of src on path by reconstructing path list
                        // Simple robust approach: reconstruct full path and take second element
                        List<String> path = reconstructPath(prev, src, dst);
                        if (path.size() >= 2) nextHop = path.get(1);
                    }
                } else {
                    nextHop = cur;
                }
                int cost = dist.get(dst);
                rt.put(dst, new RoutingTableEntry(dst, nextHop, cost));
            }
        }

        return rt;
    }

    /** Reconstruct path from src to dst using prev map (may return empty list if no path). */
    private List<String> reconstructPath(Map<String, String> prev, String src, String dst) {
        LinkedList<String> path = new LinkedList<>();
        String cur = dst;
        while (cur != null) {
            path.addFirst(cur);
            if (cur.equals(src)) break;
            cur = prev.get(cur);
        }
        if (path.isEmpty() || !path.getFirst().equals(src)) return Collections.emptyList();
        return path;
    }

    /** Get a copy of a node's routing table. */
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

