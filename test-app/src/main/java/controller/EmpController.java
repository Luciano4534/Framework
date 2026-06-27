package controller;

import annotation.Controller;
import annotation.Url;

/**
 * Example controller representing employee-related operations.
 */
@Controller
public class EmpController {

    @Url("/emp/list")
    public String list() {
        return "liste";
    }

    @Url("/emp/new")
    public String create() {
        return "create";
    }
}
