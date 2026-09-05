package com.anonranker.web;

import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.service.AnnouncementService;
import com.anonranker.service.SessionService;
import com.anonranker.service.TopicService;
import com.anonranker.web.dto.RankingRevealDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/s/{adminToken}/announce")
public class AnnouncementController {

    private final SessionService sessionService;
    private final TopicService topicService;
    private final AnnouncementService announcementService;

    public AnnouncementController(SessionService sessionService, TopicService topicService,
                                   AnnouncementService announcementService) {
        this.sessionService = sessionService;
        this.topicService = topicService;
        this.announcementService = announcementService;
    }

    @GetMapping
    public String announce(@PathVariable String adminToken, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        List<Topic> topics = topicService.listTopics(session);
        model.addAttribute("meeting", session);
        model.addAttribute("adminToken", adminToken);
        model.addAttribute("topics", topics);
        return "announce";
    }

    @GetMapping("/{topicId}/data")
    @ResponseBody
    public RankingRevealDto data(@PathVariable String adminToken, @PathVariable Long topicId) {
        Session session = sessionService.getByAdminToken(adminToken);
        Topic topic = topicService.getTopic(session, topicId);
        return announcementService.buildReveal(session, topic);
    }
}
