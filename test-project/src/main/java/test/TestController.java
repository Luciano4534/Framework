package test;

import java.util.LinkedHashMap;
import java.util.Map;

import framework.APIREST;
import framework.ModelAndView;
import framework.UrlMapping;

public class TestController {

    @UrlMapping(url = "/hello", method = "GET")
    public ModelAndView hello() {
        return new ModelAndView("/hello.jsp")
                .addAttribut("message", "Bonjour");
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
}
