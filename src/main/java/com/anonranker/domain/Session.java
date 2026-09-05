package com.anonranker.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sessions")
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDate eventDate;

    @Column(nullable = false)
    private String passwordHash;

    @Column(nullable = false, unique = true, updatable = false)
    private String adminToken;

    @Column(nullable = false, unique = true, updatable = false)
    private String votingToken;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<Member> members = new ArrayList<>();

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC")
    private List<Topic> topics = new ArrayList<>();

    @OneToOne(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    private VotingRule votingRule;

    protected Session() {
    }

    public Session(String title, LocalDate eventDate, String passwordHash, String adminToken, String votingToken) {
        this.title = title;
        this.eventDate = eventDate;
        this.passwordHash = passwordHash;
        this.adminToken = adminToken;
        this.votingToken = votingToken;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getAdminToken() {
        return adminToken;
    }

    public String getVotingToken() {
        return votingToken;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public List<Member> getMembers() {
        return members;
    }

    public List<Topic> getTopics() {
        return topics;
    }

    public VotingRule getVotingRule() {
        return votingRule;
    }

    public void setVotingRule(VotingRule votingRule) {
        this.votingRule = votingRule;
    }
}
