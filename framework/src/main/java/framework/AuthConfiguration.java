package framework;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import jakarta.servlet.ServletContext;

public final class AuthConfiguration {
    public static final String AUTH_KEY_PARAMETER = "auth-session-key";
    public static final String ROLES_KEY_PARAMETER = "roles-session-key";
    public static final String LOGIN_URL_PARAMETER = "login-url";
    public static final String DEFAULT_AUTH_KEY = "user";
    public static final String DEFAULT_ROLES_KEY = "roles";
    public static final String USER_ATTRIBUTE = "framework.auth.user";

    private final String sessionKey;
    private final String rolesKey;
    private final String loginUrl;

    public AuthConfiguration(String sessionKey, String rolesKey, String loginUrl) {
        this.sessionKey = valueOrDefault(sessionKey, DEFAULT_AUTH_KEY);
        this.rolesKey = valueOrDefault(rolesKey, DEFAULT_ROLES_KEY);
        this.loginUrl = blankToNull(loginUrl);
    }

    public static AuthConfiguration defaults() {
        return new AuthConfiguration(DEFAULT_AUTH_KEY, DEFAULT_ROLES_KEY, null);
    }

    public static AuthConfiguration from(ServletContext context) {
        return new AuthConfiguration(
                context.getInitParameter(AUTH_KEY_PARAMETER),
                context.getInitParameter(ROLES_KEY_PARAMETER),
                context.getInitParameter(LOGIN_URL_PARAMETER));
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public String getRolesKey() {
        return rolesKey;
    }

    public String getLoginUrl() {
        return loginUrl;
    }

    public boolean isAuthenticated(Session session) {
        if (session == null || !session.isValid()) {
            return false;
        }
        Object value = session.get(sessionKey);
        return value != null && !Boolean.FALSE.equals(value);
    }

    public boolean hasAnyRole(Session session, String[] requiredRoles) {
        if (requiredRoles == null || requiredRoles.length == 0) {
            return true;
        }
        Set<String> actualRoles = roles(session);
        for (String requiredRole : requiredRoles) {
            String role = normalize(requiredRole);
            if (role != null && actualRoles.contains(role)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> roles(Session session) {
        Set<String> roles = new LinkedHashSet<>();
        if (session == null || !session.isValid()) {
            return roles;
        }
        collectRoles(session.get(rolesKey), roles);
        return roles;
    }

    private static void collectRoles(Object value, Set<String> roles) {
        if (value == null) {
            return;
        }
        if (value instanceof String string) {
            for (String part : string.split(",")) {
                addRole(roles, part);
            }
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                addRole(roles, item == null ? null : String.valueOf(item));
            }
            return;
        }
        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int i = 0; i < length; i++) {
                addRole(roles, String.valueOf(Array.get(value, i)));
            }
        }
    }

    private static void addRole(Set<String> roles, String value) {
        String role = normalize(value);
        if (role != null) {
            roles.add(role);
        }
    }

    private static String valueOrDefault(String value, String defaultValue) {
        String normalized = blankToNull(value);
        return normalized == null ? defaultValue : normalized;
    }

    private static String normalize(String value) {
        String normalized = blankToNull(value);
        return normalized == null ? null : normalized.toLowerCase(Locale.ROOT);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
