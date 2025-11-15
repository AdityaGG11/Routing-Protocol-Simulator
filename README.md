🚀 Routing Protocol Simulator (Java Swing GUI)

A fully interactive Computer Networks routing simulator built using Java Swing, allowing students to design network topologies, simulate routing protocols, and visualize routing tables live on the canvas.

✨ Features
🖼️ Interactive GUI

Draw routers and links directly on the canvas

Drag routers freely

Draggable routing-table windows

Clean router icon design

Auto-align & manual repositioning of routing tables

📡 Routing Algorithms
1. Distance Vector Routing

Simulated iterative table exchange

Node & link flashing animation for updates

Convergence visualization

2. Link State Routing (Dijkstra)

Per-router shortest path tree

Instant routing table generation

Supports weighted edges

🔧 Topology Editing

Add Router

Add Link (custom/default cost)

Delete Router (click on router)

Delete Link (click on link)

Move routers around

Toggle routing tables on/off (Right Panel)

💾 Save / Load

Save topology to file

Load topology anytime

🛠️ How to Run the Simulator
Compile
javac -d out src/simulator/*.java

Run
java -cp out simulator.Main

🗂️ Project Structure

<img width="555" height="528" alt="image" src="https://github.com/user-attachments/assets/93d12704-409d-4f04-a966-0155b0e55461" />


🎮 How to Use the Simulator
➕ Add Router

Click Add Router → click on canvas

🔗 Add Link

Click Add Link → click router A → router B
Enter link cost (or use default)

❌ Delete Router

Click Delete Router → click any router

❌ Delete Link

Click Delete Link → click any edge

📊 Show/Hide Routing Tables

Use the Show/Hide Routing Tables button on the right panel

▶️ Run Algorithm

Select:

Distance Vector

Link State

Then press Run Algorithm

The routing tables appear next to routers.

🖱️ Drag Routers

Click & drag any router
Tables follow (or stay in manual positions)

💾 Save / Load

Use Save Topology and Load Topology button
