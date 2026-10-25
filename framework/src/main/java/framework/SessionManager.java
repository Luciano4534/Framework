package framework;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class SessionManager {
    public static final String CONTEXT_ATTRIBUTE = "framework.session-manager";
    public static final String SESSIONS_ATTRIBUTE = "framework.sessions";
    public static final String TIMEOUT_PARAMETER = "session-timeout";
    public static final String COOKIE_NAME = "FRAMEWORK_SESSION";
    public static final int DEFAULT_TIMEOUT_MINUTES = 30;

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private volatile long timeoutMillis = DEFAULT_TIMEOUT_MINUTES * 60_000L;

    public SessionManager() {
    }

    public SessionManager(long timeoutMillis) {
        setTimeoutMillis(timeoutMillis);
    }

    public static SessionManager from(ServletContext context) {
        Object existing = context.getAttribute(CONTEXT_ATTRIBUTE);
        if (existing instanceof SessionManager manager) {
            return manager;
        }

        SessionManager manager = new SessionManager();
        String timeout = context.getInitParameter(TIMEOUT_PARAMETER);
        if (timeout != null && !timeout.isBlank()) {
            try {
                manager.setTimeoutMinutes(Integer.parseInt(timeout.trim()));
            } catch (NumberFormatException ignored) {
                manager.setTimeoutMinutes(DEFAULT_TIMEOUT_MINUTES);
            }
        }
        context.setAttribute(CONTEXT_ATTRIBUTE, manager);
        context.setAttribute(SESSIONS_ATTRIBUTE, manager.sessions);
        return manager;
    }

    public Session getOrCreate(HttpServletRequest req, HttpServletResponse resp) {
        cleanupExpired();

        String id = readSessionId(req);
        if (id != null) {
            Session session = sessions.get(id);
            if (session != null && !session.isExpired(timeoutMillis)) {
                session.touch();
                return session;
            }
            if (session != null) {
                sessions.remove(id, session);
            }
        }

        Session session = new Session(UUID.randomUUID().toString());
        sessions.put(session.getId(), session);
        writeCookie(req, resp, session.getId());
        return session;
    }

    public Session find(String id) {
        return id == null ? null : sessions.get(id);
    }

    public void invalidate(Session session) {
        if (session != null) {
            session.invalidate();
            sessions.remove(session.getId(), session);
        }
    }

    public void invalidate(String id) {
        Session session = sessions.remove(id);
        if (session != null) {
            session.invalidate();
        }
    }

    public void cleanupExpired() {
        sessions.entrySet().removeIf(entry -> entry.getValue().isExpired(timeoutMillis));
    }

    public int size() {
        return sessions.size();
    }

    public Map<String, Session> getSessions() {
        return Collections.unmodifiableMap(sessions);
    }

    public long getTimeoutMillis() {
        return timeoutMillis;
    }

    public void setTimeoutMillis(long timeoutMillis) {
        this.timeoutMillis = Math.max(1L, timeoutMillis);
    }

    public void setTimeoutMinutes(int minutes) {
        setTimeoutMillis(minutes * 60_000L);
    }

    private String readSessionId(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void writeCookie(HttpServletRequest req, HttpServletResponse resp, String id) {
        Cookie cookie = new Cookie(COOKIE_NAME, id);
        String contextPath = req.getContextPath();
        cookie.setPath(contextPath == null || contextPath.isEmpty() ? "/" : contextPath);
        cookie.setHttpOnly(true);
        cookie.setSecure(req.isSecure());
        resp.addCookie(cookie);
    }
}
