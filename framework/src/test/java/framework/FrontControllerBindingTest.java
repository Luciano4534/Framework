package framework;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Map;

import org.junit.jupiter.api.Test;

import framework.exception.BindingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

class FrontControllerBindingTest {

    private final FrontControllerServlet servlet = new FrontControllerServlet();
    private final HttpServletResponse resp = mock(HttpServletResponse.class);

    static class Employe {
    }

    static class Address {
        private String rue;
        private String ville;

        public String getRue() {
            return rue;
        }

        public void setRue(String rue) {
            this.rue = rue;
        }

        public String getVille() {
            return ville;
        }

        public void setVille(String ville) {
            this.ville = ville;
        }
    }

    static class Bean {
        private String nom;
        private int age;
        private String appliedViaSetter;
        private String appliedViaField;
        private Address address;

        public Bean() {
        }

        public String getNom() {
            return nom;
        }

        public int getAge() {
            return age;
        }

        public String getAppliedViaSetter() {
            return appliedViaSetter;
        }

        public void setAppliedViaSetter(String appliedViaSetter) {
            this.appliedViaSetter = "setter:" + appliedViaSetter;
        }

        public String getAppliedViaField() {
            return appliedViaField;
        }

        public Address getAddress() {
            return address;
        }
    }

    static class NoDefaultConstructor {
        private String nom;

        public NoDefaultConstructor(String nom) {
            this.nom = nom;
        }

        public String getNom() {
            return nom;
        }
    }

    static class SampleController {

        public void byName(String nom, int age, double salaire) {
        }

        public void byAnnotation(@RequestParam("montant") double salaire) {
        }

        public void defaults(String nom, int age, long id, double salaire, float taux,
                boolean actif, short rang, byte niveau, char initiale) {
        }

        public void required(@RequestParam(value = "nom", required = true) String nom) {
        }

        public void complex(Employe employe) {
        }

        public void withRequest(HttpServletRequest request, String nom) {
        }

        public void bothServlets(HttpServletResponse response, HttpServletRequest request) {
        }

        public void objectArg(Bean bean) {
        }

        public void noCtorArg(NoDefaultConstructor value) {
        }
    }

    private static Method method(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return SampleController.class.getDeclaredMethod(name, parameterTypes);
    }

    private static HttpServletRequest request(Map<String, String> parameters) {
        HttpServletRequest req = mock(HttpServletRequest.class);
        parameters.forEach((name, value) -> when(req.getParameter(name)).thenReturn(value));
        return req;
    }

    @Test
    void bindsByNameFromRequestParameters() throws Exception {
        HttpServletRequest req = request(Map.of("nom", "Luciano", "age", "25", "salaire", "1234.5"));

        Object[] arguments = servlet.resolveArguments(
                method("byName", String.class, int.class, double.class), req, resp);

        assertArrayEquals(new Object[] {"Luciano", 25, 1234.5}, arguments);
    }

    @Test
    void bindsUsingRequestParamAnnotationName() throws Exception {
        HttpServletRequest req = request(Map.of("montant", "99.9"));

        Object[] arguments = servlet.resolveArguments(method("byAnnotation", double.class), req, resp);

        assertEquals(99.9, arguments[0]);
    }

    @Test
    void usesDefaultValuesWhenParameterAbsent() throws Exception {
        HttpServletRequest req = request(Map.of());

        Object[] arguments = servlet.resolveArguments(
                method("defaults", String.class, int.class, long.class, double.class, float.class,
                        boolean.class, short.class, byte.class, char.class),
                req, resp);

        assertNull(arguments[0]);
        assertEquals(0, arguments[1]);
        assertEquals(0L, arguments[2]);
        assertEquals(0.0, arguments[3]);
        assertEquals(0.0f, arguments[4]);
        assertEquals(false, arguments[5]);
        assertEquals((short) 0, arguments[6]);
        assertEquals((byte) 0, arguments[7]);
        assertEquals('\0', arguments[8]);
    }

    @Test
    void throwsWhenRequiredParameterAbsent() throws Exception {
        HttpServletRequest req = request(Map.of());

        assertThrows(BindingException.class,
                () -> servlet.resolveArguments(method("required", String.class), req, resp));
    }

    @Test
    void throwsWhenRequiredParameterBlank() throws Exception {
        HttpServletRequest req = request(Map.of("nom", "   "));

        assertThrows(BindingException.class,
                () -> servlet.resolveArguments(method("required", String.class), req, resp));
    }

    @Test
    void throwsWhenValueInvalid() throws Exception {
        HttpServletRequest req = request(Map.of("nom", "Luciano", "age", "abc", "salaire", "10"));

        assertThrows(BindingException.class,
                () -> servlet.resolveArguments(method("byName", String.class, int.class, double.class), req, resp));
    }

    @Test
    void bindsComplexObjectArgument() throws Exception {
        HttpServletRequest req = request(Map.of());

        Object[] arguments = servlet.resolveArguments(method("complex", Employe.class), req, resp);

        assertInstanceOf(Employe.class, arguments[0]);
    }

    @Test
    void injectsRequestAndBindsOtherParameters() throws Exception {
        HttpServletRequest req = request(Map.of("nom", "Luciano"));

        Object[] arguments = servlet.resolveArguments(
                method("withRequest", HttpServletRequest.class, String.class), req, resp);

        assertSame(req, arguments[0]);
        assertEquals("Luciano", arguments[1]);
    }

    @Test
    void injectsRequestAndResponse() throws Exception {
        HttpServletRequest req = request(Map.of());

        Object[] arguments = servlet.resolveArguments(
                method("bothServlets", HttpServletResponse.class, HttpServletRequest.class), req, resp);

        assertSame(resp, arguments[0]);
        assertSame(req, arguments[1]);
    }

    @Test
    void bindsObjectUsingSetterAndDirectField() throws Exception {
        HttpServletRequest req = request(Map.of(
                "nom", "Luciano",
                "age", "25",
                "appliedViaSetter", "S",
                "appliedViaField", "F",
                "address", "ignore"));

        Bean bean = (Bean) servlet.resolveArguments(method("objectArg", Bean.class), req, resp)[0];

        assertEquals("Luciano", bean.getNom());
        assertEquals(25, bean.getAge());
        assertEquals("setter:S", bean.getAppliedViaSetter());
        assertEquals("F", bean.getAppliedViaField());
    }

    @Test
    void ignoresNonSimpleField() throws Exception {
        HttpServletRequest req = request(Map.of("address", "12 rue de la Paix", "rue", "x"));

        Bean bean = (Bean) servlet.resolveArguments(method("objectArg", Bean.class), req, resp)[0];

        assertNull(bean.getAddress());
    }

    @Test
    void keepsObjectDefaultsWhenParametersAbsent() throws Exception {
        HttpServletRequest req = request(Map.of());

        Bean bean = (Bean) servlet.resolveArguments(method("objectArg", Bean.class), req, resp)[0];

        assertNull(bean.getNom());
        assertEquals(0, bean.getAge());
        assertNull(bean.getAppliedViaSetter());
        assertNull(bean.getAppliedViaField());
        assertNull(bean.getAddress());
    }

    @Test
    void throwsWhenObjectHasNoDefaultConstructor() throws Exception {
        HttpServletRequest req = request(Map.of("nom", "Luciano"));

        assertThrows(BindingException.class,
                () -> servlet.resolveArguments(method("noCtorArg", NoDefaultConstructor.class), req, resp));
    }
}
