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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PreviewNoPersistTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private SessionService sessionService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private VoteRepository voteRepository;
    @Autowired
    private VoteSubmissionRepository voteSubmissionRepository;

    private Session session;
    private Topic topic;
    private Member alice;
    private Member bob;

    @BeforeEach
    void setUp() {
        CreateSessionForm form = new CreateSessionForm();
        form.setTitle("Preview Test");
        form.setEventDate(LocalDate.now());
        form.setPassword("pw");
        form.setMemberNames("Alice\nBob");
        session = sessionService.createSession(form);
        alice = session.getMembers().get(0);
        bob = session.getMembers().get(1);
        topic = topicService.addTopic(session, "Preview topic");
    }

    @Test
    void previewFlowNeverPersistsVotesOrSubmissions() throws Exception {
        MockHttpSession httpSession = new MockHttpSession();
        // Admin auth is granted automatically right after session creation via HomeController,
        // but this test drives SessionService directly, so authenticate through the real login flow.
        mockMvc.perform(post("/s/" + session.getAdminToken() + "/login")
                .session(httpSession)
                .param("password", "pw"));

        long votesBefore = voteRepository.count();
        long submissionsBefore = voteSubmissionRepository.count();

        mockMvc.perform(post("/s/" + session.getAdminToken() + "/preview/identify")
                .session(httpSession)
                .param("memberId", String.valueOf(alice.getId())));

        mockMvc.perform(get("/s/" + session.getAdminToken() + "/preview/topics/" + topic.getId())
                .session(httpSession));

        mockMvc.perform(post("/s/" + session.getAdminToken() + "/preview/topics/" + topic.getId())
                .session(httpSession)
                .param("candidateMemberIds", String.valueOf(bob.getId())));

        assertThat(voteRepository.count()).isEqualTo(votesBefore);
        assertThat(voteSubmissionRepository.count()).isEqualTo(submissionsBefore);
    }
}
