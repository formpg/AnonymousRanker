package com.anonranker.web.dto;

import com.anonranker.domain.AnnounceOrder;
import com.anonranker.domain.AnnouncePacing;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Server-authoritative snapshot of a live announcement, served to both the
 * organizer's admin screen and any voter's read-only "watch" screen so both
 * observe the exact same reveal progress.
 */
public class AnnouncementStateDto {

    private final Long currentTopicId;
    private final String topicPrompt;
    private final int topN;
    private final AnnounceOrder announceOrder;
    private final AnnouncePacing pacing;
    private final int autoIntervalMs;
    private final List<RankEntryDto> revealed;
    private final boolean complete;
    private final List<String> candidatePool;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Integer nextRank;

    public AnnouncementStateDto(Long currentTopicId, String topicPrompt, int topN, AnnounceOrder announceOrder,
                                 AnnouncePacing pacing, int autoIntervalMs, List<RankEntryDto> revealed,
                                 boolean complete, List<String> candidatePool, Integer nextRank) {
        this.currentTopicId = currentTopicId;
        this.topicPrompt = topicPrompt;
        this.topN = topN;
        this.announceOrder = announceOrder;
        this.pacing = pacing;
        this.autoIntervalMs = autoIntervalMs;
        this.revealed = revealed;
        this.complete = complete;
        this.candidatePool = candidatePool;
        this.nextRank = nextRank;
    }

    public Long getCurrentTopicId() {
        return currentTopicId;
    }

    public String getTopicPrompt() {
        return topicPrompt;
    }

    public int getTopN() {
        return topN;
    }

    public AnnounceOrder getAnnounceOrder() {
        return announceOrder;
    }

    public AnnouncePacing getPacing() {
        return pacing;
    }

    public int getAutoIntervalMs() {
        return autoIntervalMs;
    }

    public List<RankEntryDto> getRevealed() {
        return revealed;
    }

    public boolean isComplete() {
        return complete;
    }

    public List<String> getCandidatePool() {
        return candidatePool;
    }

    /**
     * The actual rank of the next entry to be revealed, or {@code null} if
     * nothing remains. Computed server-side (rather than guessed on the
     * client from position alone) because ties mean position and rank
     * number can diverge - e.g. a 3-way tie for 2nd place means three
     * consecutive reveals all carry rank 2, which a purely positional guess
     * would get wrong (and could even guess a rank below 1).
     */
    public Integer getNextRank() {
        return nextRank;
    }
}
