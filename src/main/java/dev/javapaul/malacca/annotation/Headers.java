package dev.javapaul.malacca.annotation;

import java.lang.annotation.*;

/**
 * Container annotation for multiple {@link Header} annotations on the same method.
 * Applied automatically by the compiler when multiple @Header annotations are used.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Headers {
    Header[] value();
}