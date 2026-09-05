package com.anonranker.web;

import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.service.SessionService;
import com.anonranker.service.TopicService;
import com.anonranker.web.dto.TopicForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/s/{adminToken}/topics")
public class TopicController {

    private final SessionService sessionService;
    private final TopicService topicService;

    public TopicController(SessionService sessionService, TopicService topicService) {
        this.sessionService = sessionService;
        this.topicService = topicService;
    }

    @GetMapping("/new")
    public String newForm(@PathVariable String adminToken, Model model) {
        model.addAttribute("adminToken", adminToken);
        model.addAttribute("form", new TopicForm());
        model.addAttribute("action", "/s/" + adminToken + "/topics");
        return "topic-form";
    }

    @PostMapping
    public String create(@PathVariable String adminToken, @Valid @ModelAttribute("form") TopicForm form,
                          BindingResult bindingResult, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        if (bindingResult.hasErrors()) {
            model.addAttribute("adminToken", adminToken);
            model.addAttribute("action", "/s/" + adminToken + "/topics");
            return "topic-form";
        }
        topicService.addTopic(session, form.getPrompt());
        return "redirect:/s/" + adminToken + "/admin";
    }

    @GetMapping("/{topicId}/edit")
    public String editForm(@PathVariable String adminToken, @PathVariable Long topicId, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        Topic topic = topicService.getTopic(session, topicId);
        model.addAttribute("adminToken", adminToken);
        model.addAttribute("form", new TopicForm(topic.getPrompt()));
        model.addAttribute("action", "/s/" + adminToken + "/topics/" + topicId);
        return "topic-form";
    }

    @PostMapping("/{topicId}")
    public String update(@PathVariable String adminToken, @PathVariable Long topicId,
                          @Valid @ModelAttribute("form") TopicForm form, BindingResult bindingResult, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        if (bindingResult.hasErrors()) {
            model.addAttribute("adminToken", adminToken);
            model.addAttribute("action", "/s/" + adminToken + "/topics/" + topicId);
            return "topic-form";
        }
        topicService.updateTopic(session, topicId, form.getPrompt());
        return "redirect:/s/" + adminToken + "/admin";
    }

    @PostMapping("/{topicId}/delete")
    public String delete(@PathVariable String adminToken, @PathVariable Long topicId) {
        Session session = sessionService.getByAdminToken(adminToken);
        topicService.deleteTopic(session, topicId);
        return "redirect:/s/" + adminToken + "/admin";
    }
}
