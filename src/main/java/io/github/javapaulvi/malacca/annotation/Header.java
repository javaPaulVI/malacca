package io.github.javapaulvi.malacca.annotation;

import java.lang.annotation.*;

/**
 * Documents a request header parameter for a handler method.
 * Used by OpenApiGenerator to include the header in the generated OpenAPI spec.
 * The header value can be read at runtime via req.header("X-Api-Key").
 *
 * Example:
 * {@code @Header(name = "X-Api-Key", required = true)}
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Headers.class)
public @interface Header {
    String name();
    boolean required() default false;
}