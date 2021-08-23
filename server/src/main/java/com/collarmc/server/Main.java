package com.collarmc.server;

import com.collarmc.server.configuration.Configuration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class.getName());

    public static void main(String[] args) throws Exception {
        Configuration configuration = args.length > 0 && "environment".equals(args[0]) ? Configuration.fromEnvironment() : Configuration.defaultConfiguration();
        WebServer webServer = new WebServer(configuration);
        webServer.start((services) -> LOGGER.info("Do you want to play a block game game?"));
    }
}
