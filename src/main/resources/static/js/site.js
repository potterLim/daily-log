document.addEventListener("DOMContentLoaded", () => {
    const appHeader = document.querySelector("[data-app-header]");

    const updateHeaderState = () => {
        if (!appHeader) {
            return;
        }

        appHeader.classList.toggle("is-scrolled", window.scrollY > 12);
    };

    window.addEventListener("scroll", updateHeaderState, {passive: true});
    updateHeaderState();

    document.querySelectorAll(".js-autosize").forEach(textAreaElement => {
        const resizeTextArea = () => {
            textAreaElement.style.height = "0px";
            textAreaElement.style.height = `${Math.max(textAreaElement.scrollHeight, 132)}px`;
        };

        textAreaElement.addEventListener("input", resizeTextArea);
        resizeTextArea();
    });

    document.querySelectorAll("[data-print-page]").forEach(printButtonElement => {
        printButtonElement.addEventListener("click", () => {
            window.print();
        });
    });

    document.querySelectorAll("[data-operations-period-form]").forEach(periodFormElement => {
        const trendWeekInputElement = periodFormElement.querySelector("[data-trend-week-input]");

        const submitPeriodForm = () => {
            if (typeof periodFormElement.requestSubmit === "function") {
                periodFormElement.requestSubmit();
                return;
            }

            periodFormElement.submit();
        };

        periodFormElement.querySelectorAll("[data-auto-submit-control]").forEach(controlElement => {
            controlElement.addEventListener("change", submitPeriodForm);
        });

        periodFormElement.querySelectorAll("[data-trend-week-button]").forEach(weekButtonElement => {
            weekButtonElement.addEventListener("click", () => {
                if (trendWeekInputElement) {
                    trendWeekInputElement.value = weekButtonElement.value;
                }

                submitPeriodForm();
            });
        });
    });
});
