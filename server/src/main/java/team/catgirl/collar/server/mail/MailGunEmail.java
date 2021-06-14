package team.catgirl.collar.server.mail;

import com.google.common.io.BaseEncoding;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.http.Request;
import team.catgirl.collar.http.Response;
import team.catgirl.collar.server.http.AppUrlProvider;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MailGunEmail extends AbstractEmail {

    private static final Logger LOGGER = Logger.getLogger(MailGunEmail.class.getName());

    private final HttpClient http;
    private final String domain;
    private final String apiKey;

    public MailGunEmail(HttpClient http, AppUrlProvider urlProvider, String domain, String apiKey) {
        super(urlProvider);
        this.http = http;
        this.domain = domain;
        this.apiKey = apiKey;
    }

    @Override
    public void send(Profile profile, String subject, String templateName, Map<String, Object> variables) {
        variables = prepareVariables(profile, variables);
        Map<Object, Object> formBody = new HashMap<>();
        formBody.put("from", "noreply@collarmc.com");
        formBody.put("to", profile.email);
        formBody.put("subject", subject);
        formBody.put("html", renderHtml(templateName, variables));
        formBody.put("text", renderText(templateName, variables));

        java.net.http.HttpClient client = java.net.http.HttpClient.newBuilder()
                .version(java.net.http.HttpClient.Version.HTTP_2)
                .build();

        HttpRequest request = HttpRequest.newBuilder()
                .header("Authorization", "Basic " + BaseEncoding.base64().encode(("api" + ":" + apiKey).getBytes(StandardCharsets.UTF_8)))
                .POST(ofFormData(formBody))
                .uri(URI.create(String.format("https://api.mailgun.net/v3/%s/messages", domain)))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build();

        try {
            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException | InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Connection issue", e);
        }
    }

    private static HttpRequest.BodyPublisher ofFormData(Map<Object, Object> data) {
        var builder = new StringBuilder();
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            if (builder.length() > 0) {
                builder.append("&");
            }
            builder.append(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8));
            builder.append("=");
            builder.append(URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }
        return HttpRequest.BodyPublishers.ofString(builder.toString());
    }
}
