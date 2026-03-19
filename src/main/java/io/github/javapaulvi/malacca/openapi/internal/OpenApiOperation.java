package io.github.javapaulvi.malacca.openapi.internal;

import java.util.List;
import java.util.Map;

public record OpenApiOperation(String summary, List<OpenApiParameter> parameters, OpenApiRequestBody requestBody, Map<String, OpenApiResponse> responses) {
}
