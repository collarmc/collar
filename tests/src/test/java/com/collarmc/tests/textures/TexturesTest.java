package com.collarmc.tests.textures;

import com.collarmc.api.identity.ClientIdentity;
import com.collarmc.security.mojang.MinecraftPlayer;
import com.google.common.io.ByteSource;
import com.google.common.io.Resources;
import org.junit.Ignore;
import org.junit.Test;
import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.GroupType;
import com.collarmc.api.groups.MemberSource;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.session.Player;
import com.collarmc.api.textures.TextureType;
import com.collarmc.client.api.textures.Texture;
import com.collarmc.server.Services;
import com.collarmc.server.services.profiles.ProfileServiceServer;
import com.collarmc.server.services.textures.TextureService.CreateTextureRequest;
import com.collarmc.tests.junit.CollarAssert;
import com.collarmc.tests.junit.CollarTest;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class TexturesTest extends CollarTest {

    UUID groupId = UUID.randomUUID();

    @Override
    protected void withServices(Services services) {
        Profile profile = services.profiles.getProfile(RequestContext.SERVER, ProfileServiceServer.GetProfileRequest.byEmail("alice@example.com")).profile;
        ByteSource source = Resources.asByteSource(Resources.getResource("cat.png"));
        try {
            services.textures.createTexture(new RequestContext(profile.id, profile.roles), new CreateTextureRequest(profile.id, null, TextureType.AVATAR, source.read()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        try {
            services.textures.createTexture(new RequestContext(profile.id, profile.roles), new CreateTextureRequest(null, groupId, TextureType.AVATAR, source.read()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @Ignore
    public void requestPlayerTexture() {
        AtomicReference<Texture> theTexture = new AtomicReference<>();
        alicePlayer.collar.textures().subscribe((collar, texturesApi, texture) -> theTexture.set(texture));

        alicePlayer.collar.textures().requestPlayerTexture(alicePlayer.collar.player(), TextureType.AVATAR);
        CollarAssert.waitForCondition("Receive the requested texture", () -> theTexture.get() != null);

        AtomicReference<Optional<BufferedImage>> imageRef = new AtomicReference<>();
        imageRef.set(Optional.empty());
        theTexture.get().loadImage(imageRef::set);

        CollarAssert.waitForCondition("Loaded the texture", () -> imageRef.get().isPresent());
    }

    @Test
    @Ignore
    public void requestGroupTexture() {
        AtomicReference<Texture> theTexture = new AtomicReference<>();
        alicePlayer.collar.textures().subscribe((collar, texturesApi, texture) -> theTexture.set(texture));

        Group group = Group.newGroup(groupId, "Cool group", GroupType.GROUP, new MemberSource(new Player(new ClientIdentity(UUID.randomUUID(), null, null), new MinecraftPlayer(UUID.randomUUID(), "hypoxel.net", 1)), null), new ArrayList<>());

        alicePlayer.collar.textures().requestGroupTexture(group, TextureType.AVATAR);
        CollarAssert.waitForCondition("Receive the requested texture", () -> theTexture.get() != null);

        AtomicReference<Optional<BufferedImage>> imageRef = new AtomicReference<>();
        theTexture.get().loadImage(imageRef::set);

        CollarAssert.waitForCondition("Loaded the texture", () -> imageRef.get().isPresent());
    }
}
