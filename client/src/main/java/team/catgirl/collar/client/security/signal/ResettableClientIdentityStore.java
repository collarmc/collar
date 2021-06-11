package team.catgirl.collar.client.security.signal;

import team.catgirl.collar.client.security.ClientIdentityStore;
import team.catgirl.collar.protocol.devices.DeviceRegisteredResponse;
import team.catgirl.collar.protocol.groups.*;
import team.catgirl.collar.protocol.identity.CreateTrustRequest;
import team.catgirl.collar.protocol.signal.ResendPreKeysResponse;
import team.catgirl.collar.protocol.signal.SendPreKeysRequest;
import team.catgirl.collar.security.ClientIdentity;
import team.catgirl.collar.security.Identity;
import team.catgirl.collar.security.ServerIdentity;
import team.catgirl.collar.security.cipher.Cipher;

import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;

public class ResettableClientIdentityStore implements ClientIdentityStore {
    private SignalClientIdentityStore currentIdentityStore;
    private final Supplier<SignalClientIdentityStore> supplier;

    public ResettableClientIdentityStore(Supplier<SignalClientIdentityStore> supplier) {
        this.currentIdentityStore = supplier.get();
        this.supplier = supplier;
    }

    @Override
    public byte[] privateIdentityToken() {
        return currentIdentityStore.privateIdentityToken();
    }

    @Override
    public ClientIdentity currentIdentity() {
        return currentIdentityStore.currentIdentity();
    }

    @Override
    public boolean isTrustedIdentity(Identity identity) {
        return currentIdentityStore.isTrustedIdentity(identity);
    }

    @Override
    public void trustIdentity(Identity owner, byte[] preKeyBundle) {
        currentIdentityStore.trustIdentity(owner, preKeyBundle);
    }

    @Override
    public Cipher createCypher() {
        return currentIdentityStore.createCypher();
    }

    @Override
    public void processDeviceRegisteredResponse(DeviceRegisteredResponse response) {
        currentIdentityStore.processDeviceRegisteredResponse(response);
    }

    @Override
    public int getDeviceId() {
        return currentIdentityStore.getDeviceId();
    }

    @Override
    public SendPreKeysRequest createPreKeyRequest(DeviceRegisteredResponse response) {
        return currentIdentityStore.createPreKeyRequest(response);
    }

    @Override
    public SendPreKeysRequest createPreKeyRequest(ServerIdentity identity) {
        return currentIdentityStore.createPreKeyRequest(identity);
    }

    @Override
    public CreateTrustRequest createPreKeyRequest(ClientIdentity identity, long id) {
        return currentIdentityStore.createPreKeyRequest(identity, id);
    }

    @Override
    public SendPreKeysRequest createPreKeyRequest(ResendPreKeysResponse response) {
        return currentIdentityStore.createPreKeyRequest(response);
    }

    @Override
    public JoinGroupRequest createJoinGroupRequest(ClientIdentity identity, UUID groupId) {
        return currentIdentityStore.createJoinGroupRequest(identity, groupId);
    }

    @Override
    public AcknowledgedGroupJoinedRequest processJoinGroupResponse(JoinGroupResponse resp) {
        return currentIdentityStore.processJoinGroupResponse(resp);
    }

    @Override
    public void processAcknowledgedGroupJoinedResponse(AcknowledgedGroupJoinedResponse response) {
        currentIdentityStore.processAcknowledgedGroupJoinedResponse(response);
    }

    @Override
    public void processLeaveGroupResponse(LeaveGroupResponse response) {
        currentIdentityStore.processLeaveGroupResponse(response);
    }

    @Override
    public void clearAllGroupSessions() {
        currentIdentityStore.clearAllGroupSessions();
    }

    @Override
    public void reset() throws IOException {
        currentIdentityStore.delete();
        currentIdentityStore = supplier.get();
    }

    @Override
    public void save() throws IOException {
        currentIdentityStore.save();
    }
}
