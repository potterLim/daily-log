using System;
using System.Collections.Generic;

namespace DailyLog.ViewModels
{
    /// <summary>
    /// 저녁 회고용 뷰모델 – Goals 체크박스 + 기타 섹션.
    /// </summary>
    public sealed class EveningViewModel
    {
        public string DateString { get; init; } = DateTime.Today.ToString("yyyy-MM-dd");

        /// <summary>
        /// Goals 항목과 체크 여부
        /// </summary>
        public IList<GoalItem> Goals { get; init; } = new List<GoalItem>();

        public string Achievements { get; init; } = string.Empty;
        public string Improvements { get; init; } = string.Empty;
        public string Gratitude { get; init; } = string.Empty;
        public string Notes { get; init; } = string.Empty;
    }

    public sealed class GoalItem
    {
        public string Text { get; init; } = string.Empty;
        public bool IsDone { get; set; } = false;
    }
}
