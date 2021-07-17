package com.collarmc.server.mail;

import com.collarmc.api.profiles.Profile;

import java.util.Map;

public interface Email {
    void send(Profile profile, String subject, String templateName, Map<String, Object> variables);
}
