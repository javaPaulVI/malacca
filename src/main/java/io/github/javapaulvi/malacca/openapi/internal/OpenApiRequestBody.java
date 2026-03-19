package io.github.javapaulvi.malacca.openapi.internal;

import java.util.Map;

public record OpenApiRequestBody(boolean required, Map<String, OpenApiMediaType> content) {
}
