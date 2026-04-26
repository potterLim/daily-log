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
        LocalDate today = LocalDate.now(mClock);
        String todayDateText = today.toString();

        model.addAttribute("today", today);
        model.addAttribute("todayMorningPath", "/daymark/morning/edit?date=" + todayDateText);
        model.addAttribute("todayEveningPath", "/daymark/evening/edit?date=" + todayDateText);
        model.addAttribute("todayPreviewPath", "/daymark/preview?date=" + todayDateText);
        return "home/index";
    }
}
