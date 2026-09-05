package com.anonranker.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * Marks that a member has submitted their ballot for a topic. Kept separate
 * from {@link Vote} so "has this member already voted?" can be answered with
 * one cheap existence check, without ever joining through candidate choices.
 */
@Entity
@Table(
        name = "vote_submissions",
        uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "voter_member_id"})
)
public class VoteSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Topic topic;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "voter_member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member voterMember;

    @Column(nullable = false, updatable = false)
    private Instant submittedAt;

    protected VoteSubmission() {
    }

    public VoteSubmission(Topic topic, Member voterMember) {
        this.topic = topic;
        this.voterMember = voterMember;
        this.submittedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Topic getTopic() {
        return topic;
    }

    public Member getVoterMember() {
        return voterMember;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }
}
