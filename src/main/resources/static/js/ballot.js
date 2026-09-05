document.addEventListener('DOMContentLoaded', () => {
    const form = document.getElementById('ballot-form');
    if (!form) {
        return;
    }
    const max = parseInt(form.dataset.max, 10) || 1;
    const checkboxes = Array.from(form.querySelectorAll('input[type="checkbox"]'));

    function refresh() {
        const checkedCount = checkboxes.filter(cb => cb.checked).length;
        checkboxes.forEach(cb => {
            cb.disabled = !cb.checked && checkedCount >= max;
        });
    }

    checkboxes.forEach(cb => cb.addEventListener('change', refresh));
    refresh();
});
