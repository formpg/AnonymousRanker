package com.anonranker.service;

import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.domain.VotingRule;
import com.anonranker.web.dto.RankEntryDto;
import com.anonranker.web.dto.RankingRevealDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementService {

    private final RankingService rankingService;
    private final VotingRuleService votingRuleService;

    public AnnouncementService(RankingService rankingService, VotingRuleService votingRuleService) {
        this.rankingService = rankingService;
        this.votingRuleService = votingRuleService;
    }

    @Transactional(readOnly = true)
    public RankingRevealDto buildReveal(Session session, Topic topic) {
        VotingRule rule = votingRuleService.getRule(session);
        List<RankEntryDto> ranks = rankingService.computeRanking(session, topic, rule);
        List<String> candidatePool = session.getMembers().stream().map(Member::getName).toList();

        return new RankingRevealDto(
                topic.getPrompt(),
                rule.getTopN(),
                rule.getAnnounceOrder(),
                rule.getPacing(),
                rule.getAutoIntervalMs(),
                ranks,
                candidatePool
        );
    }
}
