package com.anonranker.web;

import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.service.AnnouncementService;
import com.anonranker.service.AnnouncementStateService;
import com.anonranker.service.SessionService;
import com.anonranker.service.TopicService;
import com.anonranker.web.dto.AnnouncementStateDto;
import com.anonranker.web.dto.RankingRevealDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
@RequestMapping("/s/{adminToken}/announce")
public class AnnouncementController {

    private final SessionService sessionService;
    private final TopicService topicService;
    private final AnnouncementService announcementService;
    private final AnnouncementStateService announcementStateService;

    public AnnouncementController(SessionService sessionService, TopicService topicService,
                                   AnnouncementService announcementService,
                                   AnnouncementStateService announcementStateService) {
        this.sessionService = sessionService;
        this.topicService = topicService;
        this.announcementService = announcementService;
        this.announcementStateService = announcementStateService;
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

    @PostMapping("/{topicId}/start")
    @ResponseBody
    public AnnouncementStateDto start(@PathVariable String adminToken, @PathVariable Long topicId) {
        Session session = sessionService.getByAdminToken(adminToken);
        Topic topic = topicService.getTopic(session, topicId);
        announcementStateService.startTopic(session.getId(), topicId);
        return announcementService.currentState(session, topic);
    }

    @PostMapping("/{topicId}/reveal-next")
    @ResponseBody
    public AnnouncementStateDto revealNext(@PathVariable String adminToken, @PathVariable Long topicId) {
        Session session = sessionService.getByAdminToken(adminToken);
        Topic topic = topicService.getTopic(session, topicId);
        RankingRevealDto sequence = announcementService.buildRevealSequence(session, topic);
        announcementStateService.revealNext(topicId, sequence.getRanks());
        return announcementService.currentState(session, topic);
    }

    @GetMapping("/state")
    @ResponseBody
    public AnnouncementStateDto state(@PathVariable String adminToken) {
        Session session = sessionService.getByAdminToken(adminToken);
        Long topicId = announcementStateService.getCurrentTopicId(session.getId());
        if (topicId == null) {
            return announcementService.emptyState();
        }
        return announcementService.currentState(session, topicService.getTopic(session, topicId));
    }
}
