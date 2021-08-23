package com.collarmc.tools;

import com.collarmc.api.authentication.AuthenticationService.LoginRequest;
import com.collarmc.api.authentication.AuthenticationService.LoginResponse;
import com.collarmc.api.authentication.AuthenticationService.RequestPasswordResetRequest;
import com.collarmc.api.profiles.ProfileService;
import com.collarmc.api.profiles.ProfileService.GetProfileRequest;
import com.collarmc.api.profiles.ProfileService.GetProfileResponse;
import com.collarmc.http.HttpClient;
import com.collarmc.http.Request;
import com.collarmc.http.Response;
import io.mikael.urlbuilder.UrlBuilder;

public final class CollarApi {

    private final String baseURL;
    private final HttpClient http;
    private String token;

    public CollarApi(String baseURL, HttpClient http) {
        this.baseURL = baseURL;
        this.http = http;
    }

    public LoginResponse login(LoginRequest req) {
        Request.Builder url = Request.url(baseURL + "/auth/login");
        if (token != null) {
            url = url.addHeader("Authorization", "Bearer " + token);
        }
        return http.execute(url.postJson(req), Response.json(LoginResponse.class));
    }

    public void resetPassword(RequestPasswordResetRequest req) {
        Request.Builder url = Request.url(baseURL + "/auth/reset/request");
        if (token != null) {
            url = url.addHeader("Authorization", "Bearer " + token);
        }
        http.execute(url.postJson(req), Response.noContent());
    }

    public void updateProfile(ProfileService.UpdateProfileRequest req) {
        Request.Builder url = Request.url(baseURL + "/auth/reset/request");
        if (token != null) {
            url = url.addHeader("Authorization", "Bearer " + token);
        }
        http.execute(url.postJson(req), Response.noContent());
    }

    public GetProfileResponse getProfile(GetProfileRequest req) {
        UrlBuilder url = UrlBuilder.fromString(baseURL + "/profile")
                .addParameter("email", req.byEmail);
        return http.execute(Request.url(url).get(), Response.json(GetProfileResponse.class));
    }

    public void setToken(String token) {
        this.token = token;
    }
}
