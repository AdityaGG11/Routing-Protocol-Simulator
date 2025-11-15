package simulator;

/**
 * Simple console test for DistanceVectorEngine.
 * Builds a small topology and runs DV until convergence.
 *
 * Save to: src/simulator/MainTestDV.java
 */
public class MainTestDV {
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
        // Optional: add another path
        g.addEdge("e5", "B", "D", 10);

        System.out.println("Graph: " + g);

        DistanceVectorEngine dv = new DistanceVectorEngine(g);
        System.out.println("\nInitial tables:");
        dv.printAllTables();

        int rounds = dv.runUntilConverged(20);
        System.out.println("Converged in " + rounds + " rounds.\n");

        System.out.println("Final tables:");
        dv.printAllTables();
    }
}

