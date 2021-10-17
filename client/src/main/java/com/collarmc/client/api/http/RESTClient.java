package com.collarmc.client.api.http;

import com.collarmc.api.authentication.AuthenticationService.LoginRequest;
import com.collarmc.api.authentication.AuthenticationService.LoginResponse;
import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.http.CreateGroupTokenRequest;
import com.collarmc.api.groups.http.CreateGroupTokenResponse;
import com.collarmc.api.groups.http.ValidateGroupTokenRequest;
import com.collarmc.api.http.HttpException;
import io.mikael.urlbuilder.UrlBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Very simple client for REST interface
 */
public final class RESTClient {

    private final String collarServerUrl;
    private final Client client = ClientBuilder.newClient();

    public RESTClient(String collarServerUrl) {
        this.collarServerUrl = collarServerUrl;
    }

    /**
     * Login to collar
     * @param email to login with
     * @param password to login with
     * @return login response
     * @throws HttpException if something went wrong
     */
    public LoginResponse login(String email, String password) {
        return client.target(url("auth", "login").toUri()).request()
                .post(Entity.json(LoginRequest.emailAndPassword(email, password)), LoginResponse.class);
    }

    /**
     * Lists all the groups the
     * @param apiToken of the user
     * @return all group information
     */
    public List<Group> groups(String apiToken) {
        return client.target(url("groups", "").toUri()).request()
                .header("Authorization", "Bearer " + apiToken)
                .get(List.class);
    }

    /**
     * Creates a group token used to verify that
     * @param apiToken of the user
     * @param group id of group
     * @return response
     */
    public CreateGroupTokenResponse createGroupMembershipToken(String apiToken, UUID group) {
        return client.target(url("groups", "token").toUri()).request()
                .header("Authorization", "Bearer " + apiToken)
                .post(Entity.json(new CreateGroupTokenRequest(group)), CreateGroupTokenResponse.class);
    }

    /**
     * Validates that the group membership token is valid
     * @param groupToken to validate
     * @return valid or not
     */
    public boolean validateGroupMembershipToken(String groupToken, UUID group) {
        try {
            client.target(url("groups", "validate").toUri()).request()
                    .post(Entity.json(new ValidateGroupTokenRequest(groupToken, group)));
            return true;
        } catch (HttpException e) {
            return false;
        }
    }

    @Nonnull
    private UrlBuilder url(String api, String part) {
        return UrlBuilder.fromString(collarServerUrl + "/api/1/" + api + "/" + part);
    }
}
