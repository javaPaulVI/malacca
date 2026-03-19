package io.github.javapaulvi.malacca.server;

import com.sun.net.httpserver.HttpServer;

import io.github.javapaulvi.malacca.dispatch.Dispatcher;
import io.github.javapaulvi.malacca.exception.internal.ServerStartException;
import io.github.javapaulvi.malacca.openapi.DocsHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MalaccaHttpServer {
    private HttpServer server;
    private Logger logger;


    public void start(int port, String name, Dispatcher dispatcher, String docsUrl, String openApiJson){
        logger = LoggerFactory.getLogger("server.MalaccaHttpServer:" + name);
        try {
            this.server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            throw new ServerStartException(e);
        }




        if (docsUrl != null) {
            DocsHandler docsHandler = new DocsHandler(openApiJson, logger);
            server.createContext(docsUrl, docsHandler::handleDocs);
            logger.info("Started docs at {}", docsUrl);
            server.createContext("/openapi.json", docsHandler::handleOpenApi);
            logger.info("Started openapi.json at /openapi.json");
//            server.createContext("/swagger-ui.css", docsHandler::handleCss);
//            logger.info("(internal) Started swagger-ui.css at /swagger-ui.css");
//            server.createContext("/swagger-uclsi-bundle.js", docsHandler::handleJs);
//            logger.info("(internal) Started swagger-ui-bundle.js at /swagger-ui-bundle.js");
        }

        server.createContext("/", exchange -> dispatcher.dispatch(exchange));
        server.start();
        logger.info("Started full server on port {}", port);
    }

    public void stop(){

        server.stop(0); // 0 = stop immediately

    }
}