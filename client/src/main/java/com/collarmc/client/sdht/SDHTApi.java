package com.collarmc.client.sdht;

import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.client.Collar;
import com.collarmc.client.api.AbstractApi;
import com.collarmc.client.minecraft.Ticks;
import com.collarmc.protocol.ProtocolRequest;
import com.collarmc.protocol.ProtocolResponse;
import com.collarmc.protocol.sdht.SDHTEventRequest;
import com.collarmc.protocol.sdht.SDHTEventResponse;
import com.collarmc.sdht.Content;
import com.collarmc.sdht.DistributedHashTable;
import com.collarmc.sdht.DistributedHashTableListener;
import com.collarmc.sdht.Key;
import com.collarmc.sdht.cipher.ContentCipher;
import com.collarmc.sdht.events.Publisher;
import com.collarmc.sdht.impl.DHTNamespaceState;
import com.collarmc.sdht.impl.DefaultDistributedHashTable;

import java.io.File;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Private API used for distributing data to nodes on the collar network
 * If you want access to this API, please file an issue in Github, do NOT use reflection to put shit in here
 * We are happy to make a nice future proof API for you to use it if you've got a good use case
 */
public class SDHTApi extends AbstractApi<SDHTListener> implements Ticks.TickListener {

    public final DistributedHashTable table;

    public SDHTApi(Collar collar, Supplier<ClientIdentityStore> identityStoreSupplier, Consumer<ProtocolRequest> sender, ContentCipher cipher, Ticks ticks, File dhtDir) {
        super(collar, identityStoreSupplier, sender);
        Publisher publisher = event -> {
            sender.accept(new SDHTEventRequest(identity(), event));
        };
        table = new DefaultDistributedHashTable(
                publisher,
                () -> identityStoreSupplier.get().identity(),
                cipher,
                new DHTNamespaceState(dhtDir),
                new DistributedHashTableListenerImpl(this)
        );
        ticks.subscribe(this);
    }

    @Override
    public void onStateChanged(Collar.State state) {
        if (state == Collar.State.DISCONNECTED) {
            table.removeAll();
        }
    }

    @Override
    public boolean handleResponse(ProtocolResponse resp) {
        if (resp instanceof SDHTEventResponse) {
            SDHTEventResponse response = (SDHTEventResponse) resp;
            if (!response.event.sender.equals(identity())) {
                table.process(response.event);
            }
        }
        return false;
    }

    @Override
    public void onTick() {
        table.processPendingRecords();
    }

    /**
     * Responsible for proxying events to the listener API
     */
    class DistributedHashTableListenerImpl implements DistributedHashTableListener {

        private final SDHTApi sdhtApi;

        public DistributedHashTableListenerImpl(SDHTApi sdhtApi) {
            this.sdhtApi = sdhtApi;
        }

        @Override
        public void onAdd(Key key, Content content) {
            fireListener("onRecordAdded", listener -> listener.onRecordAdded(collar, sdhtApi, key, content));
        }

        @Override
        public void onRemove(Key key, Content content) {
            fireListener("onRecordRemoved", listener -> listener.onRecordRemoved(collar, sdhtApi, key, content));
        }
    }
}
