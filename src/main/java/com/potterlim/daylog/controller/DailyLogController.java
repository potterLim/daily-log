package com.potterlim.daylog.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
import com.potterlim.daylog.dto.dailylog.DailyLogDayStatusDto;
import com.potterlim.daylog.dto.dailylog.MorningFormDto;
import com.potterlim.daylog.entity.UserAccount;
import com.potterlim.daylog.service.IDailyLogService;
import com.potterlim.daylog.support.DailyLogSectionType;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/daily-log")
public class DailyLogController {

    private final IDailyLogService mDailyLogService;

    public DailyLogController(IDailyLogService dailyLogService) {
        mDailyLogService = dailyLogService;
    }

    @GetMapping("/morning")
    public String showMorningDateList(@AuthenticationPrincipal UserAccount userAccount, Model model) {
        List<String> morningDates = mDailyLogService.listWeek(LocalDate.now(), userAccount.getId())
            .stream()
            .filter(DailyLogDayStatusDto::hasMorning)
            .map(dailyLogDayStatusDto -> dailyLogDayStatusDto.getDate().toString())
            .collect(Collectors.toList());

        model.addAttribute("morningDates", morningDates);
        model.addAttribute("defaultDate", LocalDate.now());
        return "dailylog/morning";
    }

    @GetMapping("/morning/edit")
    public String showMorningEditPage(
        @RequestParam("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
        @AuthenticationPrincipal UserAccount userAccount,
        Model model
    ) {
        MorningFormDto morningFormDto = new MorningFormDto();
        morningFormDto.setDate(date);
        morningFormDto.setGoals(mDailyLogService.readSection(date, userAccount.getId(), DailyLogSectionType.GOALS));
        morningFormDto.setFocus(mDailyLogService.readSection(date, userAccount.getId(), DailyLogSectionType.FOCUS));
        morningFormDto.setChallenges(mDailyLogService.readSection(date, userAccount.getId(), DailyLogSectionType.CHALLENGES));

        model.addAttribute("morningFormDto", morningFormDto);
        return "dailylog/morning-edit";
    }

    @PostMapping("/morning/save")
    public String saveMorningLog(
        @Valid @ModelAttribute("morningFormDto") MorningFormDto morningFormDto,
        BindingResult bindingResult,
        @AuthenticationPrincipal UserAccount userAccount,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "dailylog/morning-edit";
        }

        String goalsBlock = buildGoalLines(morningFormDto.getGoals());
        mDailyLogService.writeSection(morningFormDto.getDate(), userAccount.getId(), DailyLogSectionType.GOALS, goalsBlock);
        mDailyLogService.writeSection(morningFormDto.getDate(), userAccount.getId(), DailyLogSectionType.FOCUS, morningFormDto.getFocus());
        mDailyLogService.writeSection(morningFormDto.getDate(), userAccount.getId(), DailyLogSectionType.CHALLENGES, morningFormDto.getChallenges());

        redirectAttributes.addFlashAttribute("message", "✅ 아침 계획이 저장되었습니다.");
        return "redirect:/daily-log/morning";
    }

    private static String buildGoalLines(String goalsOrNull) {
        if (goalsOrNull == null || goalsOrNull.isBlank()) {
            return "";
        }

        return goalsOrNull.lines()
            .map(String::trim)
            .filter(goalLine -> !goalLine.isEmpty())
            .map(goalLine -> "- " + goalLine)
            .collect(Collectors.joining("\r\n"));
    }
}
