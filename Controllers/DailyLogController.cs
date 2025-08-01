using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Text;
using System.Text.RegularExpressions;
using Markdig;
using Microsoft.AspNetCore.Mvc;
using DailyLog.Services;
using DailyLog.ViewModels;
using System.Security.Claims;

namespace DailyLog.Controllers
{
    [Microsoft.AspNetCore.Authorization.Authorize]
    public sealed class DailyLogController : Controller
    {
        /* ─────────────── 마크다운 전처리 ─────────────── */
        private static string preprocessMarkdown(string md)
        {
            if (string.IsNullOrWhiteSpace(md))
                return md;

            var sb = new StringBuilder();
            var lines = md.Split(new[] { "\r\n", "\n" }, StringSplitOptions.None);

            foreach (var line in lines)
            {
                if (line.StartsWith("#"))
                {
                    int hashCount = 0;
                    while (hashCount < line.Length && line[hashCount] == '#')
                        hashCount++;

                    hashCount += 2;
                    string newHeader = new string('#', hashCount);
                    string rest = line.Substring(hashCount - 2).TrimStart('#', ' ');

                    sb.AppendLine();
                    sb.AppendLine($"{newHeader} {rest}");
                }
                else
                {
                    sb.AppendLine(line);
                }
            }

            return sb.ToString().TrimEnd();
        }

        /* ─────────────── 아침 목록 & 편집 ─────────────── */

        [HttpGet]
        public IActionResult Morning()
        {
            string userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var svc = new DailyLogService(Directory.GetCurrentDirectory(), userId);

            var dates = svc.ListWeek(DateTime.Today)
                            .Where(t => t.HasMorning)
                            .Select(t => t.Date.ToString("yyyy-MM-dd"))
                            .ToList();

            ViewData["DefaultDate"] = DateTime.Today.ToString("yyyy-MM-dd");
            return View(dates);
        }

        [HttpGet]
        public IActionResult MorningEdit(string date)
        {
            string userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var svc = new DailyLogService(Directory.GetCurrentDirectory(), userId);

            DateTime d = DateTime.ParseExact(date, "yyyy-MM-dd", null);

            var vm = new MorningViewModel
            {
                DateString = date,
                Goals = svc.ReadBlock(d, DailyLogService.HGoals),
                Focus = svc.ReadBlock(d, DailyLogService.HFocus),
                Challenges = svc.ReadBlock(d, DailyLogService.HChallenges)
            };

            return View("MorningEdit", vm);
        }

        [HttpPost, ValidateAntiForgeryToken]
        public IActionResult MorningSave(MorningViewModel vm)
        {
            string userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var svc = new DailyLogService(Directory.GetCurrentDirectory(), userId);

            DateTime d = DateTime.ParseExact(vm.DateString, "yyyy-MM-dd", null);

            string[] goalLines = (vm.Goals ?? string.Empty)
                                 .Replace("\r", string.Empty)
                                 .Split('\n', StringSplitOptions.RemoveEmptyEntries);

            string goalsBlock = string.Join(
                                    "\r\n",
                                    goalLines.Select(g => "- " + g.Trim()));

            svc.WriteBlock(d, DailyLogService.HGoals, goalsBlock);
            svc.WriteBlock(d, DailyLogService.HFocus, (vm.Focus ?? string.Empty).TrimEnd());
            svc.WriteBlock(d, DailyLogService.HChallenges, (vm.Challenges ?? string.Empty).TrimEnd());

            TempData["Msg"] = "✅ 아침 계획이 저장되었습니다.";
            return RedirectToAction(nameof(Morning));
        }

        /* ─────────────── 저녁 목록 & 편집 ─────────────── */

        [HttpGet]
        public IActionResult Evening(int week = 0)
        {
            string userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var svc = new DailyLogService(Directory.GetCurrentDirectory(), userId);

            DateTime refDay = DateTime.Today.AddDays(week * 7);
            DateTime start = refDay.AddDays(-3);
            DateTime end = start.AddDays(6);

            var dates = svc.ListWeek(refDay)
                            .Where(d => d.HasMorning || d.HasEvening)
                            .Select(d => d.Date.ToString("yyyy-MM-dd"))
                            .ToList();

            ViewData["WeekOffset"] = week;
            ViewData["RangeLabel"] = $"{start:yyyy-MM-dd} ~ {end:yyyy-MM-dd}";
            return View("Evening", dates);
        }

