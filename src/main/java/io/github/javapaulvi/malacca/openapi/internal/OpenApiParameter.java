package io.github.javapaulvi.malacca.openapi.internal;

public record OpenApiParameter(String name, String in, boolean required, OpenApiSchema schema) {
}
