package annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation used to map a specific URL path to a controller method.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Url {
    /**
     * The URL path mapping (e.g., "/emp/list").
     *
     * @return the mapped URL path
     */
    String value();
}
