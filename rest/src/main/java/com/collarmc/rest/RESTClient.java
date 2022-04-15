package com.collarmc.rest;

import com.collarmc.api.authentication.AuthenticationService.LoginRequest;
import com.collarmc.api.authentication.AuthenticationService.LoginResponse;
import com.collarmc.api.groups.http.*;
import com.collarmc.api.http.HttpException;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.common.io.BaseEncoding;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

/**
 * Very simple client for REST interface
 */
public final class RESTClient {

    private final HttpClient clientDelegate = HttpClient.newBuilder().build();

    private static final ObjectMapper JSON_MAPPER = new ObjectMapper()
                .enable(DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                .setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);

    private final String collarServerUrl;

    public RESTClient(String collarServerUrl) {
        this.collarServerUrl = collarServerUrl;
    }

    /**
     * Login to collar
     * @param req to login with
     * @return login response
     * @throws HttpException if something went wrong
     */
    public Optional<LoginResponse> login(LoginRequest req) {
        try {
            return Optional.ofNullable(post(uri("auth", "login"), req, LoginResponse.class));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    /**
     * Get groups for the token
     * @param apiToken for groups
     * @return groups response
     */
    public Optional<GetGroupsResponse> getGroups(String apiToken) {
        try {
            return Optional.of(get(uri("groups", "groups"),
                    GetGroupsResponse.class,
                    "Authorization",
                    "Bearer " + apiToken));
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
            return Optional.of(post(uri("groups", "token"),
                    new CreateGroupTokenRequest(group),
                    CreateGroupTokenResponse.class,
                    "Authorization",
                    "Bearer " + apiToken));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    /**
     * Add a group member
     * @param apiToken of the group
     * @param req of membership
     * @return response
     */
    public Optional<AddGroupMemberResponse> addGroupMember(String apiToken, AddGroupMemberRequest req) {
        try {
            return Optional.of(post(uri("groups/members", "add"),
                    req,
                    AddGroupMemberResponse.class,
                    "Authorization",
                    "Bearer " + apiToken));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    /**
     * Remove a group member
     * @param apiToken of the group
     * @param req of membership
     * @return response
     */
    public Optional<RemoveGroupMemberResponse> removeGroupMember(String apiToken, RemoveGroupMemberRequest req) {
        try {
            return Optional.of(post(uri("groups/members", "remove"),
                    req,
                    RemoveGroupMemberResponse.class,
                    "Authorization",
                    "Bearer " + apiToken));
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
            return Optional.of(post(uri("groups", "validate"),
                    new ValidateGroupTokenRequest(groupToken, group),
                    ValidateGroupTokenResponse.class));
        } catch (Throwable e) {
            return Optional.empty();
        }
    }

    @Nonnull
    private URI uri(String api, String part) {
        try {
            return new URI(collarServerUrl + "/api/1/" + api + "/" + part);
        } catch (URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private <T> T post(URI uri, Object req, Class<T> respType, String... headers) throws IOException {
        String body = JSON_MAPPER.writeValueAsString(req);
        HttpRequest.Builder post = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (headers.length > 0) {
            post = post.headers(headers);
        }
        HttpRequest request = post.build();
        return JSON_MAPPER.readValue(execute(request), respType);
    }

    private <T> T get(URI uri, Class<T> respType, String... headers) throws IOException {
        HttpRequest.Builder post = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(20))
                .GET();
        if (headers.length > 0) {
            post = post.headers(headers);
        }
        HttpRequest request = post.build();
        return JSON_MAPPER.readValue(execute(request), respType);
    }

    private String execute(HttpRequest request) {
        HttpResponse<String> response;
        try {
            response = clientDelegate.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
        int code = response.statusCode();
        if (code != 200) {
            throw new RuntimeException("status: " + code + " body: " + response.body());
        }
        return response.body();
    }

    public static void main(String[] args) {
        RESTClient client= new RESTClient("https://api.collarmc.com");
        client.login(LoginRequest.emailAndPassword(args[0], args[1])).ifPresent(loginResponse -> {
            System.out.println(loginResponse.token);
        });
    }
}
