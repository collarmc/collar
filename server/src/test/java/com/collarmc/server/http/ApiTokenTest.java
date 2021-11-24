package com.collarmc.server.http;

import com.collarmc.api.groups.MembershipRole;
import com.collarmc.security.ApiToken;
import com.collarmc.api.http.RequestContext;
import com.collarmc.api.profiles.Profile;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.server.junit.MongoDatabaseTestRule;
import com.collarmc.server.security.TokenCrypterImpl;
import com.collarmc.server.security.hashing.PasswordHashing;
import com.collarmc.security.TokenCrypter;
import com.collarmc.server.services.profiles.ProfileServiceServer;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ApiTokenTest {

    @Rule
    public MongoDatabaseTestRule dbRule = new MongoDatabaseTestRule();
    private ProfileService profiles;

    @Before
    public void services() {
        profiles = new ProfileServiceServer(dbRule.db, new PasswordHashing("VSZL*bR8-=r]r5P_"));
    }

    @Test
    public void roundTrip() throws Exception {
        Profile profile = profiles.createProfile(RequestContext.ANON, new ProfileServiceServer.CreateProfileRequest("bob@example.com", "password", "Bob UwU")).profile;
        ApiToken token = new ApiToken(profile.id, new Date().getTime() * TimeUnit.HOURS.toMillis(24), profile.roles, Set.of(MembershipRole.MEMBER));
        TokenCrypter crypter = new TokenCrypterImpl("helloworld");
        String tokenString = token.serialize(crypter);
        ApiToken deserialized = ApiToken.deserialize(crypter, tokenString);
        Assert.assertEquals(deserialized.id, token.id);
        Assert.assertEquals(deserialized.expiresAt, token.expiresAt);
        Assert.assertTrue(deserialized.groupRoles.contains(MembershipRole.MEMBER));
    }
}
