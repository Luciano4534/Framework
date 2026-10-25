package framework;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import framework.exception.BindingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

class FrontControllerUploadSessionTest {

    private final FrontControllerServlet servlet = new FrontControllerServlet();
    private final HttpServletResponse resp = mock(HttpServletResponse.class);

    static class Formulaire {
        private MaFile fichier;

        public MaFile getFichier() {
            return fichier;
        }

        public void setFichier(MaFile fichier) {
            this.fichier = fichier;
        }
    }

    static class SampleController {

        public void upload(MaFile fichier) {
        }

        public void uploadRequired(
                @RequestParam(value = "fichier", required = true) MaFile fichier) {
        }

        public void withSession(Session session) {
        }

        public void form(Formulaire formulaire) {
        }
    }

    private static Method method(String name, Class<?>... parameterTypes) throws NoSuchMethodException {
        return SampleController.class.getDeclaredMethod(name, parameterTypes);
    }

    private static Part part(String fileName, byte[] data) throws Exception {
        Part part = mock(Part.class);
        when(part.getSubmittedFileName()).thenReturn(fileName);
        when(part.getInputStream()).thenReturn(new ByteArrayInputStream(data));
        return part;
    }

    @Test
    void bindsUploadedFile() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getContentType()).thenReturn("multipart/form-data; boundary=x");
        Part uploaded = part("photo.PNG", new byte[] {1, 2, 3});
        when(req.getPart("fichier")).thenReturn(uploaded);

        MaFile file = (MaFile) servlet.resolveArguments(method("upload", MaFile.class), req, resp)[0];

        assertEquals("photo.PNG", file.getFileName());
        assertEquals("png", file.getType());
        assertArrayEquals(new byte[] {1, 2, 3}, file.getData());
    }

    @Test
    void returnsNullWhenRequestIsNotMultipart() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getContentType()).thenReturn("application/x-www-form-urlencoded");

        assertNull(servlet.resolveArguments(method("upload", MaFile.class), req, resp)[0]);
    }

    @Test
    void throwsWhenRequiredFileIsMissing() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getContentType()).thenReturn("multipart/form-data; boundary=x");

        assertThrows(BindingException.class,
                () -> servlet.resolveArguments(method("uploadRequired", MaFile.class), req, resp));
    }

    @Test
    void bindsFileInsideObject() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);
        when(req.getContentType()).thenReturn("multipart/form-data; boundary=x");
        Part uploaded = part("cv.pdf", new byte[] {9});
        when(req.getPart("fichier")).thenReturn(uploaded);

        Formulaire form = (Formulaire) servlet.resolveArguments(
                method("form", Formulaire.class), req, resp)[0];

        assertNotNull(form.getFichier());
        assertEquals("cv.pdf", form.getFichier().getFileName());
        assertEquals("pdf", form.getFichier().getType());
    }

    @Test
    void injectsSession() throws Exception {
        HttpServletRequest req = mock(HttpServletRequest.class);

        Session session = (Session) servlet.resolveArguments(
                method("withSession", Session.class), req, resp)[0];

        assertNotNull(session.getId());
        verify(resp).addCookie(any(Cookie.class));
    }

    @Test
    void reusesSessionFromCookie() {
        SessionManager manager = new SessionManager();
        HttpServletRequest firstReq = mock(HttpServletRequest.class);
        HttpServletResponse firstResp = mock(HttpServletResponse.class);
        Session first = manager.getOrCreate(firstReq, firstResp);
        first.set("user", "Luciano");

        ArgumentCaptor<Cookie> captor = ArgumentCaptor.forClass(Cookie.class);
        verify(firstResp).addCookie(captor.capture());

        HttpServletRequest secondReq = mock(HttpServletRequest.class);
        when(secondReq.getCookies()).thenReturn(new Cookie[] {captor.getValue()});
        Session second = manager.getOrCreate(secondReq, mock(HttpServletResponse.class));

        assertSame(first, second);
        assertEquals("Luciano", second.get("user"));
    }

    @Test
    void removesExpiredSessions() throws Exception {
        SessionManager manager = new SessionManager(5L);
        manager.getOrCreate(mock(HttpServletRequest.class), mock(HttpServletResponse.class));

        Thread.sleep(20L);
        manager.cleanupExpired();

        assertEquals(0, manager.size());
    }

    @Test
    void savesFileToConfiguredDirectory(@TempDir Path directory) throws Exception {
        FileStorage storage = new FileStorage(directory);
        MaFile file = new MaFile("note.txt", new byte[] {65, 66});

        Path saved = storage.save(file);

        assertEquals(directory.resolve("note.txt"), saved);
        assertArrayEquals(new byte[] {65, 66}, Files.readAllBytes(saved));
    }

    @Test
    void throwsWhenStorageDirectoryIsNotConfigured() {
        FileStorage storage = new FileStorage(null);

        assertThrows(IllegalStateException.class,
                () -> storage.save(new MaFile("note.txt", new byte[] {1})));
    }
}
