package com.anonranker.web;

import com.anonranker.domain.Session;
import com.anonranker.service.AnnouncementService;
import com.anonranker.service.AnnouncementStateService;
import com.anonranker.service.SessionService;
import com.anonranker.service.TopicService;
import com.anonranker.web.dto.AnnouncementStateDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Public, no-password, read-only companion view of a live announcement.
 * Reached via the same voting link - lets a voter watch the organizer's
 * reveal happen live on their own device, in sync, without ever being able
 * to trigger or control it themselves.
 */
@Controller
@RequestMapping("/vote/{votingToken}/watch")
public class WatchController {

    private final SessionService sessionService;
    private final TopicService topicService;
    private final AnnouncementService announcementService;
    private final AnnouncementStateService announcementStateService;

    public WatchController(SessionService sessionService, TopicService topicService,
                            AnnouncementService announcementService,
                            AnnouncementStateService announcementStateService) {
        this.sessionService = sessionService;
        this.topicService = topicService;
        this.announcementService = announcementService;
        this.announcementStateService = announcementStateService;
    }

    @GetMapping
    public String watch(@PathVariable String votingToken, Model model) {
        model.addAttribute("meeting", sessionService.getByVotingToken(votingToken));
        model.addAttribute("votingToken", votingToken);
        return "watch";
    }

    @GetMapping("/state")
    @ResponseBody
    public AnnouncementStateDto state(@PathVariable String votingToken) {
        Session session = sessionService.getByVotingToken(votingToken);
        Long topicId = announcementStateService.getCurrentTopicId(session.getId());
        if (topicId == null) {
            return announcementService.emptyState();
        }
        return announcementService.currentState(session, topicService.getTopic(session, topicId));
    }
}
