package team.catgirl.collar.server.mail;

import com.google.common.io.BaseEncoding;
import team.catgirl.collar.api.http.HttpException;
import team.catgirl.collar.api.profiles.Profile;
import team.catgirl.collar.http.HttpClient;
import team.catgirl.collar.http.Request;
import team.catgirl.collar.http.Response;
import team.catgirl.collar.server.http.AppUrlProvider;

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
        Message message = new Message(
                "noreply@collarmc.com",
                profile.email,
                subject,
                renderHtml(templateName, variables),
                renderText(templateName, variables)
        );
        try {
            Request request = Request.url(String.format("https://api.mailgun.net/v3/%s/messages", domain))
                    .basicAuth("api", apiKey)
                    .postJson(message);
            http.execute(request, Response.noContent());
            LOGGER.log(Level.INFO, "Sent " + templateName + " email to " + profile.email);
        } catch (HttpException.BadRequestException e) {
            LOGGER.log(Level.SEVERE, "Connection issue " + e.body, e);
        } catch (HttpException e) {
            LOGGER.log(Level.SEVERE, "Connection issue", e);
        }
    }

    public static class Message {
        public final String from;
        public final String to;
        public final String subject;
        public final String html;
        public final String text;

        public Message(String from,
                       String to,
                       String subject,
                       String html,
                       String text) {
            this.from = from;
            this.to = to;
            this.subject = subject;
            this.html = html;
            this.text = text;
        }
    }
}
