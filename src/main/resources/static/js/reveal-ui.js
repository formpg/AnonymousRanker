window.RevealUI = {
    /**
     * Plays a vertical slot-reel effect inside containerEl: a strip of names
     * flows downward (entering at the top, exiting at the bottom), starting
     * fast and decelerating to a stop on finalLabel. Shared by the
     * organizer's announce screen and every voter's read-only watch screen
     * so both play the same visual effect for each newly revealed rank.
     *
     * containerEl's own content is replaced with the reel markup for the
     * duration of the spin.
     */
    spinReel(containerEl, candidatePool, finalLabel, spinDurationMs, onSettle) {
        const spinCount = 22;
        const names = [finalLabel];
        for (let i = 0; i < spinCount; i++) {
            names.push(candidatePool.length > 0
                ? candidatePool[Math.floor(Math.random() * candidatePool.length)]
                : finalLabel);
        }

        containerEl.innerHTML = '';
        const viewport = document.createElement('div');
        viewport.className = 'reel-viewport';
        const strip = document.createElement('div');
        strip.className = 'reel-strip';
        names.forEach(name => {
            const item = document.createElement('div');
            item.className = 'reel-item';
            item.textContent = name;
            strip.appendChild(item);
        });
        viewport.appendChild(strip);
        containerEl.appendChild(viewport);

        const itemHeight = strip.firstChild.getBoundingClientRect().height;
        const lastIndex = names.length - 1;

        strip.style.transition = 'none';
        strip.style.transform = 'translateY(' + (-lastIndex * itemHeight) + 'px)';

        // Force a reflow so the browser registers the starting position
        // before the transition to the final position is applied.
        // eslint-disable-next-line no-unused-expressions
        strip.getBoundingClientRect();

        strip.style.transition = 'transform ' + spinDurationMs + 'ms cubic-bezier(0.1, 0.7, 0.15, 1)';
        strip.style.transform = 'translateY(0px)';

        strip.addEventListener('transitionend', function handler() {
            strip.removeEventListener('transitionend', handler);
            onSettle();
        }, { once: true });
    },

    formatCount(entry) {
        return (entry.count !== undefined && entry.count !== null) ? entry.count + '票' : '';
    },

    /**
     * The rank about to be revealed, guessed from position alone (assumes no
     * ties) so it can be shown big BEFORE the actual result is known -
     * the server never sends not-yet-revealed ranks ahead of time, since
     * that would spoil the surprise for anyone watching.
     */
    expectedNextRank(state) {
        if (state.announceOrder === 'BOTTOM_UP') {
            return state.topN - state.revealed.length;
        }
        return state.revealed.length + 1;
    }
};
