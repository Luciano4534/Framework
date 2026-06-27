package controller;

import annotation.Controller;
import annotation.Url;

/**
 * Example controller class annotated with @Controller.
 */
@Controller
public class LoginController {

    @Url("/login")
    public String login() {
        return "login";
    }
}
