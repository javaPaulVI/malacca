package io.github.javapaulvi.malacca.openapi;

import com.sun.net.httpserver.HttpExchange;
import io.github.javapaulvi.malacca.exception.internal.MalaccaInternalException;
import org.slf4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;


public class DocsHandler {

    private final byte[] openApiJsonBytes;
    private final byte[] openApiHtmlBytes;
//    private final byte[] openApiCssBytes;
//    private final byte[] openApiJsBytes;




    public DocsHandler(String openApiJson, Logger logger) {
        this.openApiJsonBytes = openApiJson.getBytes(StandardCharsets.UTF_8);
        try {
            this.openApiHtmlBytes = readAllBytes(getClass().getResourceAsStream("/swagger-ui.html"), "/swagger-ui.html");
//            this.openApiCssBytes = readAllBytes(getClass().getResourceAsStream("/swagger-ui.css"), "/swagger-ui.css");
//            this.openApiJsBytes = readAllBytes(getClass().getResourceAsStream("/swagger-ui-bundle.js"), "/swagger-ui-bundle.js");
//            logger.info("swagger-ui-bundle.js loaded: {} bytes", this.openApiJsBytes.length);
        } catch (IOException e) {
            throw new MalaccaInternalException("Error loading Swagger-UI resources",e);
        }
    }

    private static byte[] readAllBytes(InputStream is, String filename) throws IOException {
        if (is == null) throw new IOException("Could not find resource: " + filename);
        return is.readAllBytes();
    }

    public void handleDocs(HttpExchange exchange) throws IOException {

        exchange.getResponseHeaders().set("Content-Type", "text/html");
        exchange.sendResponseHeaders(200, openApiHtmlBytes.length);
        exchange.getResponseBody().write(openApiHtmlBytes);
        exchange.getResponseBody().close();

    }

    public void handleOpenApi(HttpExchange exchange) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, openApiJsonBytes.length);
        exchange.getResponseBody().write(openApiJsonBytes);
        exchange.getResponseBody().close();
    }

//    public void handleCss(HttpExchange exchange) throws IOException {
//        exchange.getResponseHeaders().set("Content-Type", "text/css");
//        exchange.sendResponseHeaders(200, openApiCssBytes.length);
//        exchange.getResponseBody().write(openApiCssBytes);
//        exchange.getResponseBody().close();
//    }
//
//    public void handleJs(HttpExchange exchange) throws IOException {
//        exchange.getResponseHeaders().set("Content-Type", "text/javascript");
//        exchange.sendResponseHeaders(200, openApiJsBytes.length);
//        exchange.getResponseBody().write(openApiJsBytes);
//        exchange.getResponseBody().close();
//
//    }
}
