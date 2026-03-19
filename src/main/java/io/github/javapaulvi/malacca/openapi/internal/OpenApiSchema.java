package io.github.javapaulvi.malacca.openapi.internal;

import java.util.Map;

public record OpenApiSchema(String type, Map<String, OpenApiSchema> properties, OpenApiSchema items) {}