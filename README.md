# Routing Protocol Simulator (Java Swing GUI)

A fully interactive Computer Networks routing simulator built in Java Swing.
Design network topologies, simulate routing protocols (Distance Vector & Link State), and visualize routing tables and protocol events live on the canvas.
___
**Features**

* Interactive GUI (drag routers, draw links)

* Draggable per-router routing-table windows

* Two routing algorithms:

   * Distance Vector (DV) — iterative table exchange + convergence visualization

   * Link State (LS) — Dijkstra per router, LSA & relaxations animation

* Event playback: Prepare → Play → Pause → Step → Stop

* Save / Load topology files

* Clean, educational visualization (flashing links/nodes, table updates)


# Clone repository (one-time)

Open your VS Code terminal (or any terminal) and run:

**clone**

* git clone https://github.com/AdityaGG11/Routing-Protocol-Simulator.git

**change into the project folder**

* cd "Routing-Protocol-Simulator"
___

# Quick setup: compile & run (Windows / VSCode)

These commands assume you are inside the repository root (the folder that contains src/).

**create output directory (if not present)**

* mkdir -p out

**compile all Java sources to the out/ directory**

* javac -d out src/simulator/*.java

**run the GUI**

* java -cp out simulator.Main
___

# Full project structure (root-level)

<img width="647" height="500" alt="image" src="https://github.com/user-attachments/assets/196b127b-9061-4768-9c4f-00afd48e30ae" />

___

# What each phase does:-

* Graph, Node, Edge — network model (store nodes/links, costs, positions).

* RoutingTableEntry — single routing table row (destination, next hop, cost).

* DistanceVectorEngine — implements DV: iterative neighbor vector exchanges, emits ProtocolEvents and final routing tables.

* LinkStateEngine — implements LS: Dijkstra per router, emits events for LSAs & relaxations and provides final tables.

* ProtocolEvent — structured event representing algorithm steps (MESSAGE_SEND, TABLE_UPDATE, ITERATION_START, CONVERGED, INFO).

* EventPlayer — plays a list of ProtocolEvent objects (play/pause/step/stop, speed control).

* CanvasPanel — main drawing surface: routers, links, routing tables, and animations. It applies events from EventPlayer.

* ControlPanel — left-side UI: add/delete nodes and links, mode toggles.

* RightPanel — right-side UI: algorithm selector, Prepare Events, Play/Pause/Step/Stop, speed slider, steps/logs area.

* TopologyIO — save/load topology files (optional helper).

* MainTestDV/MainTestLS — small test mains to run engines and print logs for debugging.
___
# Simulator Guide (step by step)

**Start the app**

* java -cp out simulator.Main


**Add routers**

* Click Add Router on the left, then click anywhere on the canvas.

**Add links**

* Click Add Link, click Router A → Router B, enter link cost (or leave default).

**Delete router/link**

* Click Delete Router or Delete Link, then click the target router/edge to remove.

**Show/Hide routing tables**

* Use the Show/Hide Routing Tables button on the right panel.

**Prepare Events**

* On the left panel choose Distance Vector or Link State, then click Prepare Events on the right panel (runs engine and populates events & final tables).

**Play**

* Click Play to animate events. Use Pause, Step, and change Speed with the slider.

**Save/Load topology**

* Use Save Topology / Load Topology (if implemented) to persist or reuse your topology.

___
