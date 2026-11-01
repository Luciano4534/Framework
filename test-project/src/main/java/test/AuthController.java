package test;

import java.util.List;
import java.util.Locale;

import framework.Authenticated;
import framework.ModelAndView;
import framework.Session;
import framework.UrlMapping;

public class AuthController {

    @UrlMapping(url = "/login", method = "GET")
    public ModelAndView loginForm() {
        return new ModelAndView("/login.jsp");
    }

    @UrlMapping(url = "/login", method = "POST")
    public ModelAndView login(String username, String password, Session session) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            return new ModelAndView("/login.jsp")
                    .addAttribut("error", "Nom d'utilisateur et mot de passe obligatoires");
        }

        List<String> roles = rolesFor(username);
        session.set("user", username);
        session.set("roles", roles);

        return new ModelAndView("/profil.jsp")
                .addAttribut("message", "Connecté : " + username)
                .addAttribut("roles", roles);
    }

    @UrlMapping(url = "/logout", method = "GET")
    public ModelAndView logout(Session session) {
        session.invalidate();
        return new ModelAndView("/login.jsp")
                .addAttribut("message", "Déconnecté");
    }

    @UrlMapping(url = "/profil", method = "GET")
    @Authenticated
    public ModelAndView profil(Session session) {
        return new ModelAndView("/profil.jsp")
                .addAttribut("message", "Profil de " + session.get("user"))
                .addAttribut("roles", session.get("roles"));
    }

    @UrlMapping(url = "/prof/cours", method = "GET")
    @Authenticated(roles = "prof")
    public ModelAndView cours(Session session) {
        return new ModelAndView("/profil.jsp")
                .addAttribut("message", "Cours du professeur " + session.get("user"))
                .addAttribut("roles", session.get("roles"));
    }

    @UrlMapping(url = "/admin", method = "GET")
    @Authenticated(roles = "admin")
    public ModelAndView admin(Session session) {
        return new ModelAndView("/profil.jsp")
                .addAttribut("message", "Administration : " + session.get("user"))
                .addAttribut("roles", session.get("roles"));
    }

    private List<String> rolesFor(String username) {
        String normalized = username.toLowerCase(Locale.ROOT);
        if (normalized.equals("admin") || normalized.equals("administrateur")) {
            return List.of("admin", "prof");
        }
        if (normalized.equals("prof") || normalized.equals("professeur")) {
            return List.of("prof");
        }
        return List.of("etudiant");
    }
}
