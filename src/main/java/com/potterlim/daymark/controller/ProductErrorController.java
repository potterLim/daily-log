package com.potterlim.daymark.controller;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class ProductErrorController implements ErrorController {

    @RequestMapping("/error")
    public String showErrorPage(HttpServletResponse httpServletResponse) {
        httpServletResponse.setStatus(HttpStatus.NOT_FOUND.value());
        return "error/404";
    }
}
