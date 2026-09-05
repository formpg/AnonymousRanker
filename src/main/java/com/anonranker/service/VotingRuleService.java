package com.anonranker.service;

import com.anonranker.domain.Session;
import com.anonranker.domain.VotingRule;
import com.anonranker.repository.VotingRuleRepository;
import com.anonranker.web.dto.VotingRuleForm;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VotingRuleService {

    private final VotingRuleRepository votingRuleRepository;

    public VotingRuleService(VotingRuleRepository votingRuleRepository) {
        this.votingRuleRepository = votingRuleRepository;
    }

    @Transactional(readOnly = true)
    public VotingRule getRule(Session session) {
        return votingRuleRepository.findBySession(session)
                .orElseThrow(() -> new EntityNotFoundException("Voting rule not found for session " + session.getId()));
    }

    @Transactional
    public void updateRule(Session session, VotingRuleForm form) {
        VotingRule rule = getRule(session);
        rule.setVotesPerTopic(form.getVotesPerTopic());
        rule.setAllowSelfVote(form.isAllowSelfVote());
        rule.setTopN(form.getTopN());
        rule.setRevealCounts(form.isRevealCounts());
        rule.setAnnounceOrder(form.getAnnounceOrder());
        rule.setPacing(form.getPacing());
        rule.setAutoIntervalMs(form.getAutoIntervalSeconds() * 1000);
    }
}
