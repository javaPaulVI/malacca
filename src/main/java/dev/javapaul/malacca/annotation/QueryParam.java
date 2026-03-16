package dev.javapaul.malacca.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(QueryParams.class)
public @interface QueryParam {
    String name();
    String type() default "string";
    boolean required() default false;
    String description() default "";
}