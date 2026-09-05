package com.anonranker.web;

import com.anonranker.config.AuthAttributes;
import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.domain.VotingRule;
import com.anonranker.service.BallotValidator;
import com.anonranker.service.SessionService;
import com.anonranker.service.TopicService;
import com.anonranker.service.VotingRuleService;
import com.anonranker.service.exception.InvalidBallotException;
import com.anonranker.web.dto.TopicBallotView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lets the organizer walk through the voting UI exactly as a real voter
 * would, without ever calling {@link com.anonranker.service.VotingService}
 * or touching the Vote/VoteSubmission tables - nothing here is persisted.
 */
@Controller
@RequestMapping("/s/{adminToken}/preview")
public class PreviewController {

    private final SessionService sessionService;
    private final TopicService topicService;
    private final VotingRuleService votingRuleService;
    private final BallotValidator ballotValidator;

    public PreviewController(SessionService sessionService, TopicService topicService,
                              VotingRuleService votingRuleService, BallotValidator ballotValidator) {
        this.sessionService = sessionService;
        this.topicService = topicService;
        this.votingRuleService = votingRuleService;
        this.ballotValidator = ballotValidator;
    }

    @GetMapping
    public String identifyForm(@PathVariable String adminToken, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        model.addAttribute("meeting", session);
        model.addAttribute("previewMode", true);
        model.addAttribute("action", "/s/" + adminToken + "/preview/identify");
        return "vote-identify";
    }

    @PostMapping("/identify")
    public String identify(@PathVariable String adminToken, @RequestParam Long memberId, HttpSession httpSession) {
        Session session = sessionService.getByAdminToken(adminToken);
        boolean valid = session.getMembers().stream().anyMatch(m -> m.getId().equals(memberId));
        if (!valid) {
            return "redirect:/s/" + adminToken + "/preview";
        }
        rememberVoter(httpSession, adminToken, memberId);
        return "redirect:/s/" + adminToken + "/preview/ballot";
    }

    @GetMapping("/ballot")
    public String ballotForm(@PathVariable String adminToken, HttpSession httpSession, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        Long voterId = requireVoter(httpSession, adminToken);
        if (voterId == null) {
            return "redirect:/s/" + adminToken + "/preview";
        }
        VotingRule rule = votingRuleService.getRule(session);

        List<TopicBallotView> topicViews = new ArrayList<>();
        for (Topic topic : topicService.listTopics(session)) {
            List<Member> candidates = eligibleCandidates(session, rule, voterId);
            topicViews.add(new TopicBallotView(topic.getId(), topic.getPrompt(), candidates,
                    padToSlots(List.of(), rule.getVotesPerTopic()), false, null));
        }

        model.addAttribute("meeting", session);
        model.addAttribute("previewMode", true);
        model.addAttribute("topicViews", topicViews);
        model.addAttribute("votesPerTopic", rule.getVotesPerTopic());
        model.addAttribute("action", "/s/" + adminToken + "/preview/ballot");
        return "vote-ballot";
    }

    @PostMapping("/ballot")
    public String submitBallot(@PathVariable String adminToken, HttpServletRequest request,
                                HttpSession httpSession, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        Long voterId = requireVoter(httpSession, adminToken);
        if (voterId == null) {
            return "redirect:/s/" + adminToken + "/preview";
        }
        Member voter = session.getMembers().stream().filter(m -> m.getId().equals(voterId)).findFirst().orElseThrow();
        VotingRule rule = votingRuleService.getRule(session);
        List<Topic> topics = topicService.listTopics(session);

        Map<Long, List<Long>> selectionsByTopicId = new HashMap<>();
        Map<Long, String> errorsByTopicId = new HashMap<>();
        for (Topic topic : topics) {
            List<Long> candidateIds = new ArrayList<>();
            for (int slot = 0; slot < rule.getVotesPerTopic(); slot++) {
                String raw = request.getParameter("vote_" + topic.getId() + "_" + slot);
                if (raw != null && !raw.isBlank()) {
                    candidateIds.add(Long.valueOf(raw));
                }
            }
            selectionsByTopicId.put(topic.getId(), candidateIds);
            try {
                ballotValidator.validate(rule, voter, session.getMembers(), candidateIds);
            } catch (InvalidBallotException ex) {
                errorsByTopicId.put(topic.getId(), ex.getMessage());
            }
        }

        List<TopicBallotView> topicViews = new ArrayList<>();
        for (Topic topic : topics) {
            List<Member> candidates = eligibleCandidates(session, rule, voterId);
            topicViews.add(new TopicBallotView(topic.getId(), topic.getPrompt(), candidates,
                    padToSlots(selectionsByTopicId.get(topic.getId()), rule.getVotesPerTopic()), false,
                    errorsByTopicId.get(topic.getId())));
        }

        if (!errorsByTopicId.isEmpty()) {
            model.addAttribute("meeting", session);
            model.addAttribute("previewMode", true);
            model.addAttribute("topicViews", topicViews);
            model.addAttribute("votesPerTopic", rule.getVotesPerTopic());
            model.addAttribute("action", "/s/" + adminToken + "/preview/ballot");
            return "vote-ballot";
        }

        return "redirect:/s/" + adminToken + "/preview/done";
    }

    @GetMapping("/done")
    public String done(@PathVariable String adminToken, Model model) {
        model.addAttribute("meeting", sessionService.getByAdminToken(adminToken));
        model.addAttribute("adminToken", adminToken);
        model.addAttribute("previewMode", true);
        return "vote-done";
    }

    private List<Member> eligibleCandidates(Session session, VotingRule rule, Long voterId) {
        return session.getMembers().stream()
                .filter(m -> rule.isAllowSelfVote() || !m.getId().equals(voterId))
                .toList();
    }

    private List<Long> padToSlots(List<Long> values, int slotCount) {
        List<Long> padded = new ArrayList<>(values);
        while (padded.size() < slotCount) {
            padded.add(null);
        }
        return padded;
    }

    @SuppressWarnings("unchecked")
    private void rememberVoter(HttpSession httpSession, String adminToken, Long memberId) {
        Map<String, Long> voters = (Map<String, Long>) httpSession.getAttribute(AuthAttributes.VOTER_MEMBER_IDS);
        if (voters == null) {
            voters = new HashMap<>();
            httpSession.setAttribute(AuthAttributes.VOTER_MEMBER_IDS, voters);
        }
        voters.put("preview:" + adminToken, memberId);
    }

    @SuppressWarnings("unchecked")
    private Long requireVoter(HttpSession httpSession, String adminToken) {
        Map<String, Long> voters = (Map<String, Long>) httpSession.getAttribute(AuthAttributes.VOTER_MEMBER_IDS);
        return voters != null ? voters.get("preview:" + adminToken) : null;
    }
}
