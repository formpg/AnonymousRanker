package com.anonranker.web;

import com.anonranker.config.AuthAttributes;
import com.anonranker.domain.Session;
import com.anonranker.service.SessionService;
import com.anonranker.web.dto.CreateSessionForm;
import com.anonranker.web.dto.SessionLoginForm;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Controller
public class HomeController {

    private final SessionService sessionService;

    public HomeController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/")
    public String home() {
        return "home";
    }

    @GetMapping("/sessions/new")
    public String newSessionForm(Model model) {
        model.addAttribute("form", new CreateSessionForm());
        return "session-new";
    }

    @PostMapping("/sessions")
    public String createSession(@Valid @ModelAttribute("form") CreateSessionForm form, BindingResult bindingResult,
                                 HttpSession httpSession, Model model) {
        if (bindingResult.hasErrors()) {
            return "session-new";
        }

        Session session;
        try {
            session = sessionService.createSession(form);
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            return "session-new";
        }

        grantAdminAuth(httpSession, session.getAdminToken());
        return "redirect:/s/" + session.getAdminToken() + "/admin";
    }

    @GetMapping("/sessions/login")
    public String loginForm(Model model) {
        model.addAttribute("form", new SessionLoginForm());
        return "session-login";
    }

    @PostMapping("/sessions/login")
    public String login(@ModelAttribute("form") SessionLoginForm form, HttpSession httpSession, Model model) {
        Optional<Session> session = sessionService.loginByTitleAndPassword(form.getTitle(), form.getPassword());
        if (session.isEmpty()) {
            model.addAttribute("error", "タイトルまたはパスワードが違います");
            return "session-login";
        }

        grantAdminAuth(httpSession, session.get().getAdminToken());
        return "redirect:/s/" + session.get().getAdminToken() + "/admin";
    }

    @SuppressWarnings("unchecked")
    private void grantAdminAuth(HttpSession httpSession, String adminToken) {
        Set<String> adminAuth = (Set<String>) httpSession.getAttribute(AuthAttributes.ADMIN_AUTH_TOKENS);
        if (adminAuth == null) {
            adminAuth = new HashSet<>();
            httpSession.setAttribute(AuthAttributes.ADMIN_AUTH_TOKENS, adminAuth);
        }
        adminAuth.add(adminToken);
    }
}
