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
    let revealedEntries = [];

    function rangeText(state) {
        if (state.announceOrder === 'BOTTOM_UP') {
            return '第' + state.topN + '位 〜 第1位を発表します';
        }
        return '第1位 〜 第' + state.topN + '位を発表します';
    }

    function addToList(entry) {
        revealedEntries.push(entry);
        revealedList.innerHTML = '';
        // Always shown best-rank-first (1位 at the top), regardless of
        // which order (top-down or bottom-up) they were announced in.
        revealedEntries.slice().sort((a, b) => a.rank - b.rank).forEach(e => {
            const li = document.createElement('li');
            li.textContent = '第' + e.rank + '位: ' + e.name + (RevealUI.formatCount(e) ? ' (' + RevealUI.formatCount(e) + ')' : '');
            revealedList.appendChild(li);
        });
    }

    function showTeaser(rank) {
        rankEl.textContent = '第' + rank + '位';
        rankEl.classList.add('rank-teaser');
        nameEl.textContent = '？';
        countEl.textContent = '';
    }

    function showEntry(entry) {
        rankEl.textContent = '第' + entry.rank + '位';
        rankEl.classList.remove('rank-teaser');
        nameEl.textContent = entry.name;
        countEl.textContent = RevealUI.formatCount(entry);
    }

    function wait(ms) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    function animateOne(entry, candidatePool) {
        return new Promise(resolve => {
            // The entry (and its real rank) is already known here - it came
            // back from the poll - so the teaser can show the actual rank
            // rather than guessing one from position (which ties can throw
            // off, e.g. a multi-way tie for 2nd would make a purely
            // positional guess drift below 1).
            showTeaser(entry.rank);
            wait(900).then(() => {
                RevealUI.spinReel(nameEl, candidatePool, entry.name, 10000, () => {
                    showEntry(entry);
                    addToList(entry);
                    resolve();
                });
            });
        });
    }

    async function animateSequentially(entries, state) {
        animating = true;
        for (const entry of entries) {
            await animateOne(entry, state.candidatePool);
        }
        animating = false;
    }

    function resetForNewTopic(state) {
        knownTopicId = state.currentTopicId;
        knownCount = 0;
        revealedEntries = [];
        revealedList.innerHTML = '';
        rankEl.textContent = '';
        nameEl.textContent = '';
        countEl.textContent = '';
        topicLabel.textContent = state.topicPrompt;
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
                    addToList(entry);
                });
                knownCount = state.revealed.length;
            } else if (state.revealed.length > knownCount) {
                const newEntries = state.revealed.slice(knownCount);
                knownCount = state.revealed.length;
                await animateSequentially(newEntries, state);
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
