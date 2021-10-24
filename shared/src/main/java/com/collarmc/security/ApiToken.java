package com.collarmc.security;

import com.collarmc.api.groups.MembershipRole;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Role;
import com.google.common.io.BaseEncoding;

import java.io.*;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Used for authorizing API calls
 */
public class ApiToken {

    private static final int VERSION = 3;

    public final UUID id;
    public final long expiresAt;
    public final Set<Role> roles;
    public final Set<MembershipRole> groupRoles;

    public ApiToken(UUID profileId, long expiresAt, Set<Role> roles, Set<MembershipRole> groupRoles) {
        this.id = profileId;
        this.expiresAt = expiresAt;
        this.roles = roles;
        this.groupRoles = groupRoles;
    }

    public ApiToken(UUID profileId, Set<Role> roles, Set<MembershipRole> groupRoles) {
        this.id = profileId;
        this.roles = roles;
        this.groupRoles = groupRoles;
        this.expiresAt = new Date().getTime() + TimeUnit.DAYS.toMillis(7);
    }

    public boolean isExpired() {
        return new Date().after(new Date(expiresAt));
    }

    public String serialize(TokenCrypter crypter) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.write(VERSION);
                dataStream.writeUTF(id.toString());
                dataStream.writeLong(expiresAt);
                dataStream.writeInt(roles.size());
                for (Role role : roles) {
                    dataStream.writeInt(role.ordinal());
                }
                dataStream.writeInt(groupRoles.size());
                for (MembershipRole token : groupRoles) {
                    dataStream.writeInt(token.ordinal());
                }
            }
            return BaseEncoding.base64Url().encode(crypter.crypt(outputStream.toByteArray()));
        }
    }

    public byte[] serializeToBytes(TokenCrypter crypter) throws IOException {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            try (DataOutputStream dataStream = new DataOutputStream(outputStream)) {
                dataStream.write(VERSION);
                dataStream.writeUTF(id.toString());
                dataStream.writeLong(expiresAt);
                dataStream.writeInt(roles.size());
                for (Role role : roles) {
                    dataStream.writeInt(role.ordinal());
                }
                dataStream.writeInt(groupRoles.size());
                for (MembershipRole token : groupRoles) {
                    dataStream.writeInt(token.ordinal());
                }
            }
            return crypter.crypt(outputStream.toByteArray());
        }
    }

    public static ApiToken deserialize(TokenCrypter crypter, byte[] bytes) throws IOException {
        byte[] decryptedBytes = crypter.decrypt(bytes);
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(decryptedBytes)) {
            try (DataInputStream dataStream = new DataInputStream(inputStream)) {
                int version = dataStream.read();
                String uuidAsString = dataStream.readUTF();
                long expiresAt = dataStream.readLong();
                Set<Role> roles = new HashSet<>();
                Set<MembershipRole> groupRoles = new HashSet<>();
                int rolesCount = dataStream.readInt();
                for (int i = 0; i < rolesCount; i++) {
                    int roleId = dataStream.readInt();
                    roles.add(Role.values()[roleId]);
                }
                switch (version) {
                    case 2:
                        break;
                    case 3:
                        int groupRolesCount = dataStream.readInt();
                        for (int i = 0; i < groupRolesCount; i++) {
                            int roleId = dataStream.readInt();
                            groupRoles.add(MembershipRole.values()[roleId]);
                        }
                        break;
                    default:
                        throw new IllegalStateException("unknown version " + version);
                }
                return new ApiToken(UUID.fromString(uuidAsString), expiresAt, roles, groupRoles);
            }
        }
    }

    public static ApiToken deserialize(TokenCrypter crypter, String token) throws IOException {
        byte[] bytes = BaseEncoding.base64Url().decode(token);
        return deserialize(crypter, bytes);
    }

    public RequestContext fromToken() {
        return new RequestContext(id, roles, groupRoles);
    }
}
