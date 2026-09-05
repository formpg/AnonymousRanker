window.RevealUI = {
    /**
     * Rapidly cycles random names in nameEl (slot-machine effect), slowing
     * down over spinDurationMs, then invokes onSettle. Shared by the
     * organizer's announce screen and every voter's read-only watch screen
     * so both play the same visual effect for each newly revealed rank.
     */
    spin(nameEl, candidatePool, spinDurationMs, onSettle) {
        let elapsed = 0;
        let delay = 60;

        function tick() {
            if (candidatePool.length > 0) {
                nameEl.textContent = candidatePool[Math.floor(Math.random() * candidatePool.length)];
            }
            elapsed += delay;
            delay = delay * 1.15;
            if (elapsed < spinDurationMs) {
                setTimeout(tick, delay);
            } else {
                onSettle();
            }
        }

        tick();
    },

    formatCount(entry) {
        return (entry.count !== undefined && entry.count !== null) ? entry.count + '票' : '';
    }
};
