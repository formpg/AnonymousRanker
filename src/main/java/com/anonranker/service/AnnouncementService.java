package com.anonranker.service;

import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.domain.VotingRule;
import com.anonranker.web.dto.AnnouncementStateDto;
import com.anonranker.web.dto.RankEntryDto;
import com.anonranker.web.dto.RankingRevealDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AnnouncementService {

    private final RankingService rankingService;
    private final VotingRuleService votingRuleService;
    private final AnnouncementStateService announcementStateService;

    public AnnouncementService(RankingService rankingService, VotingRuleService votingRuleService,
                                AnnouncementStateService announcementStateService) {
        this.rankingService = rankingService;
        this.votingRuleService = votingRuleService;
        this.announcementStateService = announcementStateService;
    }

    /**
     * Builds the full reveal sequence in the exact order it should be
     * announced in - server-authoritative so the organizer's trigger and
     * every voter's read-only "watch" view index into the same sequence.
     * Ascending rank order (1..N) for {@code TOP_DOWN}, reversed (countdown
     * style, lowest of the top N first) for {@code BOTTOM_UP}.
     */
    @Transactional(readOnly = true)
    public RankingRevealDto buildRevealSequence(Session session, Topic topic) {
        VotingRule rule = votingRuleService.getRule(session);
        List<RankEntryDto> ranks = rankingService.computeRanking(session, topic, rule);
        List<RankEntryDto> sequence = rule.getAnnounceOrder() == com.anonranker.domain.AnnounceOrder.BOTTOM_UP
                ? ranks.reversed()
                : ranks;
        List<String> candidatePool = session.getMembers().stream().map(Member::getName).toList();

        return new RankingRevealDto(
                topic.getPrompt(),
                rule.getTopN(),
                rule.getAnnounceOrder(),
                rule.getPacing(),
                rule.getAutoIntervalMs(),
                sequence,
                candidatePool
        );
    }

    @Transactional(readOnly = true)
    public AnnouncementStateDto currentState(Session session, Topic topic) {
        RankingRevealDto sequence = buildRevealSequence(session, topic);
        List<RankEntryDto> revealed = announcementStateService.getRevealed(topic.getId());
        boolean complete = revealed.size() >= sequence.getRanks().size();
        Integer nextRank = complete ? null : sequence.getRanks().get(revealed.size()).getRank();
        return new AnnouncementStateDto(
                topic.getId(),
                sequence.getTopicPrompt(),
                sequence.getTopN(),
                sequence.getAnnounceOrder(),
                sequence.getPacing(),
                sequence.getAutoIntervalMs(),
                revealed,
                complete,
                sequence.getCandidatePool(),
                nextRank
        );
    }

    public AnnouncementStateDto emptyState() {
        return new AnnouncementStateDto(null, null, 0, null, null, 0, List.of(), false, List.of(), null);
    }
}
