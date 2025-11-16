package simulator;

import javax.swing.*;
import java.util.List;

/**
 * EventPlayer: plays a sequence of ProtocolEvent objects at a configurable speed.
 *
 * Usage:
 *   EventPlayer p = new EventPlayer(events, listener);
 *   p.setDelayMs(400); // optional
 *   p.play();
 *   p.pause();
 *   p.stepForward(); // advance one event while paused
 *
 * The listener.onEvent(...) is called on the EDT (Swing thread) for safe UI updates.
 */
public class EventPlayer {
    public interface Listener {
        /**
         * Called whenever an event should be applied/displayed.
         * index is 0-based index of event in the list; total is list size.
         */
        void onEvent(ProtocolEvent event, int index, int total);

        /** Called when playback finishes (reached end). */
        void onFinished();
    }

    private final List<ProtocolEvent> events;
    private final Listener listener;
    private int index = 0;            // next event index to play
    private int delayMs = 400;        // ms per event by default
    private javax.swing.Timer timer;  // swing timer running playback

    public EventPlayer(List<ProtocolEvent> events, Listener listener) {
        this.events = events == null ? java.util.Collections.emptyList() : events;
        this.listener = listener;
        initTimer();
    }

    private void initTimer() {
        if (timer != null) {
            timer.stop();
        }
        timer = new javax.swing.Timer(delayMs, e -> {
            stepInternal();
        });
        timer.setRepeats(true);
    }

    /** Set delay (ms) between events. If currently playing, timer is restarted with new delay. */
    public void setDelayMs(int ms) {
        this.delayMs = Math.max(10, ms);
        if (timer != null) {
            boolean wasRunning = timer.isRunning();
            timer.stop();
            timer.setDelay(this.delayMs);
            timer.setInitialDelay(0);
            if (wasRunning) timer.start();
        }
    }

    public int getDelayMs() { return delayMs; }

    /** Start/resume playback from current index. */
    public void play() {
        if (events.isEmpty()) {
            // nothing to play — notify finished immediately on EDT
            SwingUtilities.invokeLater(() -> listener.onFinished());
            return;
        }
        if (timer == null) initTimer();
        timer.setDelay(delayMs);
        timer.start();
    }

    /** Pause playback. */
    public void pause() {
        if (timer != null) timer.stop();
    }

    /** Returns whether currently playing. */
    public boolean isPlaying() {
        return timer != null && timer.isRunning();
    }

    /** Step forward by one event (useful while paused). */
    public void stepForward() {
        // ensure timer not running
        if (isPlaying()) pause();
        stepInternal();
    }

    /** Reset playback to the beginning (index 0). Pauses playback. */
    public void reset() {
        pause();
        index = 0;
    }

    /** Return current index (0-based) */
    public int getIndex() {
        return index;
    }

    /** Internal: apply one event and advance index. Runs listener on EDT. */
    private void stepInternal() {
        if (index >= events.size()) {
            // finished
            if (timer != null) timer.stop();
            SwingUtilities.invokeLater(() -> listener.onFinished());
            return;
        }
        final ProtocolEvent ev = events.get(index);
        final int curIdx = index;
        final int total = events.size();
        // call listener on EDT
        SwingUtilities.invokeLater(() -> {
            try {
                listener.onEvent(ev, curIdx, total);
            } catch (Exception ex) {
                // swallow listener errors to avoid timer stopping unexpectedly
                ex.printStackTrace();
            }
        });
        index++;
    }
}

