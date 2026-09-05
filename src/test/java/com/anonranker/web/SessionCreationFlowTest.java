package com.anonranker.web;

import com.anonranker.domain.Session;
import com.anonranker.repository.SessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionCreationFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionRepository sessionRepository;

    @Test
    void createsSessionWithMembersAndDefaultRule() throws Exception {
        long before = sessionRepository.count();

        MvcResult result = mockMvc.perform(post("/sessions")
                        .param("title", "Year End Party")
                        .param("eventDate", "2026-12-31")
                        .param("password", "secret123")
                        .param("memberNames", "Alice\nBob\nCarol"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/s/*/admin"))
                .andReturn();

        assertThat(sessionRepository.count()).isEqualTo(before + 1);

        String location = result.getResponse().getRedirectedUrl();
        String adminToken = location.substring("/s/".length(), location.length() - "/admin".length());
        Session created = sessionRepository.findByAdminToken(adminToken).orElseThrow();
        assertThat(created.getMembers()).hasSize(3);
        assertThat(created.getVotingRule()).isNotNull();
    }
}
