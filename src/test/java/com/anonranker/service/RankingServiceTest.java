package com.anonranker.service;

import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.domain.Vote;
import com.anonranker.domain.VotingRule;
import com.anonranker.repository.VoteRepository;
import com.anonranker.web.dto.RankEntryDto;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RankingServiceTest {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private VoteRepository voteRepository;

    private RankingService rankingService;

    private Session session;
    private Topic topic;
    private Member alice;
    private Member bob;
    private Member carol;
    private Member dave;
    private VotingRule rule;

    @BeforeEach
    void setUp() {
        rankingService = new RankingService(voteRepository);

        session = new Session("Test Session", "group-ranking-test", LocalDate.now(), "hash", "admin-token", "voting-token");
        alice = new Member(session, "Alice", 0);
        bob = new Member(session, "Bob", 1);
        carol = new Member(session, "Carol", 2);
        dave = new Member(session, "Dave", 3);
        session.getMembers().addAll(List.of(alice, bob, carol, dave));
        rule = new VotingRule(session);
        rule.setTopN(3);
        rule.setRevealCounts(true);
        session.setVotingRule(rule);
        topic = new Topic(session, "Who is best?", 0);
        session.getTopics().add(topic);

        entityManager.persist(session);
        entityManager.flush();

        // Bob: 2 votes, Carol: 1 vote, Dave: 1 vote (tie), Alice: 0 votes
        entityManager.persist(new Vote(topic, alice, bob));
        entityManager.persist(new Vote(topic, carol, bob));
        entityManager.persist(new Vote(topic, bob, carol));
        entityManager.persist(new Vote(topic, alice, dave));
        entityManager.flush();
    }

    @Test
    void ranksCandidatesByDescendingVoteCount() {
        List<RankEntryDto> result = rankingService.computeRanking(session, topic, rule);

        assertThat(result).extracting(RankEntryDto::getName).containsExactly("Bob", "Carol", "Dave");
        assertThat(result).extracting(RankEntryDto::getCount).containsExactly(2L, 1L, 1L);
    }

    @Test
    void tiedCandidatesShareTheSameRank() {
        List<RankEntryDto> result = rankingService.computeRanking(session, topic, rule);

        RankEntryDto carolEntry = result.stream().filter(r -> r.getName().equals("Carol")).findFirst().orElseThrow();
        RankEntryDto daveEntry = result.stream().filter(r -> r.getName().equals("Dave")).findFirst().orElseThrow();

        assertThat(carolEntry.getRank()).isEqualTo(daveEntry.getRank());
    }

    @Test
    void truncatesToTopN() {
        rule.setTopN(1);

        List<RankEntryDto> result = rankingService.computeRanking(session, topic, rule);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Bob");
    }

    @Test
    void omitsCountWhenRevealCountsIsFalse() {
        rule.setRevealCounts(false);

        List<RankEntryDto> result = rankingService.computeRanking(session, topic, rule);

        assertThat(result).allMatch(entry -> entry.getCount() == null);
    }
}
