package dev.javapaul.malacca.http.responses;

import java.util.Map;

public class HtmlResponse extends Response<String> {
    public HtmlResponse(String html) {
        super(html, 200, Map.of("Content-Type", "text/html"));
    }

    @Override
    public boolean isJson() { return false; }
}