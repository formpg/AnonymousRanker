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
import com.anonranker.web.dto.BallotForm;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

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
        List<Topic> topics = topicService.listTopics(session);
        if (topics.isEmpty()) {
            return "redirect:/s/" + adminToken + "/preview/done";
        }
        return "redirect:/s/" + adminToken + "/preview/topics/" + topics.get(0).getId();
    }

    @GetMapping("/topics/{topicId}")
    public String ballotForm(@PathVariable String adminToken, @PathVariable Long topicId,
                              HttpSession httpSession, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        Long voterId = requireVoter(httpSession, adminToken);
        if (voterId == null) {
            return "redirect:/s/" + adminToken + "/preview";
        }
        Topic topic = topicService.getTopic(session, topicId);
        VotingRule rule = votingRuleService.getRule(session);
        List<Member> candidates = session.getMembers().stream()
                .filter(m -> rule.isAllowSelfVote() || !m.getId().equals(voterId))
                .toList();

        model.addAttribute("meeting", session);
        model.addAttribute("previewMode", true);
        model.addAttribute("topic", topic);
        model.addAttribute("candidates", candidates);
        model.addAttribute("votesPerTopic", rule.getVotesPerTopic());
        model.addAttribute("action", "/s/" + adminToken + "/preview/topics/" + topicId);
        model.addAttribute("form", new BallotForm());
        return "vote-ballot";
    }

    @PostMapping("/topics/{topicId}")
    public String submitBallot(@PathVariable String adminToken, @PathVariable Long topicId,
                                @ModelAttribute("form") BallotForm form,
                                HttpSession httpSession, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        Long voterId = requireVoter(httpSession, adminToken);
        if (voterId == null) {
            return "redirect:/s/" + adminToken + "/preview";
        }
        Topic topic = topicService.getTopic(session, topicId);
        VotingRule rule = votingRuleService.getRule(session);
        Member voter = session.getMembers().stream().filter(m -> m.getId().equals(voterId)).findFirst().orElseThrow();

        try {
            ballotValidator.validate(rule, voter, session.getMembers(), form.getCandidateMemberIds());
        } catch (InvalidBallotException ex) {
            List<Member> candidates = session.getMembers().stream()
                    .filter(m -> rule.isAllowSelfVote() || !m.getId().equals(voterId))
                    .toList();
            model.addAttribute("meeting", session);
            model.addAttribute("previewMode", true);
            model.addAttribute("topic", topic);
            model.addAttribute("candidates", candidates);
            model.addAttribute("votesPerTopic", rule.getVotesPerTopic());
            model.addAttribute("action", "/s/" + adminToken + "/preview/topics/" + topicId);
            model.addAttribute("error", ex.getMessage());
            return "vote-ballot";
        }

        List<Topic> topics = topicService.listTopics(session);
        int index = -1;
        for (int i = 0; i < topics.size(); i++) {
            if (topics.get(i).getId().equals(topicId)) {
                index = i;
                break;
            }
        }
        if (index >= 0 && index + 1 < topics.size()) {
            return "redirect:/s/" + adminToken + "/preview/topics/" + topics.get(index + 1).getId();
        }
        return "redirect:/s/" + adminToken + "/preview/done";
    }

    @GetMapping("/done")
    public String done(@PathVariable String adminToken, Model model) {
        model.addAttribute("meeting", sessionService.getByAdminToken(adminToken));
        model.addAttribute("previewMode", true);
        return "vote-done";
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
