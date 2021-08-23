package com.collarmc.client.api;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.client.Collar;
import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class AbstractApi<T extends ApiListener> {

    private static final Logger LOGGER = LogManager.getLogger(AbstractApi.class.getName());

    private final Set<T> listeners = new HashSet<>();
    protected final Collar collar;
    private final Supplier<ClientIdentityStore> identityStoreSupplier;
    protected final Consumer<ProtocolRequest> sender;

    public AbstractApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender) {
        this.collar = collar;
        this.identityStoreSupplier = identityStoreSupplier;
        this.sender = sender;
    }

    protected ClientIdentity identity() {
        ClientIdentityStore clientIdentityStore = identityStoreSupplier.get();
        if (clientIdentityStore == null) {
            throw new IllegalStateException("Client is not ready");
        }
        return identityStoreSupplier.get().identity();
    }

    protected ClientIdentityStore identityStore() {
        return identityStoreSupplier.get();
    }

    public void subscribe(T listener) {
        listeners.add(listener);
    }

    public void unsubscribe(T listener) {
        listeners.remove(listener);
    }

    protected void fireListener(String name, Consumer<T> listener) {
        LOGGER.info("Firing " + name + " listeners");
        listeners.forEach(t -> {
            try {
                listener.accept(t);
            } catch (Throwable e) {
                LOGGER.info("Listener threw exception", e);
            }
        });
    }

    /**
     * Listen to state changes from the client
     * @param state state
     */
    public abstract void onStateChanged(Collar.State state);

    /**
     * Handle a protocol response
     * @param resp to handle
     * @return if the response was handled by this module or not
     */
    public abstract boolean handleResponse(ProtocolResponse resp);
}
