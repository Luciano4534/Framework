package framework;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FrontControllerListner implements ServletContextListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        ServletContext context = event.getServletContext();
        SessionManager.from(context);

        String uploadDir = context.getInitParameter(FileStorage.DIRECTORY_PARAMETER);
        if (uploadDir != null && !uploadDir.isBlank()) {
            context.setAttribute(FileStorage.DIRECTORY_ATTRIBUTE, uploadDir.trim());
        }

        String controllerClasses = context.getInitParameter(FrontControllerServlet.CONTROLLER_CLASSES_PARAMETER);

        if (controllerClasses == null || controllerClasses.isBlank()) {
            context.setAttribute(FrontControllerServlet.MAPPINGS_ATTRIBUTE, Map.of());
            return;
        }

        try {
            List<Class<?>> controllers = new ArrayList<>();
            for (String className : controllerClasses.split(",")) {
                controllers.add(Class.forName(className.trim(), true, context.getClassLoader()));
            }
            Map<UrlMethode, Mapping> mappings = FrontControllerServlet.createMappings(controllers);
            context.setAttribute(FrontControllerServlet.MAPPINGS_ATTRIBUTE, mappings);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("Contrôleur introuvable", exception);
        }
    }
}