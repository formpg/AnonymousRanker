document.addEventListener('DOMContentLoaded', () => {
    const selects = Array.from(document.querySelectorAll('select.ballot-select'));
    if (selects.length === 0) {
        return;
    }

    const groups = new Map();
    selects.forEach(select => {
        const topicId = select.dataset.topic;
        if (!groups.has(topicId)) {
            groups.set(topicId, []);
        }
        groups.get(topicId).push(select);
    });

    function refreshGroup(groupSelects) {
        const chosen = groupSelects
            .map(s => s.value)
            .filter(v => v !== '');

        groupSelects.forEach(select => {
            Array.from(select.options).forEach(option => {
                if (option.value === '') {
                    return;
                }
                const chosenElsewhere = chosen.includes(option.value) && option.value !== select.value;
                option.disabled = chosenElsewhere;
            });
        });
    }

    groups.forEach(groupSelects => {
        groupSelects.forEach(select => {
            select.addEventListener('change', () => refreshGroup(groupSelects));
        });
        refreshGroup(groupSelects);
    });
});
