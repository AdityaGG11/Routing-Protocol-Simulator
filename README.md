Routing Protocol Simulator (Java Swing GUI)

This project is a full interactive network routing simulator built in Java Swing, allowing users to:

Draw network topologies

Add/remove routers and links

Assign link costs

Simulate Distance Vector and Link State (Dijkstra) routing

Visualize per-node routing tables inline on the canvas

Animate packet/route propagation

Save & load topologies

Drag routers and routing-table windows

Delete routers/links interactively by clicking

It is designed as an educational tool for Computer Networks courses and fully meets the requirements given by the professor.

⭐ Features Overview
🖼️ Interactive GUI

Left panel: editing tools

Canvas: fully interactive graph drawing

Right panel: configuration and settings

Per-router routing tables appear next to each router (draggable)

🧩 Topology Editing

Add routers with auto-labeling (A, B, C, …)

Add links with custom or default weights

Delete routers/links by entering delete mode and clicking them

Drag routers freely across the canvas

Routing tables follow router movement automatically

Tables can also be manually repositioned

🔍 Routing Algorithms
1) Distance Vector

Iterative table exchanges

Convergence visualization

"packet exchange" animation via flashing edges

2) Link State

Per-router Dijkstra computation

Immediate shortest-path visualization

📦 Additional Features

Save topology to file

Load topology from file

Toggle routing tables on/off

Auto-maximized window on launch

Clean node icons & UI layout

🚀 How to Run the Simulator
1. Compile
javac -d out src/simulator/*.java

2. Run
java -cp out simulator.Main

📁 Project Structure
Routing Protocol Simulator/
│
├── src/simulator/
│   ├── Main.java
│   ├── CanvasPanel.java
│   ├── ControlPanel.java
│   ├── RightPanel.java
│   ├── Graph.java
│   ├── Node.java
│   ├── Edge.java
│   ├── RoutingTableEntry.java
│   ├── DistanceVectorEngine.java
│   ├── LinkStateEngine.java
│   └── TopologyIO.java
│
├── out/              (generated after compile)
└── README.md

🎓 Usage Instructions
Adding Routers

Click Add Router

Click anywhere on the canvas

Drag routers anytime

Adding Links

Click Add Link

Click router 1 → router 2

Enter cost or press OK to use default

Deleting Routers/Links

Click Delete Router → click any router

Click Delete Link → click on any edge

Press ESC to exit delete mode

Running Routing Algorithms

Select algorithm (Distance Vector / Link State)

Click Run Algorithm

Routing tables appear next to each router

Show / Hide Routing Tables

Use the toggle button on the right panel

Saving & Loading Graph

Click Save Topology

Click Load Topology