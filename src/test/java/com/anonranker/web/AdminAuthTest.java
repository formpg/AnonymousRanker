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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminAuthTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SessionService sessionService;

    private String adminToken;

    @BeforeEach
    void setUp() {
        CreateSessionForm form = new CreateSessionForm();
        form.setTitle("Auth Test Session");
        form.setEventDate(LocalDate.now());
        form.setPassword("correct-password");
        form.setMemberNames("Alice\nBob");
        adminToken = sessionService.createSession(form).getAdminToken();
    }

    @Test
    void unauthenticatedRequestIsRedirectedToLogin() throws Exception {
        mockMvc.perform(get("/s/" + adminToken + "/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("/s/" + adminToken + "/login*"));
    }

    @Test
    void wrongPasswordStaysOnLoginPage() throws Exception {
        MockHttpSession httpSession = new MockHttpSession();

        mockMvc.perform(post("/s/" + adminToken + "/login")
                        .session(httpSession)
                        .param("password", "wrong-password"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/s/" + adminToken + "/admin").session(httpSession))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    void correctPasswordGrantsAccessToAdminPage() throws Exception {
        MockHttpSession httpSession = new MockHttpSession();

        mockMvc.perform(post("/s/" + adminToken + "/login")
                        .session(httpSession)
                        .param("password", "correct-password"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/s/" + adminToken + "/admin"));

        mockMvc.perform(get("/s/" + adminToken + "/admin").session(httpSession))
                .andExpect(status().isOk());
    }
}
