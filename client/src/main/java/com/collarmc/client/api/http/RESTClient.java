package com.collarmc.client.api.http;

import com.collarmc.api.authentication.AuthenticationService.LoginRequest;
import com.collarmc.api.authentication.AuthenticationService.LoginResponse;
import com.collarmc.api.groups.http.CreateGroupTokenRequest;
import com.collarmc.api.groups.http.CreateGroupTokenResponse;
import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.http.ValidateGroupTokenRequest;
import com.collarmc.api.http.HttpException;
import com.collarmc.client.utils.Http;
import com.collarmc.http.HttpClient;
import com.collarmc.http.Request;
import com.collarmc.http.Response;
import com.fasterxml.jackson.core.type.TypeReference;
import io.mikael.urlbuilder.UrlBuilder;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.UUID;

/**
 * Very simple client for REST interface
 */
public final class RESTClient {

    private final String collarServerUrl;
    private final HttpClient http = Http.client();

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
        Request request = Request.url(url("auth", "login"))
                .postJson(LoginRequest.emailAndPassword(email, password));
        return http.execute(request, Response.json(LoginResponse.class));
    }

    /**
     * Lists all the groups the
     * @param apiToken of the user
     * @return all group information
     */
    public List<Group> groups(String apiToken) {
        Request authorization = Request.url(url("groups", ""))
                .addHeader("Authorization", "Bearer " + apiToken)
                .get();
        return http.execute(authorization, Response.json(new TypeReference<List<Group>>(){}));
    }

    /**
     * Creates a group token used to verify that
     * @param apiToken of the user
     * @param group id of group
     * @return response
     */
    public CreateGroupTokenResponse createGroupMembershipToken(String apiToken, UUID group) {
        Request authorization = Request.url(url("groups", "token"))
                .addHeader("Authorization", "Bearer " + apiToken)
                .postJson(new CreateGroupTokenRequest(group));
        return http.execute(authorization, Response.json(CreateGroupTokenResponse.class));
    }

    /**
     * Validates that the group membership token is valid
     * @param groupToken to validate
     * @return valid or not
     */
    public boolean validateGroupMembershipToken(String groupToken, String group) {
        Request authorization = Request.url(url("groups", "validate"))
                .postJson(new ValidateGroupTokenRequest(groupToken, group));
        try {
            http.execute(authorization, Response.json(Void.class));
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
