document.addEventListener('DOMContentLoaded', () => {
    const page = document.getElementById('announce-page');
    if (!page) {
        return;
    }
    const adminToken = page.dataset.adminToken;
    const revealCard = document.getElementById('reveal-card');
    const rankEl = document.getElementById('reveal-rank');
    const nameEl = document.getElementById('reveal-name');
    const countEl = document.getElementById('reveal-count');
    const nextBtn = document.getElementById('next-btn');
    const revealedList = document.getElementById('revealed-list');
    const topicButtons = document.querySelectorAll('.topic-btn');

    let sequence = [];
    let candidatePool = [];
    let pacing = 'MANUAL';
    let autoIntervalMs = 3000;
    let index = 0;

    function spin(onSettle) {
        let elapsed = 0;
        let delay = 60;
        const spinDuration = 1200;

        function tick() {
            if (candidatePool.length > 0) {
                nameEl.textContent = candidatePool[Math.floor(Math.random() * candidatePool.length)];
            }
            elapsed += delay;
            delay = delay * 1.15;
            if (elapsed < spinDuration) {
                setTimeout(tick, delay);
            } else {
                onSettle();
            }
        }

        tick();
    }

    function revealNext() {
        if (index >= sequence.length) {
            rankEl.textContent = '発表終了';
            nameEl.textContent = '';
            countEl.textContent = '';
            nextBtn.style.display = 'none';
            return;
        }

        nextBtn.disabled = true;
        spin(() => {
            const entry = sequence[index];
            rankEl.textContent = entry.rank + '位';
            nameEl.textContent = entry.name;
            countEl.textContent = (entry.count !== undefined && entry.count !== null) ? entry.count + '票' : '';

            const li = document.createElement('li');
            const countText = (entry.count !== undefined && entry.count !== null) ? ' (' + entry.count + '票)' : '';
            li.textContent = entry.rank + '位: ' + entry.name + countText;
            revealedList.prepend(li);

            index++;
            if (index >= sequence.length) {
                nextBtn.style.display = 'none';
            } else if (pacing === 'AUTO') {
                setTimeout(revealNext, autoIntervalMs);
            } else {
                nextBtn.style.display = 'inline-block';
                nextBtn.disabled = false;
            }
        });
    }

    async function startAnnouncement(topicId) {
        const response = await fetch('/s/' + adminToken + '/announce/' + topicId + '/data');
        if (!response.ok) {
            window.alert('発表データの取得に失敗しました');
            return;
        }
        const data = await response.json();

        candidatePool = data.candidatePool || [];
        pacing = data.pacing;
        autoIntervalMs = data.autoIntervalMs;
        sequence = data.announceOrder === 'BOTTOM_UP' ? data.ranks.slice().reverse() : data.ranks.slice();
        index = 0;

        revealedList.innerHTML = '';
        revealCard.style.display = 'block';
        nextBtn.style.display = 'none';
        revealNext();
    }

    topicButtons.forEach(btn => {
        btn.addEventListener('click', () => startAnnouncement(btn.dataset.topicId));
    });
    nextBtn.addEventListener('click', revealNext);
});
