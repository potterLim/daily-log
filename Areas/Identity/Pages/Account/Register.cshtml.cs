using System.ComponentModel.DataAnnotations;
using System.Diagnostics;
using System.Text.RegularExpressions;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace DailyLog.Areas.Identity.Pages.Account
{
    public class RegisterModel : PageModel
    {
        private readonly UserManager<IdentityUser> mUserManager;
        private readonly SignInManager<IdentityUser> mSignInManager;

        public RegisterModel(UserManager<IdentityUser> userManager, SignInManager<IdentityUser> signInManager)
        {
            Debug.Assert(userManager != null, "userManager는 null이 아니어야 합니다.");
            Debug.Assert(signInManager != null, "signInManager는 null이 아니어야 합니다.");

            mUserManager = userManager;
            mSignInManager = signInManager;
        }

        [BindProperty]
        public InputModel Input { get; set; }

        public string ReturnUrl { get; set; }

        public class InputModel
        {
            [Required(ErrorMessage = "아이디를 입력해주세요.")]
            [Display(Name = "아이디")]
            public string UserName { get; set; }

            [Required(ErrorMessage = "비밀번호를 입력해주세요.")]
            [StringLength(100, ErrorMessage = "비밀번호는 최소 {2}자 이상이어야 합니다.", MinimumLength = 4)]
            [DataType(DataType.Password)]
            [Display(Name = "비밀번호")]
            public string Password { get; set; }

            [Required(ErrorMessage = "비밀번호 확인을 입력해주세요.")]
            [DataType(DataType.Password)]
            [Display(Name = "비밀번호 확인")]
            [Compare("Password", ErrorMessage = "비밀번호와 비밀번호 확인이 일치하지 않습니다.")]
            public string ConfirmPassword { get; set; }
        }

        public void OnGet(string returnUrl = null)
        {
            ReturnUrl = returnUrl;
        }

        public async Task<IActionResult> OnPostAsync(string returnUrl = null)
        {
            returnUrl ??= Url.Content("~/");

            if (!ModelState.IsValid)
            {
                return Page();
            }

            IdentityUser user = new IdentityUser
            {
                UserName = Input.UserName
            };

            IdentityResult result = await mUserManager.CreateAsync(user, Input.Password);

            if (result.Succeeded)
            {
                await mSignInManager.SignInAsync(user, isPersistent: false);
                return LocalRedirect(returnUrl);
            }

            foreach (IdentityError error in result.Errors)
            {
                string errorMessage;

                if (error.Code == "DuplicateUserName")
                {
                    errorMessage = "이미 사용 중인 아이디입니다.";
                }
                else if (error.Code == "PasswordTooShort")
                {
                    // 영어 문장에서 숫자만 추출 (예: "Passwords must be at least 6 characters long.")
                    Match match = Regex.Match(error.Description, @"\d+");
                    string length = match.Success ? match.Value : "?";
                    errorMessage = $"비밀번호는 최소 {length}자 이상이어야 합니다.";
                }
                else if (error.Description.Contains("Passwords must have at least one non alphanumeric character"))
                {
                    errorMessage = "비밀번호에 특수문자가 하나 이상 포함되어야 합니다.";
                }
                else if (error.Description.Contains("Passwords must have at least one digit"))
                {
                    errorMessage = "비밀번호에 숫자가 하나 이상 포함되어야 합니다.";
                }
                else if (error.Description.Contains("Passwords must have at least one uppercase"))
                {
                    errorMessage = "비밀번호에 대문자가 하나 이상 포함되어야 합니다.";
                }
                else if (error.Description.Contains("Passwords must have at least one lowercase"))
                {
                    errorMessage = "비밀번호에 소문자가 하나 이상 포함되어야 합니다.";
                }
                else
                {
                    errorMessage = "알 수 없는 오류가 발생했습니다. 다시 시도해주세요.";
                }

                ModelState.AddModelError(string.Empty, errorMessage);
            }

            return Page();
        }
    }
}
