package com.collarmc.client.security;

import com.collarmc.api.groups.Group;
import com.collarmc.protocol.devices.DeviceRegisteredResponse;
import com.collarmc.protocol.groups.*;
import com.collarmc.protocol.identity.CreateTrustRequest;
import com.collarmc.security.ClientIdentity;
import com.collarmc.security.discrete.GroupSession;
import com.collarmc.security.discrete.IdentityStore;

import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ClientIdentityStore extends IdentityStore<ClientIdentity> {
    /**
     * @return the players identity
     */
    ClientIdentity identity();

    GroupSession createSession(Group group);

    GroupSessionManager groupSessions();

    /**
     * @param response of the registered device
     */
    void processDeviceRegisteredResponse(DeviceRegisteredResponse response);

    /**
     * @param identity joining group
     * @param groupId of the group being joined
     * @return join request
     */
    JoinGroupRequest createJoinGroupRequest(ClientIdentity identity, UUID groupId);

    /**
     * @param identity client identity to exchange keys with
     * @param id unique for this request
     * @return SendPreKeyRequest to send to the provided client identity
     */
    CreateTrustRequest createPreKeyRequest(ClientIdentity identity, long id);

    /**
     * Used to distribute keys back to the client who joined
     * @param resp of the join
     * @return acknowledged group joined request
     */
    AcknowledgedGroupJoinedRequest processJoinGroupResponse(JoinGroupResponse resp);

    /**
     * @param response to process
     */
    void processAcknowledgedGroupJoinedResponse(AcknowledgedGroupJoinedResponse response);

    /**
     * @param response to process
     */
    void processLeaveGroupResponse(LeaveGroupResponse response);

    /**
     * Clears all the current group sessions
     */
    void clearAllGroupSessions();

    /**
     * Resets the identity store and recreates it
     * @throws IOException when store could not be recreated
     */
    void reset() throws IOException;

    /**
     * Save state
     */
    void save() throws IOException;
}
