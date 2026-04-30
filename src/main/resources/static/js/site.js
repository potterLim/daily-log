document.addEventListener("DOMContentLoaded", () => {
    const appHeader = document.querySelector("[data-app-header]");
    const externalBrowserDialogElement = document.querySelector("[data-external-browser-dialog]");

    const updateHeaderState = () => {
        if (!appHeader) {
            return;
        }

        appHeader.classList.toggle("is-scrolled", window.scrollY > 12);
    };

    window.addEventListener("scroll", updateHeaderState, {passive: true});
    updateHeaderState();

    const isEmbeddedBrowser = () => {
        const userAgent = navigator.userAgent.toLowerCase();
        return userAgent.includes("kakaotalk")
            || userAgent.includes("instagram")
            || userAgent.includes("fbav")
            || userAgent.includes("fban")
            || userAgent.includes("line/")
            || userAgent.includes("naver")
            || userAgent.includes("daumapps")
            || userAgent.includes("; wv)");
    };

    const copyTextToClipboard = async text => {
        if (navigator.clipboard && window.isSecureContext) {
            await navigator.clipboard.writeText(text);
            return;
        }

        const temporaryTextAreaElement = document.createElement("textarea");
        temporaryTextAreaElement.value = text;
        temporaryTextAreaElement.setAttribute("readonly", "");
        temporaryTextAreaElement.style.position = "fixed";
        temporaryTextAreaElement.style.opacity = "0";
        document.body.appendChild(temporaryTextAreaElement);
        temporaryTextAreaElement.select();
        document.execCommand("copy");
        temporaryTextAreaElement.remove();
    };

    const showExternalBrowserDialog = copyUrl => {
        if (!externalBrowserDialogElement) {
            return;
        }

        externalBrowserDialogElement.dataset.copyUrl = copyUrl || window.location.href;
        externalBrowserDialogElement.hidden = false;
        externalBrowserDialogElement.classList.add("is-open");
        document.body.classList.add("external-browser-dialog-open");

        const closeButtonElement = externalBrowserDialogElement.querySelector("button[data-external-browser-close]");
        if (closeButtonElement) {
            closeButtonElement.focus();
        }
    };

    const closeExternalBrowserDialog = () => {
        if (!externalBrowserDialogElement) {
            return;
        }

        externalBrowserDialogElement.classList.remove("is-open");
        externalBrowserDialogElement.hidden = true;
        document.body.classList.remove("external-browser-dialog-open");
    };

    document.querySelectorAll("[data-google-oauth-link]").forEach(googleOAuthLinkElement => {
        googleOAuthLinkElement.addEventListener("click", event => {
            if (!isEmbeddedBrowser()) {
                return;
            }

            event.preventDefault();
            showExternalBrowserDialog(window.location.href);
        });
    });

    document.querySelectorAll("[data-external-browser-close]").forEach(closeButtonElement => {
        closeButtonElement.addEventListener("click", closeExternalBrowserDialog);
    });

    document.addEventListener("keydown", event => {
        if (event.key === "Escape") {
            closeExternalBrowserDialog();
        }
    });

    document.querySelectorAll("[data-copy-url-button]").forEach(copyButtonElement => {
        copyButtonElement.addEventListener("click", async () => {
            const copyUrl = copyButtonElement.dataset.copyUrl
                || externalBrowserDialogElement?.dataset.copyUrl
                || window.location.href;
            const originalText = copyButtonElement.textContent;

            try {
                await copyTextToClipboard(copyUrl);
                copyButtonElement.textContent = "복사되었습니다";
                window.setTimeout(() => {
                    copyButtonElement.textContent = originalText;
                }, 1600);
            } catch (error) {
                copyButtonElement.textContent = "주소를 복사할 수 없습니다";
                window.setTimeout(() => {
                    copyButtonElement.textContent = originalText;
                }, 1800);
            }
        });
    });

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
