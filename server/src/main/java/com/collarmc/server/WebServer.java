package com.collarmc.server;

import com.collarmc.api.authentication.AuthenticationService.*;
import com.collarmc.api.groups.Group;
import com.collarmc.api.groups.GroupType;
import com.collarmc.api.groups.MembershipRole;
import com.collarmc.api.groups.http.*;
import com.collarmc.api.http.*;
import com.collarmc.api.http.HttpException.BadRequestException;
import com.collarmc.api.http.HttpException.NotFoundException;
import com.collarmc.api.http.HttpException.UnauthorisedException;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.api.profiles.ProfileService.GetProfileRequest;
import com.collarmc.api.profiles.ProfileService.UpdateProfileRequest;
import com.collarmc.api.profiles.PublicProfile;
import com.collarmc.api.profiles.Role;
import com.collarmc.api.textures.TextureType;
import com.collarmc.protocol.groups.CreateGroupRequest;
import com.collarmc.server.common.ServerStatus;
import com.collarmc.server.common.ServerVersion;
import com.collarmc.server.configuration.Configuration;
import com.collarmc.server.http.ApiToken;
import com.collarmc.server.http.HandlebarsTemplateEngine;
import com.collarmc.server.services.authentication.TokenCrypter;
import com.collarmc.server.services.textures.TextureService;
import com.collarmc.server.session.ClientRegistrationService;
import com.collarmc.server.session.ClientRegistrationService.RegisterClientRequest;
import com.collarmc.utils.Utils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableList;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import spark.Request;

import javax.servlet.ServletOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static spark.Spark.*;

public class WebServer {

    private static final HandlebarsTemplateEngine TEMPLATE_ENGINE = new HandlebarsTemplateEngine("/templates");
    private static final Logger LOGGER = LogManager.getLogger(WebServer.class.getName());

    private final Configuration configuration;

    public WebServer(Configuration configuration) {
        this.configuration = configuration;
    }

