package framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class FrontControllerAuthTest {

    static class ProtectedController {

        @UrlMapping(url = "/public", method = "GET")
        @APIREST(alreadyJson = true)
        public String publicAction() {
            return "public";
        }

        @UrlMapping(url = "/profil", method = "GET")
        @Authenticated
        @APIREST(alreadyJson = true)
        public String profil() {
            return "profil";
        }

        @UrlMapping(url = "/prof", method = "GET")
        @Authenticated(roles = "prof")
        @APIREST(alreadyJson = true)
        public String prof() {
            return "prof";
        }

        @UrlMapping(url = "/admin", method = "GET")
        @Authenticated(roles = {"prof", "admin"})
        @APIREST(alreadyJson = true)
        public String adminOrProf() {
            return "admin";
        }
    }

    private FrontControllerServlet servlet;
    private HttpServletResponse resp;
    private StringWriter body;

    @BeforeEach
    void setUp() {
        servlet = new FrontControllerServlet();
        servlet.initMapping(List.of(ProtectedController.class));
        servlet.sessionManager = new SessionManager();
        servlet.authConfiguration = AuthConfiguration.defaults();

        body = new StringWriter();
        resp = mock(HttpServletResponse.class);
        try {
            when(resp.getWriter()).thenReturn(new PrintWriter(body));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServletRequest request(String method, String uri, Cookie... cookies) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn(method);
        when(req.getRequestURI()).thenReturn(uri);
        when(req.getContextPath()).thenReturn("");
        if (cookies.length > 0) {
            when(req.getCookies()).thenReturn(cookies);
        }
        return req;
    }

    private Cookie sessionCookie(String user, Object roles) {
        Session session = servlet.sessionManager.getOrCreate(
                request("GET", "/login"), mock(HttpServletResponse.class));
        session.set("user", user);
        session.set("roles", roles);
        return new Cookie(SessionManager.COOKIE_NAME, session.getId());
    }

    @Test
    void publicActionDoesNotRequireSession() throws Exception {
        servlet.service(request("GET", "/public"), resp);

        assertEquals("public", body.toString());
        verify(resp, never()).sendError(anyInt(), anyString());
        verify(resp, never()).sendRedirect(anyString());
    }

    @Test
    void rejectsUnauthenticatedUser() throws Exception {
        servlet.service(request("GET", "/profil"), resp);

        verify(resp).sendError(eq(HttpServletResponse.SC_UNAUTHORIZED), anyString());
        assertEquals("", body.toString());
    }

    @Test
    void acceptsAuthenticatedUser() throws Exception {
        Cookie cookie = sessionCookie("luciano", List.of("etudiant"));

        servlet.service(request("GET", "/profil", cookie), resp);

        assertEquals("profil", body.toString());
        verify(resp, never()).sendError(anyInt(), anyString());
    }

    @Test
    void rejectsUserWithoutRequiredRole() throws Exception {
        Cookie cookie = sessionCookie("luciano", List.of("etudiant"));

        servlet.service(request("GET", "/prof", cookie), resp);

        verify(resp).sendError(eq(HttpServletResponse.SC_FORBIDDEN), anyString());
        assertEquals("", body.toString());
    }

    @Test
    void acceptsUserWithRequiredRole() throws Exception {
        Cookie cookie = sessionCookie("prof", List.of("prof"));

        servlet.service(request("GET", "/prof", cookie), resp);

        assertEquals("prof", body.toString());
    }

    @Test
    void acceptsAnyOfMultipleRoles() throws Exception {
        Cookie cookie = sessionCookie("admin", new String[] {"admin"});

        servlet.service(request("GET", "/admin", cookie), resp);

        assertEquals("admin", body.toString());
    }

    @Test
    void readsRolesFromCommaSeparatedString() throws Exception {
        Cookie cookie = sessionCookie("prof", "etudiant, prof");

        servlet.service(request("GET", "/admin", cookie), resp);

        assertEquals("admin", body.toString());
    }

    @Test
    void readsRolesFromArray() throws Exception {
        Cookie cookie = sessionCookie("prof", new String[] {"etudiant", "prof"});

        servlet.service(request("GET", "/admin", cookie), resp);

        assertEquals("admin", body.toString());
    }

    @Test
    void usesConfiguredSessionKeys() throws Exception {
        servlet.authConfiguration = new AuthConfiguration("connected", "permissions", null);
        Session session = servlet.sessionManager.getOrCreate(
                request("GET", "/login"), mock(HttpServletResponse.class));
        session.set("connected", "luciano");
        session.set("permissions", List.of("prof"));
        Cookie cookie = new Cookie(SessionManager.COOKIE_NAME, session.getId());

        servlet.service(request("GET", "/prof", cookie), resp);

        assertEquals("prof", body.toString());
    }

    @Test
    void redirectsToLoginUrlWhenUnauthenticated() throws Exception {
        servlet.authConfiguration = new AuthConfiguration("user", "roles", "/login");

        servlet.service(request("GET", "/profil"), resp);

        verify(resp).sendRedirect("/login");
        verify(resp, never()).sendError(anyInt(), anyString());
    }

    @Test
    void readsAuthParametersFromServletContext() throws Exception {
        ServletContext context = mock(ServletContext.class);
        when(context.getInitParameter(AuthConfiguration.AUTH_KEY_PARAMETER)).thenReturn("connected");
        when(context.getInitParameter(AuthConfiguration.ROLES_KEY_PARAMETER)).thenReturn("permissions");
        when(context.getInitParameter(AuthConfiguration.LOGIN_URL_PARAMETER)).thenReturn("/login");
        when(context.getInitParameter(SessionManager.TIMEOUT_PARAMETER)).thenReturn(null);
        when(context.getInitParameter(FileStorage.DIRECTORY_PARAMETER)).thenReturn(null);
        when(context.getInitParameter(FrontControllerServlet.CONTROLLER_CLASSES_PARAMETER)).thenReturn(null);

        ServletConfig config = mock(ServletConfig.class);
        when(config.getServletContext()).thenReturn(context);
        when(config.getInitParameter(FrontControllerServlet.CONTROLLER_CLASSES_PARAMETER)).thenReturn(null);

        servlet.init(config);

        assertEquals("connected", servlet.authConfiguration.getSessionKey());
        assertEquals("permissions", servlet.authConfiguration.getRolesKey());
        assertEquals("/login", servlet.authConfiguration.getLoginUrl());
    }
}
