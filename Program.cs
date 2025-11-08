using System;
using Microsoft.AspNetCore.Identity;
using Microsoft.EntityFrameworkCore;
using DailyLog.Data;

namespace DailyLog
{
    public static class Program
    {
        public static void Main(string[] args)
        {
            WebApplicationBuilder builder = WebApplication.CreateBuilder(args);

            ConfigureServices(builder);

            WebApplication app = builder.Build();

            ConfigurePipeline(app);

            app.Run();
        }

        /* ──────────────────────────── 서비스 등록 ──────────────────────────── */
        private static void ConfigureServices(WebApplicationBuilder builder)
        {
            string? connectionString = builder.Configuration["DATABASE_URL"]
                ?? builder.Configuration.GetConnectionString("DefaultConnection")
                ?? throw new InvalidOperationException("No connection string found.");

            builder.Services.AddDbContext<ApplicationDbContext>(options =>
                options.UseNpgsql(connectionString));

            builder.Services.AddDatabaseDeveloperPageExceptionFilter();

            builder.Services.Configure<IdentityOptions>(options =>
            {
                options.Password.RequireDigit = false;
                options.Password.RequiredLength = 4;
                options.Password.RequireNonAlphanumeric = false;
                options.Password.RequireUppercase = false;
                options.Password.RequireLowercase = false;
                options.User.RequireUniqueEmail = false;
            });

            builder.Services.AddDefaultIdentity<IdentityUser>(options =>
                    options.SignIn.RequireConfirmedAccount = false)
                .AddEntityFrameworkStores<ApplicationDbContext>();

            /* MVC + Razor */
            builder.Services.AddControllersWithViews();
            builder.Services.AddRazorPages();

            /* DailyLogService는 컨트롤러에서 직접 생성하도록 변경 (Singleton 제거) */
        }

        /* ──────────────────────────── HTTP 파이프라인 ──────────────────────────── */
        private static void ConfigurePipeline(WebApplication app)
        {
            if (app.Environment.IsDevelopment())
            {
                app.UseMigrationsEndPoint();
            }
            else
            {
                app.UseExceptionHandler("/Home/Error");
                app.UseHsts();
            }

            app.UseHttpsRedirection();
            app.UseStaticFiles();

            app.UseRouting();

            app.UseAuthentication();
            app.UseAuthorization();

            app.MapControllerRoute(
                name: "default",
                pattern: "{controller=Home}/{action=Index}/{id?}");

            app.MapRazorPages();

            app.UseStatusCodePages(context =>
            {
                if (context.HttpContext.Response.StatusCode == 404)
                {
                    context.HttpContext.Response.Redirect("/");
                }
                return Task.CompletedTask;
            });
        }
    }
}
