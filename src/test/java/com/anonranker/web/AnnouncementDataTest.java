package com.anonranker.web;

import com.anonranker.domain.AnnounceOrder;
import com.anonranker.domain.AnnouncePacing;
import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.service.AnnouncementService;
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
        form.setEventDate(LocalDate.now());
        form.setPassword("pw");
        form.setMemberNames("Alice\nBob\nCarol");
        session = sessionService.createSession(form);
        alice = session.getMembers().get(0);
        bob = session.getMembers().get(1);
        carol = session.getMembers().get(2);
        topic = topicService.addTopic(session, "Who is the MVP?");

        // Bob receives 2 votes (from Alice and Carol), Carol receives 1 vote (from Bob)
        votingService.castBallot(session, topic, alice, List.of(bob.getId()));
        votingService.castBallot(session, topic, carol, List.of(bob.getId()));
        votingService.castBallot(session, topic, bob, List.of(carol.getId()));
    }

    @Test
    void revealReflectsAggregatedCountsInRankOrder() {
        RankingRevealDto reveal = announcementService.buildReveal(session, topic);

        assertThat(reveal.getRanks()).extracting("name").containsExactly("Bob", "Carol", "Alice");
        assertThat(reveal.getRanks()).extracting("count").containsExactly(2L, 1L, 0L);
        assertThat(reveal.getCandidatePool()).containsExactlyInAnyOrder("Alice", "Bob", "Carol");
    }

    @Test
    void jsonOmitsCountFieldEntirelyWhenRevealCountsIsDisabled() throws Exception {
        VotingRuleForm ruleForm = new VotingRuleForm();
        ruleForm.setVotesPerTopic(1);
        ruleForm.setAllowSelfVote(false);
        ruleForm.setTopN(3);
        ruleForm.setRevealCounts(false);
        ruleForm.setAnnounceOrder(AnnounceOrder.TOP_DOWN);
        ruleForm.setPacing(AnnouncePacing.MANUAL);
        ruleForm.setAutoIntervalMs(3000);
        votingRuleService.updateRule(session, ruleForm);

        RankingRevealDto reveal = announcementService.buildReveal(session, topic);
        String json = objectMapper.writeValueAsString(reveal);

        assertThat(json).doesNotContain("\"count\"");
    }
}
