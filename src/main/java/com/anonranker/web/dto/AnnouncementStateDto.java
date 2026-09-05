package com.anonranker.web.dto;

import com.anonranker.domain.AnnounceOrder;
import com.anonranker.domain.AnnouncePacing;

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

    public AnnouncementStateDto(Long currentTopicId, String topicPrompt, int topN, AnnounceOrder announceOrder,
                                 AnnouncePacing pacing, int autoIntervalMs, List<RankEntryDto> revealed,
                                 boolean complete, List<String> candidatePool) {
        this.currentTopicId = currentTopicId;
        this.topicPrompt = topicPrompt;
        this.topN = topN;
        this.announceOrder = announceOrder;
        this.pacing = pacing;
        this.autoIntervalMs = autoIntervalMs;
        this.revealed = revealed;
        this.complete = complete;
        this.candidatePool = candidatePool;
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
}
