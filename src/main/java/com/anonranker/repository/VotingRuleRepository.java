package com.anonranker.repository;

import com.anonranker.domain.Session;
import com.anonranker.domain.VotingRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VotingRuleRepository extends JpaRepository<VotingRule, Long> {
    Optional<VotingRule> findBySession(Session session);
}
