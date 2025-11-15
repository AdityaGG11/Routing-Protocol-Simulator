package simulator;

import java.io.*;
import java.util.*;

/**
 * Minimal JSON save/load for topologies.
 * This intentionally avoids external JSON libraries to keep things simple.
 *
 * Save to: src/simulator/TopologyIO.java
 */
public class TopologyIO {

    /**
     * Save the given graph to a file in a very simple JSON format.
     */
    public static void save(Graph g, File outFile) throws IOException {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"nodes\": [\n");
        boolean first = true;
        for (Node n : g.getNodes()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("    {\"id\":\"").append(escape(n.getId()))
              .append("\",\"x\":").append(n.getX())
              .append(",\"y\":").append(n.getY()).append("}");
        }
        sb.append("\n  ],\n  \"edges\": [\n");
        first = true;
        for (Edge e : g.getEdges()) {
            if (!first) sb.append(",\n");
            first = false;
            sb.append("    {\"id\":\"").append(escape(e.getId()))
              .append("\",\"from\":\"").append(escape(e.getA()))
              .append("\",\"to\":\"").append(escape(e.getB()))
              .append("\",\"cost\":").append(e.getCost())
              .append(",\"isUp\":").append(e.isUp()).append("}");
        }
        sb.append("\n  ]\n}\n");

        try (FileWriter fw = new FileWriter(outFile)) {
            fw.write(sb.toString());
        }
    }

    /**
     * Load a graph from a file produced by save().
     * Parsing is permissive but expects the simple format produced by save().
     */
    public static Graph load(File inFile) throws IOException {
        Graph g = new Graph();
        String content;
        try (BufferedReader br = new BufferedReader(new FileReader(inFile))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) { sb.append(line).append("\n"); }
            content = sb.toString();
        }

        // Extract nodes block
        int nodesIdx = content.indexOf("\"nodes\":");
        if (nodesIdx >= 0) {
            int start = content.indexOf("[", nodesIdx);
            int end = content.indexOf("]", start);
            if (start >= 0 && end >= 0) {
                String nodesBlock = content.substring(start + 1, end).trim();
                if (!nodesBlock.isEmpty()) {
                    String[] entries = splitTopLevelObjects(nodesBlock);
                    for (String entry : entries) {
                        Map<String, String> kv = parseSimpleObject(entry);
                        String id = stripQuotes(kv.get("id"));
                        int x = parseIntOrDefault(kv.get("x"), 0);
                        int y = parseIntOrDefault(kv.get("y"), 0);
                        if (id != null) g.addNode(id, x, y);
                    }
                }
            }
        }

        // Extract edges block
        int edgesIdx = content.indexOf("\"edges\":");
        if (edgesIdx >= 0) {
            int start = content.indexOf("[", edgesIdx);
            int end = content.indexOf("]", start);
            if (start >= 0 && end >= 0) {
                String edgesBlock = content.substring(start + 1, end).trim();
                if (!edgesBlock.isEmpty()) {
                    String[] entries = splitTopLevelObjects(edgesBlock);
                    for (String entry : entries) {
                        Map<String, String> kv = parseSimpleObject(entry);
                        String id = stripQuotes(kv.get("id"));
                        String from = stripQuotes(kv.get("from"));
                        String to = stripQuotes(kv.get("to"));
                        int cost = parseIntOrDefault(kv.get("cost"), 1);
                        boolean isUp = parseBooleanOrDefault(kv.get("isUp"), true);
                        if (id != null && from != null && to != null) {
                            // ensure nodes exist
                            if (g.getNode(from) == null) g.addNode(from);
                            if (g.getNode(to) == null) g.addNode(to);
                            // create an edge id that doesn't clash if it already exists
                            String edgeId = id;
                            int suffix = 1;
                            while (g.getEdge(edgeId) != null) {
                                edgeId = id + "_" + suffix++;
                            }
                            g.addEdge(edgeId, from, to, cost);
                            Edge e = g.findEdgeBetween(from, to);
                            if (e != null) e.setUp(isUp);
                        }
                    }
                }
            }
        }

        return g;
    }

    // --- Helper parsing utilities (very small/simple) ---

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static String stripQuotes(String s) {
        if (s == null) return null;
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"") && s.length() >= 2) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static int parseIntOrDefault(String s, int def) {
        if (s == null) return def;
        try { return Integer.parseInt(s.trim().replaceAll("\"", "")); } catch (Exception ex) { return def; }
    }

    private static boolean parseBooleanOrDefault(String s, boolean def) {
        if (s == null) return def;
        try { return Boolean.parseBoolean(s.trim().replaceAll("\"", "")); } catch (Exception ex) { return def; }
    }

    /**
     * Split a JSON-like array block of objects into top-level object strings.
     * Works for the simple format emitted by save().
     */
    private static String[] splitTopLevelObjects(String block) {
        List<String> out = new ArrayList<>();
        int brace = 0;
        int start = -1;
        for (int i = 0; i < block.length(); i++) {
            char c = block.charAt(i);
            if (c == '{') {
                if (brace == 0) start = i;
                brace++;
            } else if (c == '}') {
                brace--;
                if (brace == 0 && start >= 0) {
                    out.add(block.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return out.toArray(new String[0]);
    }

    /**
     * Parse a simple object like: {"id":"A","x":100,"y":200}
     * into a map key->value (values include quotes if present).
     */
    private static Map<String, String> parseSimpleObject(String obj) {
        Map<String, String> kv = new LinkedHashMap<>();
        String inner = obj.trim();
        if (inner.startsWith("{")) inner = inner.substring(1);
        if (inner.endsWith("}")) inner = inner.substring(0, inner.length() - 1);
        // split by commas, but naive split is acceptable for this simple format
        String[] parts = inner.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
        for (String part : parts) {
            String[] pair = part.split(":", 2);
            if (pair.length == 2) {
                String key = pair[0].trim().replaceAll("^\"|\"$", "");
                String val = pair[1].trim();
                kv.put(key, val);
            }
        }
        return kv;
    }
}
