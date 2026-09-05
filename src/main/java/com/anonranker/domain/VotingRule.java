package com.anonranker.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "voting_rules")
public class VotingRule {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "session_id")
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Session session;

    @Column(nullable = false)
    private int votesPerTopic = 1;

    @Column(nullable = false)
    private boolean allowSelfVote = false;

    @Column(nullable = false)
    private int topN = 3;

    @Column(nullable = false)
    private boolean revealCounts = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnounceOrder announceOrder = AnnounceOrder.BOTTOM_UP;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AnnouncePacing pacing = AnnouncePacing.MANUAL;

    @Column(nullable = false)
    private int autoIntervalMs = 3000;

    protected VotingRule() {
    }

    public VotingRule(Session session) {
        this.session = session;
    }

    public Long getId() {
        return id;
    }

    public Session getSession() {
        return session;
    }

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
