package com.collarmc.client.security.signal;

import com.collarmc.client.security.ClientIdentityStore;
import com.collarmc.client.security.ProfileState;
import com.collarmc.protocol.groups.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.whispersystems.libsignal.*;
import org.whispersystems.libsignal.groups.GroupSessionBuilder;
import org.whispersystems.libsignal.groups.SenderKeyName;
import org.whispersystems.libsignal.protocol.SenderKeyDistributionMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SignalProtocolStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.util.KeyHelper;
import com.collarmc.api.groups.MembershipState;
import com.collarmc.client.HomeDirectory;
import com.collarmc.client.security.ClientCipher;
import com.collarmc.client.security.PrivateIdentity;
import com.collarmc.io.AtomicFile;
import com.collarmc.protocol.devices.DeviceRegisteredResponse;
import com.collarmc.protocol.identity.CreateTrustRequest;
import com.collarmc.protocol.signal.ResendPreKeysResponse;
import com.collarmc.protocol.signal.SendPreKeysRequest;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.Identity;
import com.collarmc.security.PublicKey;
import com.collarmc.security.ServerIdentity;
import com.collarmc.security.cipher.Cipher;
import com.collarmc.security.signal.PreKeys;
import com.collarmc.utils.Utils;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class SignalClientIdentityStore implements ClientIdentityStore {

    private static final Logger LOGGER = LogManager.getLogger(SignalClientIdentityStore.class.getName());

    private final UUID owner;
    private final Supplier<PrivateIdentity> privateIdentitySupplier;
    private final ClientSignalProtocolStore store;
    private final State state;
    private final File file;
    private final ReentrantReadWriteLock lock;

    public SignalClientIdentityStore(UUID owner,
                                     Supplier<PrivateIdentity> privateIdentitySupplier,
                                     ClientSignalProtocolStore store,
                                     State state,
                                     File file) {
        this.owner = owner;
        this.privateIdentitySupplier = privateIdentitySupplier;
        this.store = store;
        this.state = state;
        this.file = file;
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public byte[] privateIdentityToken() {
        return privateIdentitySupplier.get().token;
    }

    @Override
    public ClientIdentity currentIdentity() {
        IdentityKeyPair identityKeyPair = this.store.getIdentityKeyPair();
        return new ClientIdentity(owner, new PublicKey(identityKeyPair.getPublicKey().serialize()), getDeviceId());
    }

    @Override
    public boolean isTrustedIdentity(Identity identity) {
        return store.isTrustedIdentity(signalProtocolAddressFrom(identity), identityKeyFrom(identity), null);
    }

    @Override
    public void trustIdentity(Identity owner, byte[] preKeyBundle) {
        if (isTrustedIdentity(owner)) {
            throw new IllegalStateException(owner + " is already trusted");
        }
        PreKeyBundle bundle;
        try {
            bundle = PreKeys.preKeyBundleFromBytes(preKeyBundle);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        SignalProtocolAddress address = signalProtocolAddressFrom(owner);
        store.saveIdentity(address, bundle.getIdentityKey());
        SessionBuilder sessionBuilder = new SessionBuilder(store, address);
        try {
            sessionBuilder.process(bundle);
        } catch (InvalidKeyException | UntrustedIdentityException e) {
            throw new IllegalStateException("Problem trusting PreKeyBundle for " + owner, e);
        }
        LOGGER.info(currentIdentity() + " now trusts " + owner);
    }

    @Override
    public Cipher createCypher() {
        return new ClientCipher(privateIdentitySupplier.get(), store, store, currentIdentity());
    }

    @Override
    public void processDeviceRegisteredResponse(DeviceRegisteredResponse response) {
        ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();
        try {
            writeLock.lockInterruptibly();
            if (state.deviceId != null) {
                throw new IllegalStateException("deviceId has already been set");
            }
            state.deviceId = response.deviceId;
            writeState(file, state);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new IllegalStateException("could not save state", e);
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public int getDeviceId() {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            return state.deviceId;
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    @Override
    public SendPreKeysRequest createPreKeyRequest(ServerIdentity identity) {
        PreKeyBundle bundle = PreKeys.generate(new SignalProtocolAddress(identity.id().toString(), 1), store);
        try {
            return new SendPreKeysRequest(currentIdentity(), PreKeys.preKeyBundleToBytes(bundle));
        } catch (IOException e) {
            throw new IllegalStateException("could not generate PreKeyBundle");
        }
    }

    @Override
    public SendPreKeysRequest createPreKeyRequest(DeviceRegisteredResponse response) {
        int deviceId = getDeviceId();
        if (deviceId < 1) {
            throw new IllegalStateException("deviceId has not been negotiated");
        }
        PreKeyBundle bundle = PreKeys.generate(new SignalProtocolAddress(response.profile.id.toString(), deviceId), store);
        try {
            return new SendPreKeysRequest(currentIdentity(), PreKeys.preKeyBundleToBytes(bundle));
        } catch (IOException e) {
            throw new IllegalStateException("could not generate PreKeyBundle");
        }
    }

    @Override
    public SendPreKeysRequest createPreKeyRequest(ResendPreKeysResponse response) {
        int deviceId = getDeviceId();
        if (deviceId < 1) {
            throw new IllegalStateException("deviceId has not been negotiated");
        }
        PreKeyBundle bundle = PreKeys.generate(new SignalProtocolAddress(currentIdentity().id().toString(), deviceId), store);
        try {
            return new SendPreKeysRequest(currentIdentity(), PreKeys.preKeyBundleToBytes(bundle));
        } catch (IOException e) {
            throw new IllegalStateException("could not generate PreKeyBundle");
        }
    }

    @Override
    public JoinGroupRequest createJoinGroupRequest(ClientIdentity identity, UUID groupId) {
        GroupSessionBuilder builder = new GroupSessionBuilder(store);
        SenderKeyDistributionMessage message = builder.create(new SenderKeyName(groupId.toString(), signalProtocolAddressFrom(identity)));
        return new JoinGroupRequest(identity, groupId, MembershipState.ACCEPTED, message.serialize());
    }

    @Override
    public CreateTrustRequest createPreKeyRequest(ClientIdentity recipient, long id) {
        PreKeyBundle bundle = PreKeys.generate(new SignalProtocolAddress(currentIdentity().id().toString(), getDeviceId()), store);
        try {
            return new CreateTrustRequest(currentIdentity(), id, recipient, PreKeys.preKeyBundleToBytes(bundle));
        } catch (IOException e) {
            throw new IllegalStateException("could not generate PreKeyBundle");
        }
    }

    @Override
    public AcknowledgedGroupJoinedRequest processJoinGroupResponse(JoinGroupResponse resp) {
        GroupSessionBuilder builder = new GroupSessionBuilder(store);
        // Generate my sender key and send it back to the sender
        SenderKeyDistributionMessage message = builder.create(new SenderKeyName(resp.group.toString(), signalProtocolAddressFrom(currentIdentity())));
        LOGGER.info(currentIdentity() + " creating group session for " + currentIdentity() + " in group " + resp.group + " to send to " + resp.sender);
        // Process the joining clients message
        if (!resp.sender.equals(currentIdentity())) {
            try {
                SenderKeyDistributionMessage sendersMessage = new SenderKeyDistributionMessage(resp.keys);
                builder.process(new SenderKeyName(resp.group.toString(), signalProtocolAddressFrom(resp.sender)), sendersMessage);
            } catch (Throwable e) {
                throw new IllegalStateException(currentIdentity() + " could not process group keys from " + resp.sender, e);
            }
        }
        return new AcknowledgedGroupJoinedRequest(currentIdentity(), resp.sender, resp.group, message.serialize());
    }

    @Override
    public void processAcknowledgedGroupJoinedResponse(AcknowledgedGroupJoinedResponse response) {
        if (response.sender.equals(currentIdentity())) {
            // Do not process your own identity!
            // If you do, the private key will be overwritten and the client will not be able to crypt
            return;
        }
        GroupSessionBuilder builder = new GroupSessionBuilder(store);
        SenderKeyName name = new SenderKeyName(response.group.id.toString(), signalProtocolAddressFrom(response.sender));
        SenderKeyDistributionMessage senderKeyDistributionMessage;
        try {
            senderKeyDistributionMessage = new SenderKeyDistributionMessage(response.keys);
        } catch (LegacyMessageException | InvalidMessageException e) {
            throw new IllegalStateException("could not join group " + response.group.id, e);
        }
        builder.process(name, senderKeyDistributionMessage);
        LOGGER.info(currentIdentity() + " processed " + response.sender + "'s session with group " + response.group.id);
    }

    @Override
    public void processLeaveGroupResponse(LeaveGroupResponse response) {
        store.deleteGroupSession(response.groupId, response.sender == null ? currentIdentity() : response.sender);
    }

    @Override
    public void clearAllGroupSessions() {
        store.deleteAllGroupSessions();
    }

    public void delete() throws IOException {
        store.delete();
    }

    @Override
    public void reset() throws IOException {
        store.delete();
    }

    @Override
    public void save() throws IOException {
        writeState(file, state);
        store.writeState();
    }

    private SignalProtocolAddress signalProtocolAddressFrom(Identity identity) {
        ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
        try {
            readLock.lockInterruptibly();
            return new SignalProtocolAddress(identity.id().toString(), identity.deviceId());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            readLock.unlock();
        }
    }

    private static IdentityKey identityKeyFrom(Identity identity) {
        IdentityKey identityKey;
        try {
            identityKey = new IdentityKey(identity.publicKey().key, 0);
        } catch (InvalidKeyException e) {
            throw new IllegalStateException("bad key");
        }
        return identityKey;
    }

    private static final class State {
        @JsonProperty("identityKeyPair")
        public final byte[] identityKeyPair;
        @JsonProperty("registrationId")
        public final Integer registrationId;
        @JsonProperty("deviceId")
        public Integer deviceId;

        public State(@JsonProperty("identityKeyPair") byte[] identityKeyPair,
                     @JsonProperty("registrationId") Integer registrationId,
                     @JsonProperty("deviceId") Integer deviceId) {
            this.identityKeyPair = identityKeyPair;
            this.registrationId = registrationId;
            this.deviceId = deviceId;
        }
    }

    public static boolean hasIdentityStore(HomeDirectory homeDirectory) {
        return ProfileState.exists(homeDirectory);
    }

    public static SignalClientIdentityStore from(UUID profileId,
                                                 HomeDirectory homeDirectory,
                                                 Consumer<SignalProtocolStore> onInstall,
                                                 Consumer<ClientSignalProtocolStore> onReady) {
        Supplier<PrivateIdentity> privateIdentitySupplier = () -> PrivateIdentity.getOrCreate(homeDirectory);
        ClientSignalProtocolStore store;
        try {
            store = ClientSignalProtocolStore.from(homeDirectory);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create identity store", e);
        }
        File file;
        try {
            file = new File(homeDirectory.security(), "identityStore");
        } catch (IOException e) {
            throw new IllegalStateException("Could not create identity store", e);
        }
        State state;
        SignalClientIdentityStore clientIdentityStore;
        if (file.exists()) {
            try {
                state = Utils.messagePackMapper().readValue(file, State.class);
            } catch (IOException e) {
                throw new IllegalStateException("Could not read profile store", e);
            }
            clientIdentityStore = new SignalClientIdentityStore(profileId, privateIdentitySupplier, store, state, file);
            onReady.accept(store);
        } else {
            // Generate the new identity, its prekeys, etc
            IdentityKeyPair identityKeyPair = KeyHelper.generateIdentityKeyPair();
            int registrationId  = KeyHelper.generateRegistrationId(false);
            List<PreKeyRecord> preKeys = KeyHelper.generatePreKeys(0, 500);
            SignedPreKeyRecord signedPreKey;
            try {
                signedPreKey = KeyHelper.generateSignedPreKey(identityKeyPair, 0);
            } catch (InvalidKeyException e) {
                throw new IllegalStateException("problem generating signed preKey", e);
            }
            preKeys.forEach(preKeyRecord -> store.storePreKey(preKeyRecord.getId(), preKeyRecord));
            store.storeSignedPreKey(signedPreKey.getId(), signedPreKey);
            state = new State(identityKeyPair.serialize(), registrationId, null);
            // Save the identity state
            try {
                writeState(file, state);
            } catch (IOException e) {
                throw new IllegalStateException("Could not create new profile store", e);
            }
            // fire the on install consumer
            onInstall.accept(store);
            clientIdentityStore = new SignalClientIdentityStore(profileId, privateIdentitySupplier, store, state, file);
        }
        return clientIdentityStore;
    }

    private static void writeState(File file, State state) throws IOException {
        AtomicFile.write(file, theFile -> Utils.messagePackMapper().writeValue(theFile, state));
    }
}
