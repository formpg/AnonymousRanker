package com.anonranker.web;

import com.anonranker.domain.AnnounceOrder;
import com.anonranker.domain.AnnouncePacing;
import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.service.AnnouncementService;
import com.anonranker.service.AnnouncementStateService;
import com.anonranker.service.SessionService;
import com.anonranker.service.TopicService;
import com.anonranker.service.VotingRuleService;
import com.anonranker.service.VotingService;
import com.anonranker.web.dto.CreateSessionForm;
import com.anonranker.web.dto.RankingRevealDto;
import com.anonranker.web.dto.VotingRuleForm;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class AnnouncementDataTest {

    @Autowired
    private SessionService sessionService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private VotingService votingService;
    @Autowired
    private VotingRuleService votingRuleService;
    @Autowired
    private AnnouncementService announcementService;
    @Autowired
    private AnnouncementStateService announcementStateService;
    @Autowired
    private ObjectMapper objectMapper;

    private Session session;
    private Topic topic;
    private Member alice;
    private Member bob;
    private Member carol;

    @BeforeEach
    void setUp() {
        CreateSessionForm form = new CreateSessionForm();
        form.setTitle("Announcement Test");
        form.setGroupId("announcement-test-" + java.util.UUID.randomUUID());
        form.setEventDate(LocalDate.now());
        form.setPassword("pw");
        form.setMemberNames("Alice\nBob\nCarol");
        session = sessionService.createSession(form);
        alice = session.getMembers().get(0);
        bob = session.getMembers().get(1);
        carol = session.getMembers().get(2);
        topic = topicService.addTopic(session, "Who is the MVP?");
        applyRule(true, AnnounceOrder.TOP_DOWN);

        // Bob receives 2 votes (from Alice and Carol), Carol receives 1 vote (from Bob)
        votingService.upsertBallot(session, topic, alice, List.of(bob.getId()));
        votingService.upsertBallot(session, topic, carol, List.of(bob.getId()));
        votingService.upsertBallot(session, topic, bob, List.of(carol.getId()));
    }

    private void applyRule(boolean revealCounts, AnnounceOrder order) {
        VotingRuleForm ruleForm = new VotingRuleForm();
        ruleForm.setVotesPerTopic(1);
        ruleForm.setAllowSelfVote(false);
        ruleForm.setTopN(3);
        ruleForm.setRevealCounts(revealCounts);
        ruleForm.setAnnounceOrder(order);
        ruleForm.setPacing(AnnouncePacing.MANUAL);
        ruleForm.setAutoIntervalSeconds(3);
        votingRuleService.updateRule(session, ruleForm);
    }

    @Test
    void topDownSequenceStartsWithRankOne() {
        RankingRevealDto reveal = announcementService.buildRevealSequence(session, topic);

        assertThat(reveal.getRanks()).extracting("name").containsExactly("Bob", "Carol", "Alice");
        assertThat(reveal.getRanks()).extracting("count").containsExactly(2L, 1L, 0L);
        assertThat(reveal.getCandidatePool()).containsExactlyInAnyOrder("Alice", "Bob", "Carol");
    }

    @Test
    void bottomUpSequenceIsReversed() {
        applyRule(true, AnnounceOrder.BOTTOM_UP);

        RankingRevealDto reveal = announcementService.buildRevealSequence(session, topic);

        assertThat(reveal.getRanks()).extracting("name").containsExactly("Alice", "Carol", "Bob");
    }

    @Test
    void jsonOmitsCountFieldEntirelyWhenRevealCountsIsDisabled() throws Exception {
        applyRule(false, AnnounceOrder.TOP_DOWN);

        RankingRevealDto reveal = announcementService.buildRevealSequence(session, topic);
        String json = objectMapper.writeValueAsString(reveal);

        assertThat(json).doesNotContain("\"count\"");
    }

    @Test
    void currentStateTracksIncrementalReveal() {
        announcementStateService.startTopic(session.getId(), topic.getId());
        RankingRevealDto sequence = announcementService.buildRevealSequence(session, topic);

        assertThat(announcementService.currentState(session, topic).isComplete()).isFalse();
        assertThat(announcementService.currentState(session, topic).getRevealed()).isEmpty();

        announcementStateService.revealNext(topic.getId(), sequence.getRanks());
        var afterFirst = announcementService.currentState(session, topic);
        assertThat(afterFirst.getRevealed()).extracting("name").containsExactly("Bob");
        assertThat(afterFirst.isComplete()).isFalse();

        announcementStateService.revealNext(topic.getId(), sequence.getRanks());
        announcementStateService.revealNext(topic.getId(), sequence.getRanks());
        var afterAll = announcementService.currentState(session, topic);
        assertThat(afterAll.getRevealed()).extracting("name").containsExactly("Bob", "Carol", "Alice");
        assertThat(afterAll.isComplete()).isTrue();
    }

    /**
     * A tie can push more entries into the sequence than topN (everyone
     * tied for 1st all carries rank 1, so all of them get included) - the
     * server-computed nextRank must track the real rank of each of those
     * entries, never a position-based guess that could drift below 1.
     */
    @Test
    void nextRankStaysCorrectThroughAThreeWayTie() {
        CreateSessionForm form = new CreateSessionForm();
        form.setTitle("Tie Test");
        form.setGroupId("tie-test-" + java.util.UUID.randomUUID());
        form.setEventDate(LocalDate.now());
        form.setPassword("pw");
        form.setMemberNames("Alice\nBob\nCarol");
        Session tieSession = sessionService.createSession(form);
        Topic tieTopic = topicService.addTopic(tieSession, "Nobody votes - everyone ties at 0");

        VotingRuleForm ruleForm = new VotingRuleForm();
        ruleForm.setVotesPerTopic(1);
        ruleForm.setAllowSelfVote(false);
        ruleForm.setTopN(1);
        ruleForm.setRevealCounts(true);
        ruleForm.setAnnounceOrder(AnnounceOrder.BOTTOM_UP);
        ruleForm.setPacing(AnnouncePacing.MANUAL);
        ruleForm.setAutoIntervalSeconds(3);
        votingRuleService.updateRule(tieSession, ruleForm);

        RankingRevealDto sequence = announcementService.buildRevealSequence(tieSession, tieTopic);
        assertThat(sequence.getRanks()).hasSize(3);
        assertThat(sequence.getRanks()).allMatch(r -> r.getRank() == 1);

        announcementStateService.startTopic(tieSession.getId(), tieTopic.getId());
        assertThat(announcementService.currentState(tieSession, tieTopic).getNextRank()).isEqualTo(1);

        announcementStateService.revealNext(tieTopic.getId(), sequence.getRanks());
        assertThat(announcementService.currentState(tieSession, tieTopic).getNextRank()).isEqualTo(1);

        announcementStateService.revealNext(tieTopic.getId(), sequence.getRanks());
        assertThat(announcementService.currentState(tieSession, tieTopic).getNextRank()).isEqualTo(1);

        announcementStateService.revealNext(tieTopic.getId(), sequence.getRanks());
        var finalState = announcementService.currentState(tieSession, tieTopic);
        assertThat(finalState.isComplete()).isTrue();
        assertThat(finalState.getNextRank()).isNull();
    }
}
