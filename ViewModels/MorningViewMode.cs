using System;

namespace DailyLog.ViewModels
{
    public sealed class MorningViewModel
    {
        public string DateString { get; init; } = DateTime.Today.ToString("yyyy-MM-dd");

        /// <summary>목표(줄바꿈 구분)</summary>
        public string Goals { get; init; } = string.Empty;

        public string Focus { get; init; } = string.Empty;
        public string Challenges { get; init; } = string.Empty;
    }
}
