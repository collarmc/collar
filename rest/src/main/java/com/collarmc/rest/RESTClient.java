package com.collarmc.rest;

import com.collarmc.api.authentication.AuthenticationService.LoginRequest;
import com.collarmc.api.authentication.AuthenticationService.LoginResponse;
import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.http.CreateGroupTokenRequest;
import com.collarmc.api.groups.http.CreateGroupTokenResponse;
import com.collarmc.api.groups.http.ValidateGroupTokenRequest;
import com.collarmc.api.groups.http.ValidateGroupTokenResponse;
import com.collarmc.api.http.HttpException;
import io.mikael.urlbuilder.UrlBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Entity;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
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
    public Optional<LoginResponse> login(String email, String password) {
        try {
            LoginResponse post = client.target(url("auth", "login").toUri()).request()
                    .post(Entity.json(LoginRequest.emailAndPassword(email, password)), LoginResponse.class);
            return Optional.ofNullable(post);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    /**
     * Lists all the groups the
     * @param apiToken of the user
     * @return all group information
     */
    @SuppressWarnings("unchecked")
    public Optional<List<Group>> groups(String apiToken) {
        try {
            List<Group> list = client.target(url("groups", "").toUri()).request()
                    .header("Authorization", "Bearer " + apiToken)
                    .get(List.class);
            return Optional.ofNullable(list);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    /**
     * Creates a group token used to verify that
     * @param apiToken of the user
     * @param group id of group
     * @return response
     */
    public Optional<CreateGroupTokenResponse> createGroupMembershipToken(String apiToken, UUID group) {
        try {
            CreateGroupTokenResponse post = client.target(url("groups", "token").toUri()).request()
                    .header("Authorization", "Bearer " + apiToken)
                    .post(Entity.json(new CreateGroupTokenRequest(group)), CreateGroupTokenResponse.class);
            return Optional.of(post);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    /**
     * Validates that the group membership token is valid
     * @param groupToken to validate
     * @return valid or not
     */
    public Optional<ValidateGroupTokenResponse> validateGroupMembershipToken(String groupToken, UUID group) {
        try {
            ValidateGroupTokenResponse response = client.target(url("groups", "validate").toUri()).request()
                    .post(Entity.json(new ValidateGroupTokenRequest(groupToken, group)), ValidateGroupTokenResponse.class);
            return Optional.of(response);
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    @Nonnull
    private UrlBuilder url(String api, String part) {
        return UrlBuilder.fromString(collarServerUrl + "/api/1/" + api + "/" + part);
    }
}
