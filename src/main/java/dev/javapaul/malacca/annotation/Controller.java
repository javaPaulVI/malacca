package dev.javapaul.malacca.annotation;


import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This is the annotation that marks a class as a controller.
 * If there is no Controller annotation on a class, Malacca will not scan it, even if its methods are annotated with @GET, @POST, etc.
 * Example: {@code @Controller("/api/v1")}
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Controller {
    String value();
}
