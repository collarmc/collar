package com.collarmc.server.mail;

import com.collarmc.server.http.HandlebarsTemplateEngine;
import spark.ModelAndView;
import com.collarmc.api.profiles.Profile;
import com.collarmc.server.http.AppUrlProvider;

import java.util.HashMap;
import java.util.Map;

public abstract class AbstractEmail implements Email {
    private final HandlebarsTemplateEngine handlebars = new HandlebarsTemplateEngine("/emails");
    private final AppUrlProvider urlProvider;

    public AbstractEmail(AppUrlProvider urlProvider) {
        this.urlProvider = urlProvider;
    }

    protected Map<String, Object> prepareVariables(Profile profile, Map<String, Object> variables) {
        variables = new HashMap<>(variables);
        variables.put("name", profile.name);
        variables.put("email", profile.email);
        variables.put("homeUrl", urlProvider.homeUrl());
        variables.put("loginUrl", urlProvider.loginUrl());
        variables.put("signupUrl", urlProvider.signupUrl());
        variables.put("logoUrl", urlProvider.logoUrl(64));
        return variables;
    }

    protected String renderHtml(String templateName, Map<String, Object> variables) {
        return renderTemplate(templateName + "-html", variables);
    }

    protected String renderText(String templateName, Map<String, Object> variables) {
        return renderTemplate(templateName + "-txt", variables);
    }

    private String renderTemplate(String templateName, Map<String, Object> variables) {
        return handlebars.render(new ModelAndView(variables, templateName));
    }
}
