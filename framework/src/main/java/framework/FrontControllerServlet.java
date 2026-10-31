package framework;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import framework.exception.BindingException;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@MultipartConfig
public class FrontControllerServlet extends HttpServlet {
    public static final String MAPPINGS_ATTRIBUTE = "framework.mappings";
    public static final String CONTROLLER_CLASSES_PARAMETER = "controller-classes";

    private Map<UrlMethode, Mapping> mappings = new LinkedHashMap<>();
    private final Map<Class<?>, Object> controllers = new ConcurrentHashMap<>();
    SessionManager sessionManager = new SessionManager();
    AuthConfiguration authConfiguration = AuthConfiguration.defaults();
    private FileStorage fileStorage = new FileStorage(null);

    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);

        ServletContext context = config.getServletContext();
        sessionManager = SessionManager.from(context);
        fileStorage = FileStorage.from(context);
        authConfiguration = AuthConfiguration.from(context);

        @SuppressWarnings("unchecked")
        Map<UrlMethode, Mapping> contextMappings =
                (Map<UrlMethode, Mapping>) context.getAttribute(MAPPINGS_ATTRIBUTE);
        if (contextMappings != null) {
            mappings = contextMappings;
            return;
        }

        String controllerClasses = config.getInitParameter(CONTROLLER_CLASSES_PARAMETER);
        if (controllerClasses == null || controllerClasses.isBlank()) {
            return;
        }

        try {
            List<Class<?>> controllers = new ArrayList<>();
            for (String className : controllerClasses.split(",")) {
                controllers.add(Class.forName(className.trim()));
            }
            initMapping(controllers);
        } catch (ClassNotFoundException exception) {
            throw new ServletException("Contrôleur introuvable", exception);
        }
    }

    public void initMapping(Collection<Class<?>> controllerTypes) {
        mappings = createMappings(controllerTypes);
    }

    public static Map<UrlMethode, Mapping> createMappings(Collection<Class<?>> controllerTypes) {
        Map<UrlMethode, Mapping> generatedMappings = new LinkedHashMap<>();

        for (Class<?> controllerType : controllerTypes) {
            for (Method method : ScanAnnotation.getMethodWithAnnotation(controllerType, UrlMapping.class)) {
                UrlMapping annotation = method.getAnnotation(UrlMapping.class);
                UrlMethode key = new UrlMethode(annotation.url(), annotation.method());
                generatedMappings.put(key, new Mapping(controllerType, method));
            }
        }
        return generatedMappings;
    }

    @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String path = req.getRequestURI();
        String contextPath = req.getContextPath();

        if (contextPath != null && !contextPath.isEmpty()) {
            path = path.substring(contextPath.length());
        }

        UrlMethode key = new UrlMethode(path, req.getMethod());
        Mapping mapping = mappings.get(key);

        if (mapping == null) {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Aucun mapping pour " + req.getMethod() + " " + path);
            return;
        }

        if (!authorize(mapping, req, resp)) {
            return;
        }

        //recuperation si c'est un  API rest  
        try {
            Object result = invoke(mapping, req, resp);
            APIREST apiRest = mapping.getMethod().getAnnotation(APIREST.class);

            // Une methode @APIREST renvoie du JSON : une String est convertie
            // en valeur JSON, sauf si elle est deja un document JSON valide.
            if (apiRest != null) {
                resp.setContentType("application/json; charset=UTF-8");
                if (result instanceof String stringResult && !apiRest.alreadyJson()) {
                    resp.getWriter().write(stringToJson(stringResult));
                } else {
                    resp.getWriter().write(JsonConverter.toJson(result, apiRest.alreadyJson()));
                }
                return;
            }

            if (!(result instanceof ModelAndView modelAndView)) {
                throw new IllegalStateException("La méthode doit retourner un ModelAndView");
            }

            addArgToRequest(req, modelAndView.getData());
            RequestDispatcher dispatcher = req.getRequestDispatcher(modelAndView.getView());
            if (dispatcher == null) {
                throw new ServletException("Vue introuvable : " + modelAndView.getView());
            }
            dispatcher.forward(req, resp);
        } catch (BindingException exception) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write(exception.getMessage());
        } catch (JsonProcessingException exception) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Impossible de convertir la réponse en JSON");
        } catch (InvocationTargetException exception) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Erreur pendant l'invocation de la méthode");
        } catch (ReflectiveOperationException | IllegalArgumentException | IllegalStateException exception) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("text/plain; charset=UTF-8");
            resp.getWriter().write("Impossible d'invoquer la méthode du mapping");
        }
    }

    private boolean authorize(Mapping mapping, HttpServletRequest req, HttpServletResponse resp)
            throws IOException {
        Authenticated annotation = mapping.getMethod().getAnnotation(Authenticated.class);
        if (annotation == null) {
            annotation = mapping.getType().getAnnotation(Authenticated.class);
        }
        if (annotation == null) {
            return true;
        }

        Session session = sessionManager.getOrCreate(req, resp);

        if (!authConfiguration.isAuthenticated(session)) {
            return reject(req, resp, HttpServletResponse.SC_UNAUTHORIZED, "Authentification requise");
        }

        if (!authConfiguration.hasAnyRole(session, annotation.roles())) {
            return reject(req, resp, HttpServletResponse.SC_FORBIDDEN, "Role insuffisant");
        }

        req.setAttribute(AuthConfiguration.USER_ATTRIBUTE,
                session.get(authConfiguration.getSessionKey()));
        return true;
    }

    private boolean reject(HttpServletRequest req, HttpServletResponse resp, int status, String message)
            throws IOException {
        String loginUrl = authConfiguration.getLoginUrl();
        if (status == HttpServletResponse.SC_UNAUTHORIZED && loginUrl != null) {
            resp.sendRedirect(req.getContextPath() + loginUrl);
            return false;
        }
        resp.sendError(status, message);
        return false;
    }

    private void addArgToRequest(HttpServletRequest req, Map<String, Object> data) {
        data.forEach(req::setAttribute);
    }

    private String stringToJson(String value) throws JsonProcessingException {
        return JsonConverter.toJson(value, false);
    }

    private Object invoke(Mapping mapping, HttpServletRequest req, HttpServletResponse resp)
            throws ReflectiveOperationException, BindingException {
        Method method = mapping.getMethod();
        method.setAccessible(true);
        Object controller = controllerFor(mapping.getType());
        Object[] arguments = resolveArguments(method, req, resp);
        return method.invoke(controller, arguments);
    }

    private Object controllerFor(Class<?> type) throws ReflectiveOperationException {
        Object controller = controllers.get(type);
        if (controller != null) {
            return controller;
        }
        synchronized (controllers) {
            controller = controllers.get(type);
            if (controller == null) {
                controller = type.getDeclaredConstructor().newInstance();
                controllers.put(type, controller);
            }
            return controller;
        }
    }

    Object[] resolveArguments(Method method, HttpServletRequest req, HttpServletResponse resp)
            throws BindingException {
        Parameter[] parameters = method.getParameters();
        Object[] arguments = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();

            if (type.isInstance(req)) {
                arguments[i] = req;
            } else if (type.isInstance(resp)) {
                arguments[i] = resp;
            } else if (type == Session.class) {
                arguments[i] = sessionManager.getOrCreate(req, resp);
            } else if (type == FileStorage.class) {
                arguments[i] = fileStorage;
            } else if (type == MaFile.class) {
                arguments[i] = resolveFile(resolveName(parameter), isRequired(parameter), req);
            } else if (isSimpleType(type)) {
                arguments[i] = resolveSimpleArgument(parameter, type, req);
            } else {
                arguments[i] = bindObject(type, req);
            }
        }
        return arguments;
    }

    Object bindObject(Class<?> type, HttpServletRequest req) throws BindingException {
        Object instance = newInstance(type);

        for (Class<?> current = type; current != null && current != Object.class;
                current = current.getSuperclass()) {
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                    continue;
                }
                if (field.getType() == MaFile.class) {
                    MaFile file = resolveFile(field.getName(), false, req);
                    if (file != null) {
                        setField(instance, field, file);
                    }
                    continue;
                }
                if (!isSimpleType(field.getType())) {
                    continue;
                }
                String value = req.getParameter(field.getName());
                if (value == null) {
                    continue;
                }
                Object converted = convert(field.getType(), value, field.getName());
                setField(instance, field, converted);
            }
        }
        return instance;
    }

    private Object newInstance(Class<?> type) throws BindingException {
        try {
            return type.getDeclaredConstructor().newInstance();
        } catch (NoSuchMethodException exception) {
            throw new BindingException("Aucun constructeur sans argument pour : " + type.getName(), exception);
        } catch (ReflectiveOperationException exception) {
            throw new BindingException("Impossible d'instancier : " + type.getName(), exception);
        }
    }

    private void setField(Object instance, Field field, Object value) throws BindingException {
        Method setter = findSetter(instance.getClass(), field.getName(), field.getType());
        try {
            if (setter != null) {
                setter.setAccessible(true);
                setter.invoke(instance, value);
                return;
            }
            field.setAccessible(true);
            field.set(instance, value);
        } catch (ReflectiveOperationException exception) {
            throw new BindingException("Impossible d'injecter le champ '" + field.getName()
                    + "' de " + instance.getClass().getName(), exception);
        }
    }

    private Method findSetter(Class<?> type, String fieldName, Class<?> fieldType) {
        if (fieldName.isEmpty()) {
            return null;
        }
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        for (Class<?> current = type; current != null && current != Object.class;
                current = current.getSuperclass()) {
            for (Method method : current.getDeclaredMethods()) {
                if (method.getName().equals(setterName)
                        && method.getParameterCount() == 1
                        && isCompatible(method.getParameterTypes()[0], fieldType)) {
                    return method;
                }
            }
        }
        return null;
    }

    private static boolean isCompatible(Class<?> candidate, Class<?> fieldType) {
        return box(candidate) == box(fieldType);
    }

    private static Class<?> box(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        return type;
    }

    private MaFile resolveFile(String name, boolean required, HttpServletRequest req)
            throws BindingException {
        if (!isMultipart(req)) {
            if (required) {
                throw new BindingException("Fichier obligatoire absent : " + name);
            }
            return null;
        }

        try {
            Part part = req.getPart(name);
            if (part == null || part.getSubmittedFileName() == null) {
                if (required) {
                    throw new BindingException("Fichier obligatoire absent : " + name);
                }
                return null;
            }
            byte[] data;
            try (InputStream input = part.getInputStream()) {
                data = input.readAllBytes();
            }
            return new MaFile(part.getSubmittedFileName(), data);
        } catch (IOException | ServletException exception) {
            throw new BindingException("Impossible de lire le fichier '" + name + "'", exception);
        }
    }

    private boolean isMultipart(HttpServletRequest req) {
        String contentType = req.getContentType();
        return contentType != null
                && contentType.toLowerCase(Locale.ROOT).startsWith("multipart/");
    }

    private Object resolveSimpleArgument(Parameter parameter, Class<?> type, HttpServletRequest req)
            throws BindingException {
        String name = resolveName(parameter);
        String value = req.getParameter(name);

        if (value == null) {
            if (isRequired(parameter)) {
                throw new BindingException("Paramètre obligatoire absent : " + name);
            }
            return defaultValue(type);
        }
        if (value.isBlank() && isRequired(parameter)) {
            throw new BindingException("Paramètre obligatoire absent : " + name);
        }
        return convert(type, value, name);
    }

    private String resolveName(Parameter parameter) {
        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
        if (annotation != null && !annotation.value().isBlank()) {
            return annotation.value();
        }
        return parameter.getName();
    }

    private boolean isRequired(Parameter parameter) {
        RequestParam annotation = parameter.getAnnotation(RequestParam.class);
        return annotation != null && annotation.required();
    }

    static boolean isSimpleType(Class<?> type) {
        return type.isPrimitive()
                || type == String.class
                || type == Integer.class
                || type == Long.class
                || type == Double.class
                || type == Float.class
                || type == Boolean.class
                || type == Short.class
                || type == Byte.class
                || type == Character.class;
    }

    static Object defaultValue(Class<?> type) {
        if (!type.isPrimitive()) {
            return null;
        }
        if (type == boolean.class) {
            return Boolean.FALSE;
        }
        if (type == char.class) {
            return '\0';
        }
        if (type == byte.class) {
            return (byte) 0;
        }
        if (type == short.class) {
            return (short) 0;
        }
        if (type == int.class) {
            return 0;
        }
        if (type == long.class) {
            return 0L;
        }
        if (type == float.class) {
            return 0f;
        }
        if (type == double.class) {
            return 0d;
        }
        return null;
    }

    static Object convert(Class<?> type, String value, String name) throws BindingException {
        String trimmed = value.trim();
        try {
            if (type == String.class) {
                return value;
            }
            if (type == int.class || type == Integer.class) {
                return Integer.valueOf(trimmed);
            }
            if (type == long.class || type == Long.class) {
                return Long.valueOf(trimmed);
            }
            if (type == double.class || type == Double.class) {
                return Double.valueOf(trimmed);
            }
            if (type == float.class || type == Float.class) {
                return Float.valueOf(trimmed);
            }
            if (type == short.class || type == Short.class) {
                return Short.valueOf(trimmed);
            }
            if (type == byte.class || type == Byte.class) {
                return Byte.valueOf(trimmed);
            }
            if (type == boolean.class || type == Boolean.class) {
                if (!"true".equalsIgnoreCase(trimmed) && !"false".equalsIgnoreCase(trimmed)) {
                    throw new BindingException("Valeur invalide pour le paramètre '" + name + "' : " + value);
                }
                return Boolean.valueOf(trimmed);
            }
            if (type == char.class || type == Character.class) {
                if (value.length() != 1) {
                    throw new BindingException("Valeur invalide pour le paramètre '" + name + "' : " + value);
                }
                return value.charAt(0);
            }
        } catch (NumberFormatException exception) {
            throw new BindingException("Valeur invalide pour le paramètre '" + name + "' : " + value, exception);
        }
        throw new BindingException("Type non supporté : " + type.getName());
    }
}
