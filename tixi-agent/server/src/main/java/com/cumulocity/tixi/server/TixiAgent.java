package com.cumulocity.tixi.server;

import com.cumulocity.agent.server.Server;
import com.cumulocity.agent.server.ServerBuilder;

public class TixiAgent {

    public static void main(String[] args) {
        final Server server = ServerBuilder.on(8080)
                .application("Tixi")
                .loadConfiguration("client")
                .rest()
                .scan("com.cumulocity.tixi.server.resources")
                .scan("com.cumulocity.tixi.server.services")
                .scan("com.cumulocity.tixi.server.components")
                .build();
        server.start();
    }

}