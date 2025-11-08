using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;
using DailyLog.Data;

namespace DailyLog.Extensions
{
    public static class ApplicationBuilderExtensions
    {
        public static void MigrateDatabase(this WebApplication app)
        {
            using (IServiceScope scope = app.Services.CreateScope())
            {
                ApplicationDbContext context = scope.ServiceProvider.GetRequiredService<ApplicationDbContext>();
                context.Database.Migrate();
            }
        }
    }
}
