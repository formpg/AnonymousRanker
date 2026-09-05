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
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Covers "log back into a previously created group" - the organizer types
 * the group's ID + password again instead of needing to have kept the admin
 * link, per the requirement that a group can be revisited any number of
 * times this way.
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
    private String groupId;

    @BeforeEach
    void setUp() {
        groupId = "login-test-" + UUID.randomUUID();
        CreateSessionForm form = new CreateSessionForm();
        form.setTitle("Login Test Group");
        form.setGroupId(groupId);
        form.setEventDate(LocalDate.now());
        form.setPassword("correct-password");
        form.setMemberNames("Alice\nBob");
        adminToken = sessionService.createSession(form).getAdminToken();
    }

    @Test
    void correctGroupIdAndPasswordLogsBackIntoTheSameGroup() throws Exception {
        MockHttpSession httpSession = new MockHttpSession();

        mockMvc.perform(post("/sessions/login")
                        .session(httpSession)
                        .param("groupId", groupId)
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
                        .param("groupId", groupId)
                        .param("password", "wrong-password"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("グループIDまたはパスワードが違います")));

        mockMvc.perform(get("/s/" + adminToken + "/admin").session(httpSession))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void unknownGroupIdShowsAnError() throws Exception {
        mockMvc.perform(post("/sessions/login")
                        .param("groupId", "no-such-group-" + UUID.randomUUID())
                        .param("password", "whatever"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("グループIDまたはパスワードが違います")));
    }
}
