package dev.javapaul.malacca.http.responses;

import java.util.Map;

public class PlainTextResponse extends Response<String>{
    public PlainTextResponse(String body) {
        super(body, 200, Map.of("Content-Type", "text/plain"));
    }

    @Override
    public boolean isJson() {
        return false;
    }

}
