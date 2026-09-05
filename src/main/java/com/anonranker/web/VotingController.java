package com.anonranker.web;

import com.anonranker.config.AuthAttributes;
import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.domain.VotingRule;
import com.anonranker.service.SessionService;
import com.anonranker.service.TopicService;
import com.anonranker.service.VotingRuleService;
import com.anonranker.service.VotingService;
import com.anonranker.service.exception.AlreadyVotedException;
import com.anonranker.service.exception.InvalidBallotException;
import com.anonranker.web.dto.BallotForm;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Public, no-password voting flow reached via a session's unguessable voting
 * link. Every template rendered from here uses the nav-free voting layout so
 * a voter can never navigate anywhere but through the ballot flow.
 */
@Controller
@RequestMapping("/vote/{votingToken}")
public class VotingController {

    private final SessionService sessionService;
    private final TopicService topicService;
    private final VotingService votingService;
    private final VotingRuleService votingRuleService;

    public VotingController(SessionService sessionService, TopicService topicService,
                             VotingService votingService, VotingRuleService votingRuleService) {
        this.sessionService = sessionService;
        this.topicService = topicService;
        this.votingService = votingService;
        this.votingRuleService = votingRuleService;
    }

    @GetMapping
    public String identifyForm(@PathVariable String votingToken, Model model) {
        Session session = sessionService.getByVotingToken(votingToken);
        model.addAttribute("meeting", session);
        model.addAttribute("votingToken", votingToken);
        model.addAttribute("action", "/vote/" + votingToken + "/identify");
        return "vote-identify";
    }

    @PostMapping("/identify")
    public String identify(@PathVariable String votingToken, @RequestParam Long memberId, HttpSession httpSession) {
        Session session = sessionService.getByVotingToken(votingToken);
        boolean valid = session.getMembers().stream().anyMatch(m -> m.getId().equals(memberId));
        if (!valid) {
            return "redirect:/vote/" + votingToken;
        }
        rememberVoter(httpSession, votingToken, memberId);
        return "redirect:" + nextStepPath(session, memberId, votingToken);
    }

    @GetMapping("/topics/{topicId}")
    public String ballotForm(@PathVariable String votingToken, @PathVariable Long topicId,
                              HttpSession httpSession, Model model) {
        Session session = sessionService.getByVotingToken(votingToken);
        Long voterId = requireVoter(httpSession, votingToken);
        if (voterId == null) {
            return "redirect:/vote/" + votingToken;
        }
        Topic topic = topicService.getTopic(session, topicId);
        Member voter = findMember(session, voterId);
        if (votingService.hasSubmitted(topic, voter)) {
            return "redirect:" + nextStepPath(session, voterId, votingToken);
        }

        VotingRule rule = votingRuleService.getRule(session);
        List<Member> candidates = session.getMembers().stream()
                .filter(m -> rule.isAllowSelfVote() || !m.getId().equals(voterId))
                .toList();

        model.addAttribute("meeting", session);
        model.addAttribute("votingToken", votingToken);
        model.addAttribute("topic", topic);
        model.addAttribute("candidates", candidates);
        model.addAttribute("votesPerTopic", rule.getVotesPerTopic());
        model.addAttribute("action", "/vote/" + votingToken + "/topics/" + topicId);
        model.addAttribute("form", new BallotForm());
        return "vote-ballot";
    }

    @PostMapping("/topics/{topicId}")
    public String submitBallot(@PathVariable String votingToken, @PathVariable Long topicId,
                                @ModelAttribute("form") BallotForm form,
                                HttpSession httpSession, Model model) {
        Session session = sessionService.getByVotingToken(votingToken);
        Long voterId = requireVoter(httpSession, votingToken);
        if (voterId == null) {
            return "redirect:/vote/" + votingToken;
        }
        Topic topic = topicService.getTopic(session, topicId);
        Member voter = findMember(session, voterId);

        try {
            votingService.castBallot(session, topic, voter, form.getCandidateMemberIds());
        } catch (AlreadyVotedException | InvalidBallotException ex) {
            VotingRule rule = votingRuleService.getRule(session);
            List<Member> candidates = session.getMembers().stream()
                    .filter(m -> rule.isAllowSelfVote() || !m.getId().equals(voterId))
                    .toList();
            model.addAttribute("meeting", session);
            model.addAttribute("votingToken", votingToken);
            model.addAttribute("topic", topic);
            model.addAttribute("candidates", candidates);
            model.addAttribute("votesPerTopic", rule.getVotesPerTopic());
            model.addAttribute("action", "/vote/" + votingToken + "/topics/" + topicId);
            model.addAttribute("error", ex.getMessage());
            return "vote-ballot";
        }

        return "redirect:" + nextStepPath(session, voterId, votingToken);
    }

    @GetMapping("/done")
    public String done(@PathVariable String votingToken, Model model) {
        model.addAttribute("meeting", sessionService.getByVotingToken(votingToken));
        return "vote-done";
    }

    private String nextStepPath(Session session, Long voterId, String votingToken) {
        Member voter = findMember(session, voterId);
        List<Topic> topics = topicService.listTopics(session);
        Optional<Topic> next = topics.stream()
                .filter(t -> !votingService.hasSubmitted(t, voter))
                .findFirst();
        return next.map(t -> "/vote/" + votingToken + "/topics/" + t.getId())
                .orElse("/vote/" + votingToken + "/done");
    }

    private Member findMember(Session session, Long memberId) {
        return session.getMembers().stream()
                .filter(m -> m.getId().equals(memberId))
                .findFirst()
                .orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private void rememberVoter(HttpSession httpSession, String votingToken, Long memberId) {
        Map<String, Long> voters = (Map<String, Long>) httpSession.getAttribute(AuthAttributes.VOTER_MEMBER_IDS);
        if (voters == null) {
            voters = new HashMap<>();
            httpSession.setAttribute(AuthAttributes.VOTER_MEMBER_IDS, voters);
        }
        voters.put(votingToken, memberId);
    }

    @SuppressWarnings("unchecked")
    private Long requireVoter(HttpSession httpSession, String votingToken) {
        Map<String, Long> voters = (Map<String, Long>) httpSession.getAttribute(AuthAttributes.VOTER_MEMBER_IDS);
        return voters != null ? voters.get(votingToken) : null;
    }
}
