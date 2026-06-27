package controller;

import annotation.Controller;
import annotation.Url;

/**
 * Example controller class annotated with @Controller.
 */
@Controller
public class TestController {

    @Url("/test")
    public String test() {
        return "test";
    }
}
