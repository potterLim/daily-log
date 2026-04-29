package com.potterlim.daymark.controller;

import com.potterlim.daymark.config.DaymarkApplicationProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LayoutModelAttributeControllerAdvice {

    private final DaymarkApplicationProperties mDaymarkApplicationProperties;

    public LayoutModelAttributeControllerAdvice(DaymarkApplicationProperties daymarkApplicationProperties) {
        mDaymarkApplicationProperties = daymarkApplicationProperties;
    }

    @ModelAttribute
    public void populateLayoutState(Model model) {
        model.addAttribute("supportContactEmailAddress", mDaymarkApplicationProperties.getSupport().getContactEmail());
    }
}
