package simulator;

/**
 * Simple console test for LinkStateEngine.
 * Builds the same sample topology and runs LS.
 *
 * Save to: src/simulator/MainTestLS.java
 */
public class MainTestLS {
    public static void main(String[] args) {
        Graph g = new Graph();

        // Create nodes
        g.addNode("A");
        g.addNode("B");
        g.addNode("C");
        g.addNode("D");

        // Create edges (id, from, to, cost)
        g.addEdge("e1", "A", "B", 1);
        g.addEdge("e2", "B", "C", 3);
        g.addEdge("e3", "A", "C", 7);
        g.addEdge("e4", "C", "D", 2);
        g.addEdge("e5", "B", "D", 10);

        System.out.println("Graph: " + g);

        LinkStateEngine ls = new LinkStateEngine(g);
        System.out.println("\nLink-State routing tables:");
        ls.printAllTables();
    }
}

