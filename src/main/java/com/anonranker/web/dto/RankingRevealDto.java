package com.anonranker.web.dto;

import com.anonranker.domain.AnnounceOrder;
import com.anonranker.domain.AnnouncePacing;

import java.util.List;

public class RankingRevealDto {

    private final String topicPrompt;
    private final int topN;
    private final AnnounceOrder announceOrder;
    private final AnnouncePacing pacing;
    private final int autoIntervalMs;
    private final List<RankEntryDto> ranks;
    private final List<String> candidatePool;

    public RankingRevealDto(String topicPrompt, int topN, AnnounceOrder announceOrder, AnnouncePacing pacing,
                             int autoIntervalMs, List<RankEntryDto> ranks, List<String> candidatePool) {
        this.topicPrompt = topicPrompt;
        this.topN = topN;
        this.announceOrder = announceOrder;
        this.pacing = pacing;
        this.autoIntervalMs = autoIntervalMs;
        this.ranks = ranks;
        this.candidatePool = candidatePool;
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

    public List<RankEntryDto> getRanks() {
        return ranks;
    }

    public List<String> getCandidatePool() {
        return candidatePool;
    }
}