        [HttpGet]
        public IActionResult EveningEdit(string date)
        {
            string userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var svc = new DailyLogService(Directory.GetCurrentDirectory(), userId);

            DateTime d = DateTime.ParseExact(date, "yyyy-MM-dd", null);

            // 아침 Goals, Focus, Challenges 내용을 가져옴
            string goals = svc.ReadBlock(d, DailyLogService.HGoals);
            string focus = svc.ReadBlock(d, DailyLogService.HFocus);
            string challenges = svc.ReadBlock(d, DailyLogService.HChallenges);

            // 아침 로그를 Markdown 리스트 형태로 다시 구성
            var sbMorning = new StringBuilder();
            if (!string.IsNullOrWhiteSpace(goals))
            {
                sbMorning.AppendLine(DailyLogService.HGoals);
                sbMorning.AppendLine();
                foreach (string line in goals.Split(new[] { "\r\n", "\n" }, StringSplitOptions.RemoveEmptyEntries))
                {
                    sbMorning.AppendLine("- " + line.Trim());
                }
                sbMorning.AppendLine();
            }
            if (!string.IsNullOrWhiteSpace(focus))
            {
                sbMorning.AppendLine(DailyLogService.HFocus);
                sbMorning.AppendLine();
                foreach (string line in focus.Split(new[] { "\r\n", "\n" }, StringSplitOptions.RemoveEmptyEntries))
                {
                    sbMorning.AppendLine("- " + line.Trim());
                }
                sbMorning.AppendLine();
            }
            if (!string.IsNullOrWhiteSpace(challenges))
            {
                sbMorning.AppendLine(DailyLogService.HChallenges);
                sbMorning.AppendLine();
                foreach (string line in challenges.Split(new[] { "\r\n", "\n" }, StringSplitOptions.RemoveEmptyEntries))
                {
                    sbMorning.AppendLine("- " + line.Trim());
                }
            }

            string mdMorningContent = sbMorning.ToString().TrimEnd();

            // 🔧 전처리: 목차 등급 낮추기 & 목차 전후 한줄 공백
            mdMorningContent = preprocessMarkdown(mdMorningContent);

            string morningMdHtml = Markdig.Markdown.ToHtml(
                                        mdMorningContent,
                                        new MarkdownPipelineBuilder().UseAdvancedExtensions().Build());

            ViewData["MorningLogHtml"] = string.IsNullOrWhiteSpace(morningMdHtml)
                                         ? "<p><em>아침 로그가 없습니다.</em></p>"
                                         : morningMdHtml;

            // 나머지(Goals 체크박스 처리)는 그대로 유지…
            List<string> goalsPlain = (goals ?? string.Empty)
                .Split(new[] { "\r\n", "\n" }, StringSplitOptions.RemoveEmptyEntries)
                .Select(line => line.Trim())
                .ToList();

            string path = Path.Combine(
                Directory.GetCurrentDirectory(),
                "logs",
                userId,
                $"{d:yyyy_MM}_Week{((d.Day - 1) / 7) + 1}",
                $"{d:yyyy-MM-dd}.md");

            HashSet<string> checkedGoals = new HashSet<string>();
            if (System.IO.File.Exists(path))
            {
                var lines = System.IO.File.ReadAllLines(path);
                foreach (var line in lines)
                {
                    if (line.StartsWith("- [x]", StringComparison.OrdinalIgnoreCase))
                    {
                        string checkedGoal = line.Substring(5).Trim();
                        checkedGoals.Add(checkedGoal);
                    }
                }
            }

            List<GoalItem> goalsState = goalsPlain.Select(g => new GoalItem
            {
                Text = g,
                IsDone = checkedGoals.Contains(g)
            }).ToList();

            var vm = new EveningViewModel
            {
                DateString = date,
                Goals = goalsState,
                Achievements = svc.ReadBlock(d, DailyLogService.HAchieve),
                Improvements = svc.ReadBlock(d, DailyLogService.HImprove),
                Gratitude = svc.ReadBlock(d, DailyLogService.HThanks),
                Notes = svc.ReadBlock(d, DailyLogService.HNotes)
            };

            return View("EveningEdit", vm);
        }

