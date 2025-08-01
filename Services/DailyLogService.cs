using System;
using System.Collections.Generic;
using System.IO;
using System.Text;

namespace DailyLog.Services
{
    /// <summary>
    /// .md 로그 파일의 헤더-단위 CRUD 서비스
    /// (사용자별로 logs/{userId} 디렉토리 아래에서만 동작)
    /// </summary>
    public sealed class DailyLogService : IDailyLogService
    {
        private readonly string mRoot;

        public const string HGoals = "## 🚀 Today's Goals";
        public const string HFocus = "## 🎯 Focus Areas";
        public const string HChallenges = "## ⚙️ Challenges & Strategies";

        public const string HEvGoals = "## ✅ Goals Checked";
        public const string HAchieve = "## 🏆 Achievements";
        public const string HImprove = "## 🔧 Improvements";
        public const string HThanks = "## 💛 Gratitude";
        public const string HNotes = "## 📌 Notes for Tomorrow";

        private static readonly string[] sHeaderOrder =
        {
            HGoals, HFocus, HChallenges,
            HEvGoals, HAchieve, HImprove, HThanks, HNotes
        };

        private static readonly string[] sMorningHeaders =
        {
            HGoals, HFocus, HChallenges
        };

        private static readonly string sMorningTemplate =
            $"{HGoals}\r\n\r\n\r\n" +
            $"{HFocus}\r\n\r\n\r\n" +
            $"{HChallenges}\r\n\r\n";

        public DailyLogService(string contentRootPath, string userId)
        {
            mRoot = Path.Combine(contentRootPath, "logs", userId);
        }

        public string ReadBlock(DateTime date, string header)
        {
            string path = getPath(date);
            if (!File.Exists(path))
                return string.Empty;

            Dictionary<string, string> blocks = parseBlocks(File.ReadAllText(path));

            if (!blocks.ContainsKey(header))
                return string.Empty;

            string body = blocks[header];
            if (body.Length == 0)
                return string.Empty;

            string[] lines = body.Split(new[] { "\r\n", "\n" }, StringSplitOptions.RemoveEmptyEntries);

            var result = new StringBuilder();
            foreach (string line in lines)
            {
                string cleanLine = line.StartsWith("- ") ? line.Substring(2).TrimEnd() : line.TrimEnd();
                result.AppendLine(cleanLine);
            }

            return result.ToString().TrimEnd();
        }

        public void WriteBlock(DateTime date, string header, string body)
        {
            if (body is null)
                body = string.Empty;

            string path = getPath(date);

            string fileText = File.Exists(path)
                              ? File.ReadAllText(path)
                              : (isMorningHeader(header) ? sMorningTemplate : string.Empty);

            Dictionary<string, string> blocks = parseBlocks(fileText);

            blocks[header] = normalizeBody(body);

            string rebuilt = buildFile(blocks);

            Directory.CreateDirectory(Path.GetDirectoryName(path) ?? mRoot);
            File.WriteAllText(path, toCrLf(rebuilt), Encoding.UTF8);
        }

        public bool MorningExists(DateTime d)
        {
            return headerExists(d, HGoals);
        }

        public bool EveningExists(DateTime d)
        {
            return headerExists(d, HAchieve);
        }

        public IEnumerable<(DateTime Date, bool HasMorning, bool HasEvening)>
            ListWeek(DateTime refDay)
        {
            DateTime monday = refDay.AddDays(-((int)refDay.DayOfWeek + 6) % 7);

            for (int i = 0; i < 7; ++i)
            {
                DateTime current = monday.AddDays(i);

                string path = getPath(current);
                if (!File.Exists(path))
                {
                    continue;
                }

                yield return (current, MorningExists(current), EveningExists(current));
            }
        }

        private bool headerExists(DateTime d, string header)
        {
            string path = getPath(d);
            if (!File.Exists(path))
                return false;

            foreach (string line in File.ReadLines(path))
            {
                if (line.Trim().Equals(header, StringComparison.Ordinal))
                    return true;
            }

            return false;
        }

        private static Dictionary<string, string> parseBlocks(string text)
        {
            Dictionary<string, StringBuilder> temp = new Dictionary<string, StringBuilder>();
            string[] lines = toCrLf(text).Split(new[] { "\r\n" }, StringSplitOptions.None);

            string currentHeader = null;

            foreach (string line in lines)
            {
                if (line.StartsWith("## "))
                {
                    currentHeader = line.TrimEnd();
                    if (!temp.ContainsKey(currentHeader))
                    {
                        temp[currentHeader] = new StringBuilder();
                    }
                }
                else if (currentHeader != null)
                {
                    temp[currentHeader].AppendLine(line);
                }
            }

            Dictionary<string, string> result = new Dictionary<string, string>();
            foreach (var pair in temp)
            {
                result[pair.Key] = pair.Value.ToString().TrimEnd();
            }

            return result;
        }

        private static string buildFile(Dictionary<string, string> blocks)
        {
            StringBuilder sb = new StringBuilder();

            bool isFirstBlock = true;

            foreach (string header in sHeaderOrder)
            {
                if (!blocks.ContainsKey(header))
                    continue;

                string body = blocks[header].Trim();

                if (!isFirstBlock)
                    sb.AppendLine();

                sb.AppendLine(header);

                if (body.Length > 0)
                {
                    string[] lines = body.Split(new[] { "\r\n", "\n" }, StringSplitOptions.RemoveEmptyEntries);

                    foreach (string line in lines)
                    {
                        string cleanLine = line.TrimStart('-', ' ').Trim();
                        sb.AppendLine($"- {cleanLine}");
                    }
                }

                isFirstBlock = false;
            }

            return sb.ToString().TrimEnd();
        }

        private static string normalizeBody(string raw)
        {
            string text = toCrLf(raw);
            if (text.Length == 0)
                return string.Empty;

            var sb = new StringBuilder();
            string[] lines = text.Split(new[] { "\r\n" }, StringSplitOptions.None);

            foreach (string line in lines)
            {
                string trimmed = line.Trim();
                if (trimmed.Length == 0)
                    continue;

                sb.AppendLine(trimmed);
            }

            return sb.ToString().TrimEnd();
        }

        private static bool isMorningHeader(string header)
        {
            foreach (string h in sMorningHeaders)
            {
                if (h.Equals(header, StringComparison.Ordinal))
                    return true;
            }

            return false;
        }

        private string getPath(DateTime d)
        {
            string folder = Path.Combine(mRoot, $"{d:yyyy_MM}_Week{((d.Day - 1) / 7) + 1}");
            return Path.Combine(folder, $"{d:yyyy-MM-dd}.md");
        }

        private static string toCrLf(string txt)
        {
            if (txt is null)
                return string.Empty;

            return txt.Replace("\r\n", "\n").Replace("\r", "\n").Replace("\n", "\r\n");
        }
    }
}
