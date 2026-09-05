package com.anonranker.web;

import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.repository.MemberRepository;
import com.anonranker.repository.VoteSubmissionRepository;
import com.anonranker.service.SessionService;
import com.anonranker.service.TopicService;
import com.anonranker.web.dto.SubmissionProgressDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping("/s/{adminToken}")
public class SessionAdminController {

    private final SessionService sessionService;
    private final TopicService topicService;
    private final MemberRepository memberRepository;
    private final VoteSubmissionRepository voteSubmissionRepository;

    public SessionAdminController(SessionService sessionService, TopicService topicService,
                                   MemberRepository memberRepository,
                                   VoteSubmissionRepository voteSubmissionRepository) {
        this.sessionService = sessionService;
        this.topicService = topicService;
        this.memberRepository = memberRepository;
        this.voteSubmissionRepository = voteSubmissionRepository;
    }

    @GetMapping("/admin")
    public String admin(@PathVariable String adminToken, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        List<Topic> topics = topicService.listTopics(session);
        long totalMembers = memberRepository.countBySession(session);

        List<SubmissionProgressDto> progress = topics.stream()
                .map(topic -> new SubmissionProgressDto(
                        topic.getId(),
                        topic.getPrompt(),
                        voteSubmissionRepository.countByTopic(topic),
                        totalMembers
                ))
                .toList();

        model.addAttribute("meeting", session);
        model.addAttribute("adminToken", adminToken);
        model.addAttribute("topics", topics);
        model.addAttribute("progress", progress);
        model.addAttribute("votingPath", "/vote/" + session.getVotingToken());
        return "admin";
    }
}
