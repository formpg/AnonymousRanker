document.addEventListener('DOMContentLoaded', () => {
    const page = document.getElementById('watch-page');
    if (!page) {
        return;
    }
    const votingToken = page.dataset.votingToken;
    const waitingLabel = document.getElementById('waiting-label');
    const topicLabel = document.getElementById('reveal-topic-label');
    const rangeLabel = document.getElementById('reveal-range-label');
    const rankEl = document.getElementById('reveal-rank');
    const nameEl = document.getElementById('reveal-name');
    const countEl = document.getElementById('reveal-count');
    const revealedList = document.getElementById('revealed-list');

    let knownTopicId = null;
    let knownCount = 0;
    let animating = false;

    function rangeText(state) {
        if (state.announceOrder === 'BOTTOM_UP') {
            return '第' + state.topN + '位 〜 第1位を発表します';
        }
        return '第1位 〜 第' + state.topN + '位を発表します';
    }

    function appendToList(entry) {
        const li = document.createElement('li');
        li.textContent = '第' + entry.rank + '位: ' + entry.name + (RevealUI.formatCount(entry) ? ' (' + RevealUI.formatCount(entry) + ')' : '');
        revealedList.prepend(li);
    }

    function showEntry(entry) {
        rankEl.textContent = '第' + entry.rank + '位';
        nameEl.textContent = entry.name;
        countEl.textContent = RevealUI.formatCount(entry);
    }

    function animateOne(entry, candidatePool) {
        return new Promise(resolve => {
            RevealUI.spin(nameEl, candidatePool, 1200, () => {
                showEntry(entry);
                appendToList(entry);
                resolve();
            });
        });
    }

    async function animateSequentially(entries, candidatePool) {
        animating = true;
        for (const entry of entries) {
            await animateOne(entry, candidatePool);
        }
        animating = false;
    }

    function resetForNewTopic(state) {
        knownTopicId = state.currentTopicId;
        knownCount = 0;
        revealedList.innerHTML = '';
        rankEl.textContent = '';
        nameEl.textContent = '';
        countEl.textContent = '';
        topicLabel.textContent = '発表中のお題: ' + state.topicPrompt;
        rangeLabel.textContent = rangeText(state);
        waitingLabel.style.display = 'none';
        topicLabel.style.display = 'block';
        rangeLabel.style.display = 'block';
    }

    async function poll() {
        if (animating) {
            scheduleNext();
            return;
        }

        try {
            const response = await fetch('/vote/' + votingToken + '/watch/state');
            const state = await response.json();

            if (!state.currentTopicId) {
                scheduleNext();
                return;
            }

            if (state.currentTopicId !== knownTopicId) {
                resetForNewTopic(state);
                // Entries already revealed before this screen was opened are shown instantly, no spin.
                state.revealed.forEach(entry => {
                    showEntry(entry);
                    appendToList(entry);
                });
                knownCount = state.revealed.length;
            } else if (state.revealed.length > knownCount) {
                const newEntries = state.revealed.slice(knownCount);
                knownCount = state.revealed.length;
                await animateSequentially(newEntries, state.candidatePool);
            }

            if (state.complete && knownCount > 0) {
                rankEl.textContent = '発表終了';
            }
        } catch (e) {
            // Network hiccup - just try again on the next poll.
        }

        scheduleNext();
    }

    function scheduleNext() {
        setTimeout(poll, 1000);
    }

    poll();
});
