package com.collarmc.client.security;

import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.MembershipState;
import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.api.identity.ServerIdentity;
import com.collarmc.client.HomeDirectory;
import com.collarmc.io.AtomicFile;
import com.collarmc.io.IO;
import com.collarmc.protocol.devices.ClientRegisteredResponse;
import com.collarmc.protocol.groups.*;
import com.collarmc.protocol.identity.IdentifyRequest;
import com.collarmc.protocol.identity.IdentifyResponse;
import com.collarmc.security.CollarIdentity;
import com.collarmc.security.TokenGenerator;
import com.collarmc.security.messages.Cipher;
import com.collarmc.security.messages.CipherException;
import com.collarmc.security.messages.GroupSession;
import com.collarmc.security.messages.SodiumCipher;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class ClientIdentityStoreImpl implements ClientIdentityStore {

    private static final Logger LOGGER = LogManager.getLogger(ClientIdentityStoreImpl.class);
    private final GroupSessionManager groupSessionManager = new GroupSessionManager(this);
    private final HomeDirectory homeDirectory;
    private CollarIdentity collarIdentity;

    private byte[] token = TokenGenerator.byteToken(256);

    public ClientIdentityStoreImpl(HomeDirectory homeDirectory) throws IOException, CipherException {
        this.homeDirectory = homeDirectory;
    }

    @Override
    public ClientIdentity identity() {
        return collarIdentity == null ? null : new ClientIdentity(collarIdentity.id, collarIdentity.publicKey());
    }

    @Override
    public ServerIdentity serverIdentity() {
        return collarIdentity == null ? null : collarIdentity.serverIdentity;
    }

    @Override
    public IdentifyRequest createIdentifyRequest() {
        if (isValid()) {
            try {
                collarIdentity = CollarIdentity.from(IO.readBytesFromFile(getIdentityFile(homeDirectory)));
                return new IdentifyRequest(identity(), cipher().encrypt(token, collarIdentity.serverIdentity));
            } catch (CipherException e) {
                LOGGER.log(Level.ERROR, "could not encrypt token", e);
                throw new IllegalStateException(e);
            } catch (IOException e) {
                LOGGER.log(Level.ERROR, "problem opening identity file", e);
                throw new IllegalStateException(e);
            }
        } else {
            return IdentifyRequest.unknown();
        }
    }

    @Override
    public boolean verifyIdentityResponse(IdentifyResponse response) {
        // Decrypt the token received from the server
        byte[] token;
        try {
            token = cipher().decrypt(response.token, collarIdentity.serverIdentity);
        } catch (CipherException e) {
            LOGGER.log(Level.ERROR, "could not decrypt token", e);
            return false;
        }
        // Check that the token from server matches the token we initially sent
        return Arrays.equals(this.token, token);
    }

    @Override
    public GroupSession createSession(Group group) {
        return new GroupSession(group.id, this, collarIdentity, group.members.stream().map(member -> member.player.identity).collect(Collectors.toSet()));
    }

    @Override
    public GroupSessionManager groupSessions() {
        return groupSessionManager;
    }

    @Override
    public Cipher cipher() {
        return new SodiumCipher(collarIdentity.keyPair, false);
    }

    @Override
    public IdentifyRequest processClientRegisteredResponse(ClientRegisteredResponse response) throws CipherException {
        collarIdentity = CollarIdentity.createClientIdentity(response.profile.id, response.serverIdentity);
        try {
            AtomicFile.write(getIdentityFile(homeDirectory), theFile -> IO.writeBytesToFile(theFile, collarIdentity.serialize()));
        } catch (IOException e) {
            throw new IllegalStateException("could not write collar identity", e);
        }
        return createIdentifyRequest();
    }

    @Override
    public JoinGroupRequest createJoinGroupRequest(UUID groupId) {
        return new JoinGroupRequest(groupId, MembershipState.ACCEPTED);
    }

    @Override
    public AcknowledgedGroupJoinedRequest processJoinGroupResponse(JoinGroupResponse resp) {
        groupSessionManager.createOrUpdate(resp.group);
        return new AcknowledgedGroupJoinedRequest(resp.sender, resp.group.id);
    }

    @Override
    public void processAcknowledgedGroupJoinedResponse(AcknowledgedGroupJoinedResponse response) {
        // Trust members
        groupSessionManager.createOrUpdate(response.group);
    }

    @Override
    public void processLeaveGroupResponse(LeaveGroupResponse response) {
        groupSessionManager.delete(response.groupId);
    }

    @Override
    public void clearAllGroupSessions() {
        groupSessionManager.clear();
    }

    @Override
    public void reset() throws IOException {
        File identityFile = getIdentityFile(homeDirectory);
        if (!identityFile.exists() || identityFile.delete()) {
            collarIdentity = null;
            token = TokenGenerator.byteToken(256);
            LOGGER.debug("Identity deleted");
        }
    }

    public boolean isValid() {
        try {
            File identityFile = getIdentityFile(homeDirectory);
            return (identityFile.exists() && identityFile.isFile()) || collarIdentity != null;
        } catch (IOException e) {
            return false;
        }
    }

    @Nonnull
    private static File getIdentityFile(HomeDirectory homeDirectory) throws IOException {
        return new File(homeDirectory.profile(), "identity.cif");
    }
}
