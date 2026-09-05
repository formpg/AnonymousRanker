package com.anonranker.web;

import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.repository.VoteRepository;
import com.anonranker.repository.VoteSubmissionRepository;
import com.anonranker.service.SessionService;
import com.anonranker.service.TopicService;
import com.anonranker.web.dto.CreateSessionForm;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class VotingFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private TopicService topicService;

    @Autowired
    private VoteSubmissionRepository voteSubmissionRepository;

    @Autowired
    private VoteRepository voteRepository;

    private Session session;
    private Topic topic;
    private Member alice;
    private Member bob;
    private Member carol;

    @BeforeEach
    void setUp() {
        CreateSessionForm form = new CreateSessionForm();
        form.setTitle("Voting Flow Test");
        form.setGroupId("voting-flow-test-" + java.util.UUID.randomUUID());
        form.setEventDate(LocalDate.now());
        form.setPassword("pw");
        form.setMemberNames("Alice\nBob\nCarol");
        session = sessionService.createSession(form);
        alice = session.getMembers().get(0);
        bob = session.getMembers().get(1);
        carol = session.getMembers().get(2);
        topic = topicService.addTopic(session, "Who is the best?");
    }

    private MockHttpSession identifyAs(Member member) throws Exception {
        MockHttpSession httpSession = new MockHttpSession();
        mockMvc.perform(post("/vote/" + session.getVotingToken() + "/identify")
                .session(httpSession)
                .param("memberId", String.valueOf(member.getId())));
        return httpSession;
    }

    @Test
    void ballotPageExcludesTheVoterWhenSelfVoteIsDisallowed() throws Exception {
        MockHttpSession httpSession = identifyAs(alice);

        mockMvc.perform(get("/vote/" + session.getVotingToken() + "/ballot").session(httpSession))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString(">" + alice.getName() + "<"))))
                .andExpect(content().string(containsString(bob.getName())));
    }

    @Test
    void castingABallotRecordsAVote() throws Exception {
        MockHttpSession httpSession = identifyAs(alice);

        mockMvc.perform(post("/vote/" + session.getVotingToken() + "/ballot")
                        .session(httpSession)
                        .param("vote_" + topic.getId() + "_0", String.valueOf(bob.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vote/" + session.getVotingToken() + "/done"));

        assertThat(voteSubmissionRepository.existsByTopicAndVoterMember(topic, alice)).isTrue();
        assertThat(voteRepository.findByTopicAndVoterMember(topic, alice))
                .extracting(v -> v.getCandidateMember().getId())
                .containsExactly(bob.getId());
    }

    @Test
    void resubmittingTheBallotOverwritesThePreviousVote() throws Exception {
        MockHttpSession httpSession = identifyAs(alice);

        mockMvc.perform(post("/vote/" + session.getVotingToken() + "/ballot")
                .session(httpSession)
                .param("vote_" + topic.getId() + "_0", String.valueOf(bob.getId())));

        mockMvc.perform(post("/vote/" + session.getVotingToken() + "/ballot")
                        .session(httpSession)
                        .param("vote_" + topic.getId() + "_0", String.valueOf(carol.getId())))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/vote/" + session.getVotingToken() + "/done"));

        assertThat(voteRepository.findByTopicAndVoterMember(topic, alice))
                .extracting(v -> v.getCandidateMember().getId())
                .containsExactly(carol.getId());
    }
}
