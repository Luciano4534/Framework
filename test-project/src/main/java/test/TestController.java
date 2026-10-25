package test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import framework.APIREST;
import framework.FileStorage;
import framework.MaFile;
import framework.ModelAndView;
import framework.RequestParam;
import framework.Session;
import framework.UrlMapping;
import test.model.Employe;

public class TestController {

    @UrlMapping(url = "/employe/formulaire", method = "GET")
    public ModelAndView formulaire() {
        return new ModelAndView("/formulaire.jsp");
    }

    @UrlMapping(url = "/employe/save", method = "POST")
    public ModelAndView save(String nom, int age, @RequestParam("salaire") double salaire) {
        return new ModelAndView("/hello.jsp")
                .addAttribut("message", "Employé " + nom + ", " + age + " ans, salaire " + salaire);
    }

    @UrlMapping(url = "/employe/formulaire-objet", method = "GET")
    public ModelAndView formulaireObjet() {
        return new ModelAndView("/formulaireObjet.jsp");
    }

    @UrlMapping(url = "/employe/save-objet", method = "POST")
    public ModelAndView saveObjet(Employe employe) {
        return new ModelAndView("/hello.jsp")
                .addAttribut("message", "Employé " + employe.getNom() + ", " + employe.getAge()
                        + " ans, salaire " + employe.getSalaire());
    }

    @UrlMapping(url = "/hello", method = "GET")
    public ModelAndView hello() {
        return new ModelAndView("/hello.jsp")
                .addAttribut("message", "Bonjour");
    }

    @UrlMapping(url = "/employe/upload", method = "GET")
    public ModelAndView uploadForm() {
        return new ModelAndView("/upload.jsp");
    }

    @UrlMapping(url = "/employe/upload", method = "POST")
    public ModelAndView upload(MaFile fichier, String description, FileStorage storage) throws IOException {
        if (fichier == null) {
            return new ModelAndView("/hello.jsp")
                    .addAttribut("message", "Aucun fichier reçu");
        }
        Path saved = storage.save(fichier);
        return new ModelAndView("/hello.jsp")
                .addAttribut("message", "Fichier " + fichier.getFileName() + " (" + fichier.getType()
                        + ", " + fichier.getSize() + " octets) enregistré dans " + saved
                        + (description == null || description.isBlank() ? "" : " - " + description));
    }

    @UrlMapping(url = "/session", method = "GET")
    public ModelAndView session(Session session, String nom) {
        if (nom != null && !nom.isBlank()) {
            session.set("nom", nom);
        }
        Integer visites = session.get("visites", Integer.class);
        int total = visites == null ? 1 : visites + 1;
        session.set("visites", total);
        String utilisateur = session.get("nom", String.class);
        return new ModelAndView("/hello.jsp")
                .addAttribut("message", "Session " + session.getId() + " - visites : " + total
                        + (utilisateur == null ? "" : " - utilisateur : " + utilisateur));
    }

    @UrlMapping(url = "/api/hello", method = "GET")
    @APIREST(alreadyJson = true)
    public String apiHello() {
        return "{\"message\":\"Bonjour API\"}";
    }

    @UrlMapping(url = "/api/user", method = "GET")
    @APIREST
    public Map<String, Object> apiUser() {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", 1);
        user.put("name", "Luciano");
        return user;
    }

    @UrlMapping (url = "/api/produit", method = "GET")
    @APIREST
    public Map<String, Object> produit() {
        Map<String, Object> produit = new LinkedHashMap<>();
        produit.put("id", 1);
        produit.put("nom", "Ordinateur");
        return produit;
    }

    @UrlMapping (url = "/api/sakafo", method = "GET")
   @APIREST
    public Map<String, Object> sakafo() {
        Map<String, Object> sakafo = new LinkedHashMap<>();
        sakafo.put("id", 1);
        sakafo.put("nom", "Ravitoto");
        return sakafo;
    }

    @UrlMapping (url = "/api/test", method = "GET")
    @APIREST 
    public String test(){
        return "huhu";
    }
    
    @UrlMapping  (url = "/api/test2", method = "GET")
    @APIREST
    public String test2(){
        return "test2";
    }

 
}