        [HttpPost, ValidateAntiForgeryToken]
        public IActionResult EveningSave(EveningViewModel vm)
        {
            string userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var svc = new DailyLogService(Directory.GetCurrentDirectory(), userId);

            DateTime d = DateTime.ParseExact(vm.DateString, "yyyy-MM-dd", null);

            var sbGoals = new StringBuilder();
            foreach (GoalItem g in vm.Goals ?? new List<GoalItem>())
            {
                sbGoals.AppendLine($"- [{(g.IsDone ? 'x' : ' ')}] {g.Text}");
            }

            svc.WriteBlock(d, DailyLogService.HEvGoals, sbGoals.ToString().TrimEnd());
            svc.WriteBlock(d, DailyLogService.HAchieve, (vm.Achievements ?? string.Empty).TrimEnd());
            svc.WriteBlock(d, DailyLogService.HImprove, (vm.Improvements ?? string.Empty).TrimEnd());
            svc.WriteBlock(d, DailyLogService.HThanks, (vm.Gratitude ?? string.Empty).TrimEnd());
            svc.WriteBlock(d, DailyLogService.HNotes, (vm.Notes ?? string.Empty).TrimEnd());

            TempData["Msg"] = "🌙 저녁 회고가 저장되었습니다.";
            return RedirectToAction(nameof(Evening));
        }

        /* ─────────────── 주간 통계 & 미리보기 ─────────────── */

        [HttpGet]
        public IActionResult Week()
        {
            string userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var svc = new DailyLogService(Directory.GetCurrentDirectory(), userId);

            var weekData = svc.ListWeek(DateTime.Today).ToList();
            var progressList = new List<(DateTime Date, int Achieved, int Total, int Percent)>();

            int weekAchieved = 0;
            int weekTotal = 0;

            foreach (var (date, hasMorning, hasEvening) in weekData)
            {
                string path = Path.Combine(
                    Directory.GetCurrentDirectory(),
                    "logs",
                    userId,
                    $"{date:yyyy_MM}_Week{((date.Day - 1) / 7) + 1}",
                    $"{date:yyyy-MM-dd}.md");

                if (!System.IO.File.Exists(path))
                {
                    continue;
                }

                var goals = (svc.ReadBlock(date, DailyLogService.HGoals) ?? string.Empty)
                    .Split(new[] { "\r\n", "\n" }, StringSplitOptions.RemoveEmptyEntries)
                    .Select(line => line.Trim())
                    .ToList();

                int total = goals.Count;
                int achieved = 0;

                HashSet<string> checkedGoals = new HashSet<string>();
                if (hasEvening)
                {
                    var lines = System.IO.File.ReadAllLines(path);
                    foreach (var line in lines)
                    {
                        if (line.StartsWith("- [x]", StringComparison.OrdinalIgnoreCase))
                        {
                            string checkedGoal = line.Substring(5).Trim();
                            checkedGoals.Add(checkedGoal);
                        }
                    }

                    foreach (var g in goals)
                    {
                        if (checkedGoals.Contains(g))
                        {
                            achieved++;
                        }
                    }
                }

                int percent = (total == 0) ? 0 : (int)((achieved / (double)total) * 100);

                progressList.Add((date, achieved, total, percent));
                weekAchieved += achieved;
                weekTotal += total;
            }

            int weekPercent = (weekTotal == 0) ? 0 : (int)((weekAchieved / (double)weekTotal) * 100);

            ViewData["ProgressList"] = progressList;
            ViewData["WeekAchieved"] = weekAchieved;
            ViewData["WeekTotal"] = weekTotal;
            ViewData["WeekPercent"] = weekPercent;

            return View(progressList);
        }

        [HttpGet]
        public IActionResult LogPreview(string date)
        {
            string userId = User.FindFirst(ClaimTypes.NameIdentifier)?.Value;
            var svc = new DailyLogService(Directory.GetCurrentDirectory(), userId);

            DateTime d = DateTime.ParseExact(date, "yyyy-MM-dd", null);

            string mdPath = Path.Combine(
                Directory.GetCurrentDirectory(),
                "logs",
                userId,
                $"{d:yyyy_MM}_Week{((d.Day - 1) / 7) + 1}",
                $"{d:yyyy-MM-dd}.md");

            if (!System.IO.File.Exists(mdPath))
            {
                return NotFound();
            }

            string mdContent = System.IO.File.ReadAllText(mdPath);

            // 🔧 전처리: 목차 등급 낮추기 & 목차 전후 한줄 공백
            mdContent = preprocessMarkdown(mdContent);

            string html = Markdig.Markdown.ToHtml(
                              mdContent,
                              new MarkdownPipelineBuilder().UseAdvancedExtensions().Build());

            ViewData["Date"] = date;
            return View("LogPreview", html);
        }
    }
}
