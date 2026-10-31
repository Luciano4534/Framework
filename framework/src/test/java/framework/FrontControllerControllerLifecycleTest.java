package framework;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class FrontControllerControllerLifecycleTest {

    static class CountingController {
        static final AtomicInteger INSTANCES = new AtomicInteger();
        private int calls;

        CountingController() {
            INSTANCES.incrementAndGet();
        }

        @UrlMapping(url = "/counter/first", method = "GET")
        @APIREST(alreadyJson = true)
        public String first() {
            calls++;
            return "first";
        }

        @UrlMapping(url = "/counter/second", method = "GET")
        @APIREST(alreadyJson = true)
        public String second() {
            return "calls=" + calls;
        }
    }

    static class OtherController {
        static final AtomicInteger INSTANCES = new AtomicInteger();

        OtherController() {
            INSTANCES.incrementAndGet();
        }

        @UrlMapping(url = "/other", method = "GET")
        @APIREST(alreadyJson = true)
        public String other() {
            return "other";
        }
    }

    private FrontControllerServlet servlet;
    private HttpServletResponse resp;
    private StringWriter body;

    @BeforeEach
    void setUp() {
        CountingController.INSTANCES.set(0);
        OtherController.INSTANCES.set(0);
        servlet = new FrontControllerServlet();
        servlet.initMapping(List.of(CountingController.class, OtherController.class));

        body = new StringWriter();
        resp = mock(HttpServletResponse.class);
        try {
            when(resp.getWriter()).thenReturn(new PrintWriter(body));
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static HttpServletRequest request(String uri) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getMethod()).thenReturn("GET");
        when(req.getRequestURI()).thenReturn(uri);
        when(req.getContextPath()).thenReturn("");
        return req;
    }

    private void clearBody() {
        body.getBuffer().setLength(0);
    }

    @Test
    void instantiatesControllerOnlyOnceAndReusesIt() throws Exception {
        servlet.service(request("/counter/first"), resp);
        clearBody();
        servlet.service(request("/counter/second"), resp);

        assertEquals(1, CountingController.INSTANCES.get());
        assertEquals("calls=1", body.toString());
    }

    @Test
    void createsOneInstancePerControllerClass() throws Exception {
        servlet.service(request("/counter/first"), resp);
        servlet.service(request("/other"), resp);

        assertEquals(1, CountingController.INSTANCES.get());
        assertEquals(1, OtherController.INSTANCES.get());
    }
}