    public void start(Consumer<Services> callback) throws Exception {
        LOGGER.info("Reticulating splines...");
        // Set http port
        port(configuration.httpPort);
        // Services
        Services services = new Services(configuration);

        // Setup WebSockets
        webSocketIdleTimeoutMillis((int) TimeUnit.SECONDS.toMillis(60));
        webSocket("/api/1/listen", new CollarServer(services));

        // Always serialize objects returned as JSON
        exception(HttpException.class, (e, request, response) -> {
            response.status(e.code);
            try {
                response.body(Utils.jsonMapper().writeValueAsString(new ErrorResponse(e.getMessage())));
            } catch (JsonProcessingException jsonProcessingException) {
                throw new RuntimeException(e);
            }
            LOGGER.error(request.pathInfo(), e);
        });

        exception(Exception.class, (e, request, response) -> {
            response.status(500);
            try {
                response.body(Utils.jsonMapper().writeValueAsString(new ErrorResponse(e.getMessage())));
            } catch (JsonProcessingException jsonProcessingException) {
                throw new RuntimeException(e);
            }
            LOGGER.error(request.pathInfo(), e);
        });

        staticFiles.location("/public");

//        // Setup CORS
        options("/*", (request, response) -> {
            String accessControlRequestHeaders = request.headers("Access-Control-Request-Headers");
            if (accessControlRequestHeaders != null) {
                response.header("Access-Control-Allow-Headers", accessControlRequestHeaders);
            }
            String accessControlRequestMethod = request.headers("Access-Control-Request-Method");
            if (accessControlRequestMethod != null) {
                response.header("Access-Control-Allow-Methods", accessControlRequestMethod);
            }
            response.header("Access-Control-Allow-Credentials", "true");
            return "OK";
        }, Object::toString);

        before((request, response) -> {
            response.header("Server", "Collar");
            response.header("Access-Control-Allow-Origin", "*");
            response.header("Access-Control-Allow-Credentials", "true");
        });

        // API routes
        path("/api", () -> {

            // Version 1
            path("/1", () -> {

                before("/*", (request, response) -> {
                    setupRequest(services.tokenCrypter, request);
                });

                // Used to test if API is available
                get("/", (request, response) -> new ServerStatusResponse("OK"), services.jsonMapper::writeValueAsString);

                path("/profile", () -> {
                    before("/*", (request, response) -> {
                        assertAuthenticated(request);
                    });
                    get("/", (request, response) -> {
                        String email = request.queryParams("email");
                        RequestContext context = from(request);
                        return services.profiles.getProfile(context, GetProfileRequest.byEmail(email));
                    });
                    // Get your own profile
                    get("/me", (request, response) -> {
                        RequestContext context = from(request);
                        return services.profiles.getProfile(context, GetProfileRequest.byId(context.owner)).profile;
                    }, services.jsonMapper::writeValueAsString);
                    // Get someone elses profile
                    get("/:id", (request, response) -> {
                        String id = request.params("id");
                        UUID uuid = UUID.fromString(id);
                        return services.profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(uuid)).profile.toPublic();
                    }, services.jsonMapper::writeValueAsString);
                    post("/reset", (request, response) -> {
                        LoginRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), LoginRequest.class);
                        RequestContext context = from(request);
                        LoginResponse loginResp = services.auth.login(context, req);
                        if (!context.hasRole(Role.ADMINISTRATOR) && !context.callerIs(loginResp.profile.id)) {
                            throw new BadRequestException("user mismatch");
                        }
                        services.profileStorage.delete(context.owner);
                        services.profiles.updateProfile(context, UpdateProfileRequest.resetKeys(loginResp.profile.id));
                        return new Object();
                    }, services.jsonMapper::writeValueAsString);
                    post("/devices/trust", (request, response) -> {
                        // TODO: update web app and remove
                        RequestContext context = from(request);
                        RegisterClientRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), RegisterClientRequest.class);
                        PublicProfile profile = services.profiles.getProfile(context, GetProfileRequest.byId(context.owner)).profile.toPublic();
                        services.deviceRegistration.onClientRegistered(profile, req.token);
                        return new ClientRegistrationService.RegisterClientResponse();
                    }, services.jsonMapper::writeValueAsString);
                    post("/client/register", (request, response) -> {
                        RequestContext context = from(request);
                        RegisterClientRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), RegisterClientRequest.class);
                        PublicProfile profile = services.profiles.getProfile(context, GetProfileRequest.byId(context.owner)).profile.toPublic();
                        services.deviceRegistration.onClientRegistered(profile, req.token);
                        return new ClientRegistrationService.RegisterClientResponse();
                    }, services.jsonMapper::writeValueAsString);
                    get("/textures/:type", (request, response) -> {
                        RequestContext context = from(request);
                        TextureType textureType = TextureType.valueOf(request.params("type").toUpperCase());
                        PublicProfile profile = services.profiles.getProfile(RequestContext.SERVER, GetProfileRequest.byId(context.owner)).profile.toPublic();
                        // Find all textures in groups
                        List<TextureService.Texture> groupTextures = services.groupStore.findGroupsContaining(profile)
                                .filter(group -> group.type == GroupType.GROUP)
                                .flatMap(group -> services.textures.findTextures(context, new TextureService.FindTexturesRequest(textureType, null, group.id)).textures.stream())
                                .collect(Collectors.toList());
                        // Find all the textures belonging to player
                        List<TextureService.Texture> playerTextures = services.textures.findTextures(from(request), new TextureService.FindTexturesRequest(textureType, context.owner, null)).textures;
                        return new TextureService.FindTexturesResponse(ImmutableList.<TextureService.Texture>builder().addAll(playerTextures).addAll(groupTextures).build());
                    }, services.jsonMapper::writeValueAsString);
                    post("/textures/upload", (request, response) -> {
                        RequestContext context = from(request);
                        TextureService.CreateTextureRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), TextureService.CreateTextureRequest.class);
                        if (req.group != null) {
                            Group group = services.groups.findGroup(req.group).orElseThrow(() -> new NotFoundException("could not find group " + req.group));
                            if (group.members.stream().noneMatch(member -> member.player.identity.id().equals(context.owner) && member.membershipRole == MembershipRole.OWNER)) {
                                throw new NotFoundException("could not find group " + req.group);
                            }
                        }
                        return services.textures.createTexture(context, req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/textures/:type/default", (request, response) -> {
                        RequestContext context = from(request);
                        UpdateProfileRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), UpdateProfileRequest.class);
                        context.assertCallerIs(req.profile);
                        if (req.cape != null) {
                            throw new HttpException.BadRequestException("missing capeTexture");
                        }
                        return services.profiles.updateProfile(context, req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/reset", (request, response) -> {
                        RequestContext context = from(request);
                        services.profileStorage.delete(context.owner);
                        services.profiles.updateProfile(context, UpdateProfileRequest.resetKeys(context.owner));
                        response.status(204);
                        return null;
                    }, services.jsonMapper::writeValueAsString);
                });

                path("/groups", () -> {
                    get("/groups", (request, response) -> {
                        RequestContext context = from(request);
                        context.assertNotAnonymous();
                        List<Group> groups = services.groupStore.findGroupsContaining(context.owner).collect(Collectors.toList());
                        return new GetGroupsResponse(groups);
                    }, services.jsonMapper::writeValueAsString);
                    post("/validate", (request, response) -> {
                        RequestContext context = from(request);
                        context.assertAnonymous();
                        ValidateGroupTokenRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), ValidateGroupTokenRequest.class);
                        return services.groups.validateGroupToken(req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/token", (request, response) -> {
                        RequestContext context = from(request);
                        context.assertNotAnonymous();
                        CreateGroupTokenRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), CreateGroupTokenRequest.class);
                        return services.groups.createGroupToken(context, req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/members/add", (request, response) -> {
                        RequestContext context = from(request);
                        AddGroupMemberRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), AddGroupMemberRequest.class);
                        context.assertNotAnonymous();
                        return services.groups.addGroupMember(context, req);
                    });
                    post("/members/remove", (request, response) -> {
                        RequestContext context = from(request);
                        RemoveGroupMemberRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), RemoveGroupMemberRequest.class);
                        context.assertNotAnonymous();
                        return services.groups.removeGroupMember(context, req);
                    });
                });

                path("/auth", () -> {
                    before("/*", (request, response) -> {
                        from(request).assertAnonymous();
                    });
                    // Login
                    post("/login", (request, response) -> {
                        LoginRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), LoginRequest.class);
                        return services.auth.login(from(request), req);
                    }, services.jsonMapper::writeValueAsString);
                    // Create an account
                    post("/create", (request, response) -> {
                        CreateAccountRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), CreateAccountRequest.class);
                        return services.auth.createAccount(from(request), req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/verify", (request, response) -> {
                        VerifyAccountRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), VerifyAccountRequest.class);
                        return services.auth.verify(from(request), req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/reset/request", (request, response) -> {
                        RequestPasswordResetRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), RequestPasswordResetRequest.class);
                        return services.auth.requestPasswordReset(from(request), req);
                    }, services.jsonMapper::writeValueAsString);
                    post("/reset/perform", (request, response) -> {
                        ResetPasswordRequest req = services.jsonMapper.readValue(request.bodyAsBytes(), ResetPasswordRequest.class);
                        return services.auth.resetPassword(from(request), req);
                    });
                });

                get("/textures/:id/png", (request, response) -> {
                    String idAsString = request.params("id");
                    UUID uuid = UUID.fromString(idAsString);
                    byte[] bytes = services.textures.getTextureContent(new TextureService.GetTextureContentRequest(uuid)).content.bytes;
                    response.raw().setStatus(200);
                    response.raw().addHeader("Content-Type", "image/png");
                    response.raw().setContentLength(bytes.length);
                    ServletOutputStream outputStream = response.raw().getOutputStream();
                    outputStream.write(bytes);
                    outputStream.flush();
                    outputStream.close();
                    return response.raw();
                });
            });
        });

        // Reports server version
        // This contract is forever, please change with care!
        get("/api/version", (request, response) -> ServerVersion.version(), services.jsonMapper::writeValueAsString);
        get("/api/status", (request, response) -> new ServerStatus(services.profiles.playerCount(RequestContext.SERVER, new ProfileService.PlayerCountRequest()).total, services.sessions.count()), services.jsonMapper::writeValueAsString);
        // Query this route to discover what version of the APIs are supported and how the server is configured
        get("/api/discover", (request, response) -> {
            List<CollarVersion> versions = new ArrayList<>();
            versions.add(new CollarVersion(0, 1));
            List<CollarFeature> features = new ArrayList<>();
            features.add(new CollarFeature("auth:verification_scheme", configuration.minecraftSessionVerifier.getName()));
            features.add(new CollarFeature("groups:locations", true));
            features.add(new CollarFeature("groups:waypoints", true));
            features.add(new CollarFeature("profile:friends", true));
            return new DiscoverResponse(versions, features);
        }, services.jsonMapper::writeValueAsString);

        callback.accept(services);
        LOGGER.info("Collar server started.");
        LOGGER.info(services.urlProvider.homeUrl());
    }

    public static RequestContext from(Request req) {
        return req.attribute("requestContext");
    }

    private void assertAuthenticated(Request request) {
        // Must let in OPTIONS because CORS is horrible
        if (!request.requestMethod().equals("OPTIONS")) {
            from(request).assertNotAnonymous();
        }
    }

    /**
     * @param request http request
     * @throws IOException on token decoding
     */
    private static void setupRequest(TokenCrypter crypter, Request request) throws IOException {
        String authorization = request.headers("Authorization");
        RequestContext context;
        if (authorization == null) {
            context = RequestContext.ANON;
        } else if (authorization.startsWith("Bearer ")) {
            String tokenString = authorization.substring(authorization.indexOf(" ") + 1);
            ApiToken token = ApiToken.deserialize(crypter, tokenString);
            if (token.isExpired()) {
                throw new UnauthorisedException("expired token");
            }
            context = token.fromToken();
        } else {
            throw new UnauthorisedException("bad authorization header");
        }
        request.attribute("requestContext", context);
    }

    public static final class ErrorResponse {
        public final String message;

        public ErrorResponse(String message) {
            this.message = message;
        }
    }
}
