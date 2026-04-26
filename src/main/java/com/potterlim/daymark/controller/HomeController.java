package com.potterlim.daymark.controller;

import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    private final Clock mClock;

    public HomeController(Clock clock) {
        mClock = clock;
    }

    @GetMapping("/")
    public String showHomePage(Model model) {
        model.addAttribute("today", LocalDate.now(mClock));
        return "home/index";
    }
}
