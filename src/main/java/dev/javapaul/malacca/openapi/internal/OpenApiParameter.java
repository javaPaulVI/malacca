package dev.javapaul.malacca.openapi.internal;

public record OpenApiParameter(String name, String in, boolean required, OpenApiSchema schema) {
}
