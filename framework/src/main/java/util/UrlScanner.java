package util;

import annotation.Url;
import exception.DuplicateURLException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;

/**
 * Utility class to scan controller classes and compile URL-to-method mappings.
 */
public class UrlScanner {

    /**
     * Scans a list of controller classes, extracting all methods annotated with {@link Url}.
     * Checks for and rejects duplicate URL mappings by throwing {@link DuplicateURLException}.
     *
     * @param controllers the list of controller classes to scan
     * @return a map of URL paths to their corresponding UrlMapping configurations
     * @throws DuplicateURLException if the same URL path is mapped more than once
     */
    public static HashMap<String, UrlMapping> scan(List<Class<?>> controllers) {
        HashMap<String, UrlMapping> mappings = new HashMap<>();

        for (Class<?> controller : controllers) {
            // Retrieve all methods of the controller class (public, private, protected, etc.)
            Method[] methods = controller.getDeclaredMethods();
            for (Method method : methods) {
                // Check if the method is annotated with @Url
                if (method.isAnnotationPresent(Url.class)) {
                    Url urlAnnotation = method.getAnnotation(Url.class);
                    String url = urlAnnotation.value();

                    // If URL is already mapped to another method, throw DuplicateURLException
                    if (mappings.containsKey(url)) {
                        throw new DuplicateURLException("URL déjà utilisée : " + url);
                    }

                    // Create mapping and put in map
                    UrlMapping mapping = new UrlMapping(controller.getName(), method.getName());
                    mappings.put(url, mapping);
                }
            }
        }

        return mappings;
    }
}
