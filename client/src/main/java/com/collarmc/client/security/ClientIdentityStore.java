package com.collarmc.client.security;

import com.collarmc.api.groups.Group;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.protocol.devices.DeviceRegisteredResponse;
import com.collarmc.protocol.groups.*;
import com.collarmc.protocol.identity.IdentifyRequest;
import com.collarmc.protocol.identity.IdentifyResponse;
import com.collarmc.security.messages.GroupSession;
import com.collarmc.security.messages.IdentityStore;

import java.io.IOException;
import java.util.UUID;

public interface ClientIdentityStore extends IdentityStore<ClientIdentity> {
    /**
     * @return this players identity
     */
    ClientIdentity identity();

    /**
     * @return verified identity of the server
     */
    ServerIdentity serverIdentity();

    /**
     * Create a new group session
     * @param group to create session for
     * @return session
     */
    GroupSession createSession(Group group);

    /**
     * @return groups message sessions
     */
    GroupSessionManager groupSessions();

    /**
     * @return create the identify request to send to server
     */
    IdentifyRequest createIdentifyRequest();

    /**
     * Checks that the token sent in {@link IdentifyRequest#token} can be decrypted and verified using the stored ServerIdentity
     * @param response to verify
     * @return verified or not
     */
    boolean verifyIdentityResponse(IdentifyResponse response);

    /**
     * @param response of the registered device
     */
    IdentifyRequest processDeviceRegisteredResponse(DeviceRegisteredResponse response);

    /**
     * @param groupId of the group being joined
     * @return join request
     */
    JoinGroupRequest createJoinGroupRequest(UUID groupId);

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

    boolean isValid();
}
