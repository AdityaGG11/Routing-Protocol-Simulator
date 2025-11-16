package simulator;

/**
 * Immutable representation of a protocol event emitted by routing engines.
 *
 * Events are lightweight and intended for UI playback/animation.
 */
public class ProtocolEvent {
    public enum Type {
        MESSAGE_SEND,   // a routing message sent from source -> target (visualize message on edge)
        TABLE_UPDATE,   // a router updated its routing table entry for a destination
        ITERATION_START,
        ITERATION_END,
        CONVERGED,
        INFO            // general informational line
    }

    private final Type type;
    private final String source;     // node id sending (if any)
    private final String target;     // node id receiving / node whose table changed (if any)
    private final String dest;       // destination affected (for TABLE_UPDATE)
    private final String via;        // next-hop (for TABLE_UPDATE)
    private final Integer oldCost;   // nullable
    private final Integer newCost;   // nullable
    private final String edgeId;     // optional edge id to flash/animate
    private final int iteration;     // iteration number (0 if not applicable)
    private final String message;    // human-readable fallback or info text

    // Full constructor (use builder-like style when creating events)
    public ProtocolEvent(Type type,
                         String source,
                         String target,
                         String dest,
                         String via,
                         Integer oldCost,
                         Integer newCost,
                         String edgeId,
                         int iteration,
                         String message) {
        this.type = type;
        this.source = source;
        this.target = target;
        this.dest = dest;
        this.via = via;
        this.oldCost = oldCost;
        this.newCost = newCost;
        this.edgeId = edgeId;
        this.iteration = iteration;
        this.message = message;
    }

    // Convenience constructors for common events
    public static ProtocolEvent iterationStart(int iter) {
        return new ProtocolEvent(Type.ITERATION_START, null, null, null, null, null, null, null, iter,
                "Iteration " + iter + " start");
    }

    public static ProtocolEvent iterationEnd(int iter) {
        return new ProtocolEvent(Type.ITERATION_END, null, null, null, null, null, null, null, iter,
                "Iteration " + iter + " end");
    }

    public static ProtocolEvent converged(int iter) {
        return new ProtocolEvent(Type.CONVERGED, null, null, null, null, null, null, null, iter,
                "Converged at iteration " + iter);
    }

    public static ProtocolEvent info(String text) {
        return new ProtocolEvent(Type.INFO, null, null, null, null, null, null, null, 0, text);
    }

    public static ProtocolEvent messageSend(String src, String tgt, String edgeId, int iter, String text) {
        return new ProtocolEvent(Type.MESSAGE_SEND, src, tgt, null, null, null, null, edgeId, iter, text);
    }

    public static ProtocolEvent tableUpdate(String target, String destination, String nextHop,
                                            Integer oldCost, Integer newCost, String edgeId, int iter, String text) {
        return new ProtocolEvent(Type.TABLE_UPDATE, null, target, destination, nextHop, oldCost, newCost, edgeId, iter, text);
    }

    // Getters
    public Type getType() { return type; }
    public String getSource() { return source; }
    public String getTarget() { return target; }
    public String getDest() { return dest; }
    public String getVia() { return via; }
    public Integer getOldCost() { return oldCost; }
    public Integer getNewCost() { return newCost; }
    public String getEdgeId() { return edgeId; }
    public int getIteration() { return iteration; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        switch (type) {
            case MESSAGE_SEND:
                return String.format("[MSG] %s -> %s (edge=%s) iter=%d %s", source, target, edgeId, iteration,
                        message == null ? "" : message);
            case TABLE_UPDATE:
                return String.format("[UPDATE] %s: dest=%s via=%s old=%s new=%s iter=%d %s",
                        target, dest, via, oldCost == null ? "?" : oldCost, newCost == null ? "?" : newCost, iteration,
                        message == null ? "" : message);
            case ITERATION_START:
            case ITERATION_END:
            case CONVERGED:
            case INFO:
            default:
                return String.format("[%s] %s", type.name(), message == null ? "" : message);
        }
    }
}
