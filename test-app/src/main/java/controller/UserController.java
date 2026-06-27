package controller;

import annotation.Controller;
import annotation.Url;

/**
 * Example controller class annotated with @Controller.
 */
@Controller
public class UserController {

    @Url("/user/list")
    public String listUsers() {
        return "userList";
    }
}
