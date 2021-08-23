package com.collarmc.server.services.profile;

import com.collarmc.api.http.HttpException;
import com.collarmc.api.http.HttpException.ConflictException;
import com.collarmc.api.http.HttpException.NotFoundException;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.api.profiles.ProfileService.GetProfileRequest;
import com.collarmc.server.junit.MongoDatabaseTestRule;
import com.collarmc.server.security.hashing.PasswordHashing;
import com.collarmc.server.services.profiles.ProfileServiceServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.fail;

public class ProfileServiceTest {
    @Rule
    public MongoDatabaseTestRule dbRule = new MongoDatabaseTestRule();
    private ProfileService profiles;

    @Before
    public void services() {
        profiles = new ProfileServiceServer(dbRule.db, new PasswordHashing("VSZL*bR8-=r]r5P_"));
    }

    @Test
    public void createProfile() throws Exception {
        try {
            profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byEmail("bob@example.com"));
            fail("Should not exist");
        } catch (NotFoundException ignored) {}

        Profile profile = profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest("bob@example.com", "password", "Bob UwU")).profile;
        Assert.assertEquals("bob@example.com", profile.email);
        Assert.assertNotEquals("password", profile.hashedPassword);
        Assert.assertEquals("Bob UwU", profile.name);
        Assert.assertNotNull(profile.id);

        try {
            profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest("bob@example.com", "password", "Bob UwU"));
            fail("should not be able to create a duplicate profile");
        } catch (ConflictException ignored) {}
    }

    @Test
    public void createProfileWithBadEmail() throws Exception {
        try {
            profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byEmail("bob@example.com"));
            fail("Should not exist");
        } catch (NotFoundException ignored) {}

        try {
            profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest("bad-romance", "password", "Bob UwU"));
            fail();
        } catch (HttpException.BadRequestException e) {
            Assert.assertEquals("email address is invalid", e.getMessage());
        }
    }

    @Test
    public void getByEmail() {
        Profile savedProfile = profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest("bob@example.com", "password", "Bob UwU")).profile;
        Profile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byEmail("bob@example.com")).profile;
        Assert.assertEquals("bob@example.com", profile.email);
        Assert.assertEquals("Bob UwU", profile.name);
        Assert.assertEquals(savedProfile.id, profile.id);
        Assert.assertEquals(savedProfile.hashedPassword, savedProfile.hashedPassword);
    }

    @Test
    public void getById() {
        Profile savedProfile = profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest("bob@example.com", "password", "Bob UwU")).profile;
        Profile profile = profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(savedProfile.id)).profile;
        Assert.assertEquals("bob@example.com", profile.email);
        Assert.assertEquals("Bob UwU", profile.name);
        Assert.assertEquals(savedProfile.id, profile.id);
        Assert.assertEquals(savedProfile.hashedPassword, savedProfile.hashedPassword);
    }
}
