document.addEventListener('DOMContentLoaded', () => {
    const copyBtn = document.getElementById('copy-btn');
    const target = document.getElementById('voting-url');
    if (!copyBtn || !target) {
        return;
    }
    copyBtn.addEventListener('click', async () => {
        try {
            await navigator.clipboard.writeText(target.textContent);
            const original = copyBtn.textContent;
            copyBtn.textContent = 'コピーしました！';
            setTimeout(() => { copyBtn.textContent = original; }, 1500);
        } catch (e) {
            window.prompt('このURLをコピーしてください', target.textContent);
        }
    });
});
