package team.catgirl.collar.client.minecraft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Allows Collar internals to subscribe to Minecraft client ticks
 */
public final class Ticks {

    private static final Logger LOGGER = LogManager.getLogger(Ticks.class.getName());

    private final CopyOnWriteArrayList<TickListener> listeners = new CopyOnWriteArrayList<>();

    public Ticks() {}

    /**
     * @param listener to subscribe
     */
    public void subscribe(TickListener listener) {
        listeners.add(listener);
    }

    /**
     * @param listener to unsubscribe
     */
    public void unsubscribe(TickListener listener) {
        listeners.remove(listener);
    }

    /**
     * Called by the Minecraft client on a tick event
     */
    public void onTick() {
        listeners.forEach(onTick -> {
            try {
                onTick.onTick();
            } catch (Throwable e) {
                LOGGER.info("Tick listener failed", e);
            }
        });
    }

    /**
     * @param listener to test
     * @return subscribed
     */
    public boolean isSubscribed(TickListener listener) {
        return listeners.contains(listener);
    }

    public interface TickListener {
        /**
         * Fired on Minecraft client tick
         */
        void onTick();
    }
}
