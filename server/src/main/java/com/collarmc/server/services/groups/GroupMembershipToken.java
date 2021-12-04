package com.collarmc.server.services.groups;

import com.collarmc.api.http.HttpException.UnauthorisedException;
import com.collarmc.api.io.IO;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

/**
 * Used for authenticating a Group membership outside of Collar
 */
public final class GroupMembershipToken {
    private static final int VERSION = 1;
    public final UUID group;
    public final UUID profile;
    public final Instant expiresAt;

    public GroupMembershipToken(UUID group, UUID profile, Instant expiresAt) {
        this.group = group;
        this.profile = profile;
        this.expiresAt = expiresAt;
    }

    public GroupMembershipToken(byte[] bytes) {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
             DataInputStream dis = new DataInputStream(bis)) {
            int version = dis.readInt();
            if (version != VERSION) {
                throw new IllegalStateException("bad version " + 1);
            }
            group = IO.readUUID(dis);
            profile = IO.readUUID(dis);
            expiresAt = Instant.ofEpochMilli(dis.readLong());
        } catch (IOException e) {
            throw new IllegalStateException("bad token " + new String(bytes, StandardCharsets.UTF_8), e);
        }
    }

    public void assertValid(UUID group) {
        if (expiresAt.isAfter(Instant.now())) {
            throw new UnauthorisedException("token expired");
        }
        if (!this.group.equals(group)) {
            throw new UnauthorisedException("incorrect group");
        }
    }

    public byte[] serialize() {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             DataOutputStream dos = new DataOutputStream(bos)) {
            dos.writeInt(VERSION);
            IO.writeUUIDToBytes(group);
            IO.writeUUIDToBytes(profile);
            dos.writeLong(expiresAt.toEpochMilli());
            return bos.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
