// Services/IDailyLogService.cs
using System;
using System.Collections.Generic;

namespace DailyLog.Services
{
    public interface IDailyLogService
    {
        /* 헤더 단위 CRUD */
        string ReadBlock(DateTime date, string header);
        void WriteBlock(DateTime date, string header, string body);

        /* 아침·저녁 존재 여부 */
        bool MorningExists(DateTime d);
        bool EveningExists(DateTime d);

        /* 주간 조회 */
        IEnumerable<(DateTime Date, bool HasMorning, bool HasEvening)>
            ListWeek(DateTime refDay);
    }
}
