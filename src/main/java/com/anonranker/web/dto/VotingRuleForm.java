package com.anonranker.web.dto;

import com.anonranker.domain.AnnounceOrder;
import com.anonranker.domain.AnnouncePacing;
import jakarta.validation.constraints.Min;

public class VotingRuleForm {

    @Min(value = 1, message = "1以上を指定してください")
    private int votesPerTopic;

    private boolean allowSelfVote;

    @Min(value = 1, message = "1以上を指定してください")
    private int topN;

    private boolean revealCounts;

    private AnnounceOrder announceOrder;

    private AnnouncePacing pacing;

    @Min(value = 500, message = "500ms以上を指定してください")
    private int autoIntervalMs;

    public int getVotesPerTopic() {
        return votesPerTopic;
    }

    public void setVotesPerTopic(int votesPerTopic) {
        this.votesPerTopic = votesPerTopic;
    }

    public boolean isAllowSelfVote() {
        return allowSelfVote;
    }

    public void setAllowSelfVote(boolean allowSelfVote) {
        this.allowSelfVote = allowSelfVote;
    }

    public int getTopN() {
        return topN;
    }

    public void setTopN(int topN) {
        this.topN = topN;
    }

    public boolean isRevealCounts() {
        return revealCounts;
    }

    public void setRevealCounts(boolean revealCounts) {
        this.revealCounts = revealCounts;
    }

    public AnnounceOrder getAnnounceOrder() {
        return announceOrder;
    }

    public void setAnnounceOrder(AnnounceOrder announceOrder) {
        this.announceOrder = announceOrder;
    }

    public AnnouncePacing getPacing() {
        return pacing;
    }

    public void setPacing(AnnouncePacing pacing) {
        this.pacing = pacing;
    }

    public int getAutoIntervalMs() {
        return autoIntervalMs;
    }

    public void setAutoIntervalMs(int autoIntervalMs) {
        this.autoIntervalMs = autoIntervalMs;
    }
}
