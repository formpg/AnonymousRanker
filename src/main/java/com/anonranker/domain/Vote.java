package com.anonranker.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

/**
 * A single cast vote. Never expose this entity (or a DTO built from it that
 * keeps the voter/candidate pair together) through any view, JSON response,
 * or export - only aggregated counts derived from it may be surfaced.
 */
@Entity
@Table(
        name = "votes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"topic_id", "voter_member_id", "candidate_member_id"})
)
public class Vote {

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

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_member_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Member candidateMember;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Vote() {
    }

    public Vote(Topic topic, Member voterMember, Member candidateMember) {
        this.topic = topic;
        this.voterMember = voterMember;
        this.candidateMember = candidateMember;
        this.createdAt = Instant.now();
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

    public Member getCandidateMember() {
        return candidateMember;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
