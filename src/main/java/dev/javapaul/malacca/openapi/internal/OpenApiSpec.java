package dev.javapaul.malacca.openapi.internal;

import java.util.Map;

public record OpenApiSpec(String openapi, OpenApiInfo info, Map<String, Map<String, OpenApiOperation>> paths) {
    public String hi(){
        return "hi";
    }
}
