package com.collarmc.server.mail;

import com.collarmc.server.http.AppUrlProvider;
import com.google.common.io.Files;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import com.collarmc.api.profiles.Profile;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class LocalEmail extends AbstractEmail {

    private final Logger LOGGER = LogManager.getLogger(LocalEmail.class.getName());

    public LocalEmail(AppUrlProvider urlProvider) {
        super(urlProvider);
    }

    @Override
    public void send(Profile profile, String subject, String templateName, Map<String, Object> variables) {
        variables = prepareVariables(profile, variables);
        String path = "target/emails/" + profile.name + "/" + System.currentTimeMillis() + "-" + templateName;
        write(new File(path + ".txt"), renderText(templateName, variables));
        write(new File(path + ".html"), renderHtml(templateName, variables));
    }

    private void write(File file, String value) {
        File parent = file.getParentFile();
        if (!parent.exists() && !parent.mkdirs()) {
            throw new IllegalStateException("could not make dir " + parent);
        }
        try {
            Files.write(value.getBytes(StandardCharsets.UTF_8), file);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOGGER.info("Email sent " + file);
    }
}
