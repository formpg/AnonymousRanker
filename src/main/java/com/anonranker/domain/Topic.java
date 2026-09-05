package com.anonranker.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "topics")
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Session session;

    @Column(nullable = false)
    private String prompt;

    @Column(nullable = false)
    private int displayOrder;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Topic() {
    }

    public Topic(Session session, String prompt, int displayOrder) {
        this.session = session;
        this.prompt = prompt;
        this.displayOrder = displayOrder;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Session getSession() {
        return session;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(int displayOrder) {
        this.displayOrder = displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
