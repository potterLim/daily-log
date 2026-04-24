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
});
