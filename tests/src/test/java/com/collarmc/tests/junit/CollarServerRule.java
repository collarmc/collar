package com.collarmc.tests.junit;

import com.mongodb.client.MongoDatabase;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import spark.Spark;
import com.collarmc.api.http.HttpException;
import com.collarmc.http.HttpClient;
import com.collarmc.http.Request;
import com.collarmc.http.Response;
import com.collarmc.server.Services;
import com.collarmc.server.WebServer;
import com.collarmc.server.configuration.Configuration;
import com.collarmc.server.mongo.Mongo;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public final class CollarServerRule implements TestRule {

    private static final HttpClient http = new HttpClient();
    private final AtomicBoolean started = new AtomicBoolean(false);
    private final Consumer<Services> setupState;
    private Thread serverThread;
    private final Configuration configuration;
    public WebServer webServer;

    public CollarServerRule(Consumer<Services> setupState, Configuration configuration) {
        this.setupState = setupState;
        this.configuration = configuration;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        Mongo.getTestingDatabase().drop();
        MongoDatabase db = Mongo.getTestingDatabase();
        return new Statement() {
            @Override public void evaluate() throws Throwable {
                stopServer();
                serverThread = new Thread(() -> {
                    webServer = new WebServer(configuration);
                    try {
                        webServer.start(services -> {
                            setupState.accept(services);
                            started.set(true);
                        });
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    while (true) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ignored) {}
                    }
                });
                serverThread.start();
                while (!started.get()) {
                    Thread.sleep(500);
                }
                try {
                    base.evaluate();
                } finally {
                    serverThread.interrupt();
                    stopServer();
                    db.drop();
                }
            }
        };
    }

    private void stopServer() {
        Spark.stop();
        Spark.awaitStop();
    }

    public boolean isServerStarted() {
        try {
            http.execute(Request.url("http://localhost:3001/api/discover").get(), Response.noContent());
            return true;
        } catch (HttpException ignored) {
            return false;
        }
    }
}
