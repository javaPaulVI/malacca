package dev.javapaul.malacca.dispatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import dev.javapaul.malacca.exception.internal.ResponseWriteException;
import dev.javapaul.malacca.http.responses.Response;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

public class ResponseWriter {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseWriter(){

    }

    public void writeResponse(HttpExchange exchange, Response<?> response) {
        String body;
        try {
            if (response.isJson()) {
                body = objectMapper.writeValueAsString(response.body());
            } else {
                body = response.body().toString();
            }
        } catch (JsonProcessingException e) {
            throw new ResponseWriteException(e);
        }
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        if (response.isJson()) response.header("Content-Type", "application/json");
        else response.header("Content-Type", response.headers().get("Content-Type"));

        for (String key : response.headers().keySet()) {
            exchange.getResponseHeaders().set(key, response.headers().get(key));
        }
        try {
            exchange.sendResponseHeaders(response.statusCode(), bytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(bytes);
            os.close();
        } catch (IOException e){
            throw new ResponseWriteException(e);
        }
    }


}
