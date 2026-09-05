document.addEventListener('DOMContentLoaded', () => {
    const page = document.getElementById('announce-page');
    if (!page) {
        return;
    }
    const adminToken = page.dataset.adminToken;
    const revealCard = document.getElementById('reveal-card');
    const topicLabel = document.getElementById('reveal-topic-label');
    const rangeLabel = document.getElementById('reveal-range-label');
    const rankEl = document.getElementById('reveal-rank');
    const nameEl = document.getElementById('reveal-name');
    const countEl = document.getElementById('reveal-count');
    const nextBtn = document.getElementById('next-btn');
    const revealedList = document.getElementById('revealed-list');
    const topicButtons = document.querySelectorAll('.topic-btn');

    let currentState = null;
    let currentTopicId = null;

    function rangeText(state) {
        if (state.announceOrder === 'BOTTOM_UP') {
            return '第' + state.topN + '位 〜 第1位を発表します';
        }
        return '第1位 〜 第' + state.topN + '位を発表します';
    }

    function renderRevealed(state) {
        revealedList.innerHTML = '';
        // Always shown best-rank-first (1位 at the top), regardless of
        // which order (top-down or bottom-up) they were announced in.
        state.revealed.slice().sort((a, b) => a.rank - b.rank).forEach(entry => {
            const li = document.createElement('li');
            li.textContent = '第' + entry.rank + '位: ' + entry.name + (RevealUI.formatCount(entry) ? ' (' + RevealUI.formatCount(entry) + ')' : '');
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

    async function revealStep() {
        const beforeCount = currentState.revealed.length;
        nextBtn.disabled = true;

        showTeaser(RevealUI.expectedNextRank(currentState));
        await wait(900);

        const response = await fetch('/s/' + adminToken + '/announce/' + currentTopicId + '/reveal-next', { method: 'POST' });
        currentState = await response.json();

        if (currentState.revealed.length === beforeCount) {
            // Already complete - nothing new was revealed.
            rankEl.textContent = '発表終了';
            rankEl.classList.remove('rank-teaser');
            nameEl.textContent = '';
            countEl.textContent = '';
            nextBtn.style.display = 'none';
            return;
        }

        const entry = currentState.revealed[currentState.revealed.length - 1];
        RevealUI.spinReel(nameEl, currentState.candidatePool, entry.name, 3200, () => {
            showEntry(entry);
            // Only added to the "revealed so far" list once the reel has
            // actually settled, so it can't spoil the result early.
            renderRevealed(currentState);

            if (currentState.complete) {
                nextBtn.style.display = 'none';
            } else if (currentState.pacing === 'AUTO') {
                setTimeout(revealStep, currentState.autoIntervalMs);
            } else {
                nextBtn.style.display = 'inline-block';
                nextBtn.disabled = false;
            }
        });
    }

    async function startAnnouncement(topicId) {
        currentTopicId = topicId;
        const response = await fetch('/s/' + adminToken + '/announce/' + topicId + '/start', { method: 'POST' });
        currentState = await response.json();

        topicLabel.textContent = currentState.topicPrompt;
        rangeLabel.textContent = rangeText(currentState);
        revealedList.innerHTML = '';
        revealCard.style.display = 'block';
        nextBtn.style.display = 'none';

        revealStep();
    }

    topicButtons.forEach(btn => {
        btn.addEventListener('click', () => startAnnouncement(btn.dataset.topicId));
    });
    nextBtn.addEventListener('click', revealStep);
});
