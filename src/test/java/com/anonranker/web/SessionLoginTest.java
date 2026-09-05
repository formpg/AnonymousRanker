package com.anonranker.web;

import com.anonranker.service.SessionService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.containsString;

/**
 * Covers "log back into a previously created session" - the organizer types
 * the session's title + password again instead of needing to have kept the
 * admin link, per the requirement that a session can be revisited any
 * number of times this way.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SessionLoginTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    private String adminToken;
    private String sessionTitle;

    @BeforeEach
    void setUp() {
        // Title is randomized per test method run because the test datasource is
        // shared across test methods (and classes, via Spring's context cache) -
        // a fixed title would collide with sessions created by other @BeforeEach
        // invocations and make findByTitle()'s match ambiguous.
        sessionTitle = "Login Test Session " + java.util.UUID.randomUUID();
        CreateSessionForm form = new CreateSessionForm();
        form.setTitle(sessionTitle);
        form.setEventDate(LocalDate.now());
        form.setPassword("correct-password");
        form.setMemberNames("Alice\nBob");
        adminToken = sessionService.createSession(form).getAdminToken();
    }

    @Test
    void correctTitleAndPasswordLogsBackIntoTheSameSession() throws Exception {
        MockHttpSession httpSession = new MockHttpSession();

        mockMvc.perform(post("/sessions/login")
                        .session(httpSession)
                        .param("title", sessionTitle)
                        .param("password", "correct-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/s/" + adminToken + "/admin"));

        mockMvc.perform(get("/s/" + adminToken + "/admin").session(httpSession))
                .andExpect(status().isOk());
    }

    @Test
    void wrongPasswordShowsAnErrorWithoutGrantingAccess() throws Exception {
        MockHttpSession httpSession = new MockHttpSession();

        mockMvc.perform(post("/sessions/login")
                        .session(httpSession)
                        .param("title", sessionTitle)
                        .param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("タイトルまたはパスワードが違います")));

        mockMvc.perform(get("/s/" + adminToken + "/admin").session(httpSession))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void unknownTitleShowsAnError() throws Exception {
        mockMvc.perform(post("/sessions/login")
                        .param("title", "No Such Session " + java.util.UUID.randomUUID())
                        .param("password", "whatever"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("タイトルまたはパスワードが違います")));
    }
}
