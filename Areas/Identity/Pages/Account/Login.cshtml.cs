using System.ComponentModel.DataAnnotations;
using System.Diagnostics;
using System.Threading.Tasks;
using Microsoft.AspNetCore.Identity;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.Mvc.RazorPages;

namespace DailyLog.Areas.Identity.Pages.Account
{
    public class LoginModel : PageModel
    {
        private readonly SignInManager<IdentityUser> mSignInManager;
        private readonly UserManager<IdentityUser> mUserManager;

        public LoginModel(SignInManager<IdentityUser> signInManager, UserManager<IdentityUser> userManager)
        {
            Debug.Assert(signInManager != null, "signInManager는 null이 아니어야 합니다.");
            Debug.Assert(userManager != null, "userManager는 null이 아니어야 합니다.");

            mSignInManager = signInManager;
            mUserManager = userManager;
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
            [DataType(DataType.Password)]
            [Display(Name = "비밀번호")]
            public string Password { get; set; }

            [Display(Name = "자동 로그인")]
            public bool RememberMe { get; set; }
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

            IdentityUser user = await mUserManager.FindByNameAsync(Input.UserName);

            if (user == null)
            {
                ModelState.AddModelError(string.Empty, "존재하지 않는 아이디입니다.");
                return Page();
            }

            Microsoft.AspNetCore.Identity.SignInResult result = await mSignInManager.PasswordSignInAsync(
                Input.UserName,
                Input.Password,
                Input.RememberMe,
                lockoutOnFailure: true);

            if (result.Succeeded)
            {
                return LocalRedirect(returnUrl);
            }

            if (result.IsLockedOut)
            {
                ModelState.AddModelError(string.Empty, "계정이 잠겼습니다. 잠시 후 다시 시도해주세요.");
            }
            else if (result.IsNotAllowed)
            {
                ModelState.AddModelError(string.Empty, "이메일 인증이 필요합니다.");
            }
            else
            {
                ModelState.AddModelError(string.Empty, "비밀번호가 올바르지 않습니다.");
            }

            return Page();
        }
    }
}
