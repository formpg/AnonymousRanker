package com.anonranker.web;

import com.anonranker.config.AuthAttributes;
import com.anonranker.domain.Session;
import com.anonranker.service.SessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.HashSet;
import java.util.Set;

@Controller
@org.springframework.web.bind.annotation.RequestMapping("/s/{adminToken}")
public class SessionAuthController {

    private final SessionService sessionService;

    public SessionAuthController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    @GetMapping("/login")
    public String loginForm(@PathVariable String adminToken,
                             @RequestParam(required = false) String redirect,
                             Model model) {
        sessionService.getByAdminToken(adminToken); // 404s via handler if missing
        model.addAttribute("adminToken", adminToken);
        model.addAttribute("action", "/s/" + adminToken + "/login");
        model.addAttribute("redirect", redirect);
        return "login";
    }

    @PostMapping("/login")
    @SuppressWarnings("unchecked")
    public String login(@PathVariable String adminToken,
                         @RequestParam String password,
                         @RequestParam(required = false) String redirect,
                         HttpSession httpSession, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        if (!sessionService.checkPassword(session, password)) {
            model.addAttribute("adminToken", adminToken);
            model.addAttribute("action", "/s/" + adminToken + "/login");
            model.addAttribute("redirect", redirect);
            model.addAttribute("error", "パスワードが違います");
            return "login";
        }

        Set<String> adminAuth = (Set<String>) httpSession.getAttribute(AuthAttributes.ADMIN_AUTH_TOKENS);
        if (adminAuth == null) {
            adminAuth = new HashSet<>();
            httpSession.setAttribute(AuthAttributes.ADMIN_AUTH_TOKENS, adminAuth);
        }
        adminAuth.add(adminToken);

        return "redirect:" + (redirect != null && !redirect.isBlank() ? redirect : "/s/" + adminToken + "/admin");
    }

    @GetMapping("/announce/login")
    public String announceLoginForm(@PathVariable String adminToken,
                                     @RequestParam(required = false) String redirect,
                                     Model model) {
        sessionService.getByAdminToken(adminToken);
        model.addAttribute("adminToken", adminToken);
        model.addAttribute("action", "/s/" + adminToken + "/announce/login");
        model.addAttribute("redirect", redirect);
        return "login";
    }

    @PostMapping("/announce/login")
    @SuppressWarnings("unchecked")
    public String announceLogin(@PathVariable String adminToken,
                                 @RequestParam String password,
                                 @RequestParam(required = false) String redirect,
                                 HttpSession httpSession, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        if (!sessionService.checkPassword(session, password)) {
            model.addAttribute("adminToken", adminToken);
            model.addAttribute("action", "/s/" + adminToken + "/announce/login");
            model.addAttribute("redirect", redirect);
            model.addAttribute("error", "パスワードが違います");
            return "login";
        }

        Set<String> announceAuth = (Set<String>) httpSession.getAttribute(AuthAttributes.ANNOUNCE_AUTH_TOKENS);
        if (announceAuth == null) {
            announceAuth = new HashSet<>();
            httpSession.setAttribute(AuthAttributes.ANNOUNCE_AUTH_TOKENS, announceAuth);
        }
        announceAuth.add(adminToken);

        return "redirect:" + (redirect != null && !redirect.isBlank() ? redirect : "/s/" + adminToken + "/announce");
    }
}
