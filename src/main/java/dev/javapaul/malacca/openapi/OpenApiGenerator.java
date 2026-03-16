package dev.javapaul.malacca.openapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import dev.javapaul.malacca.annotation.Description;
import dev.javapaul.malacca.annotation.Header;   // [ADDED] import for @Header
import dev.javapaul.malacca.annotation.Headers;  // [ADDED] import for @Headers
import dev.javapaul.malacca.annotation.QueryParam;
import dev.javapaul.malacca.annotation.QueryParams;
import dev.javapaul.malacca.exception.internal.MalaccaInternalException;
import dev.javapaul.malacca.http.HttpMethod;
import dev.javapaul.malacca.openapi.internal.*;
import dev.javapaul.malacca.routing.PathMatcher;
import dev.javapaul.malacca.routing.RouteEntry;
import dev.javapaul.malacca.routing.Router;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

/**
 * Generates an OpenAPI 3.0 specification from the registered routes and API metadata.
 * Not meant to be instantiated — all methods are static.
 */
public class OpenApiGenerator {

    private static final ObjectMapper objectMapper = new ObjectMapper()
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    /**
     * Generates a JSON string representing the OpenAPI 3.0 specification
     * for all routes registered in the given router.
     *
     * @param router  the router containing all registered routes
     * @param apiInfo the API metadata — title, version, description
     * @return a JSON string of the full OpenAPI spec
     * @throws MalaccaInternalException if serialization fails
     */
    public static String generate(Router router, ApiInfo apiInfo) {
        Map<String, Map<String, OpenApiOperation>> paths = new LinkedHashMap<>();

        for (RouteEntry entry : router.routes()) {
            String pathPattern = entry.pathPattern();
            String httpMethod = entry.httpMethod().toString().toLowerCase();
            OpenApiOperation operation = buildOperation(entry);
            paths.computeIfAbsent(pathPattern, k -> new LinkedHashMap<>())
                    .put(httpMethod, operation);
        }

        OpenApiSpec spec = new OpenApiSpec(
                "3.0.0",
                new OpenApiInfo(apiInfo.title(), apiInfo.version(), apiInfo.description()),
                paths
        );

        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(spec);
        } catch (JsonProcessingException e) {
            throw new MalaccaInternalException("Failed to serialize OpenAPI spec", e);
        }
    }

    /**
     * Builds an {@link OpenApiOperation} for a single route entry.
     * Extracts path parameters, query parameters, header parameters, request body
     * schema and description from the handler method via reflection and annotations.
     *
     * @param entry the route entry to build the operation for
     * @return a fully populated OpenApiOperation
     */
    private static OpenApiOperation buildOperation(RouteEntry entry) {
        Method method = entry.handlerMethod();

        String summary = method.isAnnotationPresent(Description.class)
                ? method.getAnnotation(Description.class).value()
                : method.getName().replaceAll("([A-Z])", " $1").trim();

        List<OpenApiParameter> parameters = new ArrayList<>();

        for (String param : PathMatcher.getParams(entry.pathPattern())) {
            parameters.add(new OpenApiParameter(
                    param, "path", true,
                    new OpenApiSchema("string", null, null)
            ));
        }

        for (QueryParam param : resolveQueryParams(method)) {
            parameters.add(new OpenApiParameter(
                    param.name(), "query", param.required(),
                    new OpenApiSchema(param.type(), null, null)
            ));
        }

        // [ADDED] Resolve @Header annotations and add them to the parameters list with "in: header".
        // Headers are always typed as string since HTTP headers are always plain text values.
        for (Header header : resolveHeaders(method)) {
            parameters.add(new OpenApiParameter(
                    header.name(), "header", header.required(),
                    new OpenApiSchema("string", null, null)
            ));
        }

        OpenApiRequestBody requestBody = null;
        if (entry.httpMethod() == HttpMethod.POST ||
                entry.httpMethod() == HttpMethod.PUT ||
                entry.httpMethod() == HttpMethod.PATCH) {
            Class<?> bodyClass = resolveBodyClass(method);
            if (bodyClass != null) {
                OpenApiSchema schema = new OpenApiSchema("object", buildSchemaProperties(bodyClass), null);
                requestBody = new OpenApiRequestBody(true, Map.of("application/json", new OpenApiMediaType(schema)));
            }
        }

        return new OpenApiOperation(
                summary,
                parameters.isEmpty() ? null : parameters,
                requestBody,
                Map.of("200", new OpenApiResponse("Success"))
        );
    }

    /**
     * Resolves query parameter annotations from a handler method.
     * Supports both {@link QueryParams} container and single {@link QueryParam}.
     *
     * @param method the handler method
     * @return an array of QueryParam annotations, empty if none declared
     */
    private static QueryParam[] resolveQueryParams(Method method) {
        if (method.isAnnotationPresent(QueryParams.class))
            return method.getAnnotation(QueryParams.class).value();
        if (method.isAnnotationPresent(QueryParam.class))
            return new QueryParam[]{method.getAnnotation(QueryParam.class)};
        return new QueryParam[0];
    }

    // [ADDED] Resolves @Header annotations from a handler method.
    // Mirrors resolveQueryParams exactly — supports @Headers container for multiple headers
    // and falls back to a single @Header if only one is present.
    /**
     * Resolves header annotations from a handler method.
     * Supports both {@link Headers} container and single {@link Header}.
     *
     * @param method the handler method
     * @return an array of Header annotations, empty if none declared
     */
    private static Header[] resolveHeaders(Method method) {
        if (method.isAnnotationPresent(Headers.class))
            return method.getAnnotation(Headers.class).value();
        if (method.isAnnotationPresent(Header.class))
            return new Header[]{method.getAnnotation(Header.class)};
        return new Header[0];
    }

    /**
     * Resolves the body class from the handler method's {@code Request<T>} parameter.
     * Returns null if the parameter is {@code Request<?>} or has no type argument.
     *
     * @param method the handler method
     * @return the body class, or null if no concrete type is declared
     */
    private static Class<?> resolveBodyClass(Method method) {
        if (method.getParameterCount() == 0) return null;
        Type paramType = method.getGenericParameterTypes()[0];
        if (!(paramType instanceof ParameterizedType parameterizedType)) return null;
        Type typeArg = parameterizedType.getActualTypeArguments()[0];
        if (typeArg instanceof WildcardType) return null;
        if (typeArg instanceof Class<?> bodyClass) return bodyClass;
        return null;
    }

    /**
     * Builds a map of property names to their OpenAPI schemas for the given class.
     * Uses Jackson's bean introspection to extract field names and their OpenAPI schema representations.
     *
     * @param bodyClass the class to generate schema properties for
     * @return a map of field name to {@link OpenApiSchema}, or null if the class has no properties
     */
    private static Map<String, OpenApiSchema> buildSchemaProperties(Class<?> bodyClass) {
        JavaType javaType = objectMapper.constructType(bodyClass);
        BeanDescription beanDesc = objectMapper.getSerializationConfig().introspect(javaType);
        Map<String, OpenApiSchema> properties = new LinkedHashMap<>();
        for (BeanPropertyDefinition prop : beanDesc.findProperties()) {
            properties.put(prop.getName(), mapToOpenApiSchema(prop.getPrimaryType()));
        }
        return properties.isEmpty() ? null : properties;
    }

    /**
     * Maps a Jackson {@link JavaType} to its corresponding {@link OpenApiSchema}.
     * Handles primitives, strings, booleans, numbers, collections and nested objects.
     * For collections, recursively resolves the element type.
     * For objects, recursively builds schema properties.
     *
     * @param type the Jackson JavaType to map
     * @return the corresponding OpenApiSchema
     */
    private static OpenApiSchema mapToOpenApiSchema(JavaType type) {
        Class<?> raw = type.getRawClass();

        if (raw == String.class)
            return new OpenApiSchema("string", null, null);
        if (raw == int.class || raw == Integer.class ||
                raw == long.class || raw == Long.class)
            return new OpenApiSchema("integer", null, null);
        if (raw == boolean.class || raw == Boolean.class)
            return new OpenApiSchema("boolean", null, null);
        if (raw == double.class || raw == Double.class ||
                raw == float.class || raw == Float.class)
            return new OpenApiSchema("number", null, null);
        if (Collection.class.isAssignableFrom(raw)) {
            JavaType elementType = type.getContentType();
            OpenApiSchema items = elementType != null
                    ? mapToOpenApiSchema(elementType)
                    : new OpenApiSchema("string", null, null);
            return new OpenApiSchema("array", null, items);
        }

        return new OpenApiSchema("object", buildSchemaProperties(raw), null);
    }
}