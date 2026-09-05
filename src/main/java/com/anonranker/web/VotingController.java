package com.anonranker.web;

import com.anonranker.config.AuthAttributes;
import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.domain.VotingRule;
import com.anonranker.service.AnnouncementStateService;
import com.anonranker.service.BallotValidator;
import com.anonranker.service.SessionService;
import com.anonranker.service.TopicService;
import com.anonranker.service.VotingRuleService;
import com.anonranker.service.VotingService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Public, no-password voting flow reached via a session's unguessable voting
 * link. Every template rendered from here uses the nav-free voting layout so
 * a voter can never navigate anywhere but through this flow.
 */
@Controller
@RequestMapping("/vote/{votingToken}")
public class VotingController {

    private final SessionService sessionService;
    private final TopicService topicService;
    private final VotingService votingService;
    private final VotingRuleService votingRuleService;
    private final BallotValidator ballotValidator;
    private final AnnouncementStateService announcementStateService;

    public VotingController(SessionService sessionService, TopicService topicService,
                             VotingService votingService, VotingRuleService votingRuleService,
                             BallotValidator ballotValidator, AnnouncementStateService announcementStateService) {
        this.sessionService = sessionService;
        this.topicService = topicService;
        this.votingService = votingService;
        this.votingRuleService = votingRuleService;
        this.ballotValidator = ballotValidator;
        this.announcementStateService = announcementStateService;
    }

    @GetMapping
    public String menu(@PathVariable String votingToken, Model model) {
        model.addAttribute("meeting", sessionService.getByVotingToken(votingToken));
        model.addAttribute("votingToken", votingToken);
        return "vote-menu";
    }

    @GetMapping("/vote")
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
            return "redirect:/vote/" + votingToken + "/vote";
        }
        rememberVoter(httpSession, votingToken, memberId);
        return "redirect:/vote/" + votingToken + "/ballot";
    }

    @GetMapping("/ballot")
    public String ballotForm(@PathVariable String votingToken, HttpSession httpSession, Model model) {
        Session session = sessionService.getByVotingToken(votingToken);
        Long voterId = requireVoter(httpSession, votingToken);
        if (voterId == null) {
            return "redirect:/vote/" + votingToken + "/vote";
        }
        Member voter = findMember(session, voterId);
        VotingRule rule = votingRuleService.getRule(session);

        List<TopicBallotView> topicViews = new ArrayList<>();
        for (Topic topic : topicService.listTopics(session)) {
            boolean locked = announcementStateService.hasAnyReveal(topic.getId());
            List<Member> candidates = eligibleCandidates(session, rule, voterId);
            List<Long> existing = votingService.getExistingCandidateIds(topic, voter);
            topicViews.add(new TopicBallotView(topic.getId(), topic.getPrompt(), candidates,
                    padToSlots(existing, rule.getVotesPerTopic()), locked, null));
        }

        model.addAttribute("meeting", session);
        model.addAttribute("votingToken", votingToken);
        model.addAttribute("topicViews", topicViews);
        model.addAttribute("votesPerTopic", rule.getVotesPerTopic());
        model.addAttribute("action", "/vote/" + votingToken + "/ballot");
        return "vote-ballot";
    }

    @PostMapping("/ballot")
    public String submitBallot(@PathVariable String votingToken, HttpServletRequest request,
                                HttpSession httpSession, Model model) {
        Session session = sessionService.getByVotingToken(votingToken);
        Long voterId = requireVoter(httpSession, votingToken);
        if (voterId == null) {
            return "redirect:/vote/" + votingToken + "/vote";
        }
        Member voter = findMember(session, voterId);
        VotingRule rule = votingRuleService.getRule(session);
        List<Member> sessionMembers = session.getMembers();
        List<Topic> topics = topicService.listTopics(session);

        Map<Long, List<Long>> selectionsByTopicId = parseSelections(request, topics, rule.getVotesPerTopic());
        Map<Long, String> errorsByTopicId = new HashMap<>();

        for (Topic topic : topics) {
            if (announcementStateService.hasAnyReveal(topic.getId())) {
                continue;
            }
            try {
                ballotValidator.validate(rule, voter, sessionMembers, selectionsByTopicId.get(topic.getId()));
            } catch (InvalidBallotException ex) {
                errorsByTopicId.put(topic.getId(), ex.getMessage());
            }
        }

        if (!errorsByTopicId.isEmpty()) {
            List<TopicBallotView> topicViews = new ArrayList<>();
            for (Topic topic : topics) {
                boolean locked = announcementStateService.hasAnyReveal(topic.getId());
                List<Member> candidates = eligibleCandidates(session, rule, voterId);
                List<Long> submitted = locked
                        ? votingService.getExistingCandidateIds(topic, voter)
                        : selectionsByTopicId.get(topic.getId());
                topicViews.add(new TopicBallotView(topic.getId(), topic.getPrompt(), candidates,
                        padToSlots(submitted, rule.getVotesPerTopic()), locked, errorsByTopicId.get(topic.getId())));
            }
            model.addAttribute("meeting", session);
            model.addAttribute("votingToken", votingToken);
            model.addAttribute("topicViews", topicViews);
            model.addAttribute("votesPerTopic", rule.getVotesPerTopic());
            model.addAttribute("action", "/vote/" + votingToken + "/ballot");
            return "vote-ballot";
        }

        for (Topic topic : topics) {
            if (announcementStateService.hasAnyReveal(topic.getId())) {
                continue;
            }
            votingService.upsertBallot(session, topic, voter, selectionsByTopicId.get(topic.getId()));
        }

        return "redirect:/vote/" + votingToken + "/done";
    }

    @GetMapping("/done")
    public String done(@PathVariable String votingToken, Model model) {
        model.addAttribute("meeting", sessionService.getByVotingToken(votingToken));
        model.addAttribute("votingToken", votingToken);
        return "vote-done";
    }

    private List<Member> eligibleCandidates(Session session, VotingRule rule, Long voterId) {
        return session.getMembers().stream()
                .filter(m -> rule.isAllowSelfVote() || !m.getId().equals(voterId))
                .toList();
    }

    private Map<Long, List<Long>> parseSelections(HttpServletRequest request, List<Topic> topics, int votesPerTopic) {
        Map<Long, List<Long>> result = new LinkedHashMap<>();
        for (Topic topic : topics) {
            List<Long> candidateIds = new ArrayList<>();
            for (int slot = 0; slot < votesPerTopic; slot++) {
                String raw = request.getParameter("vote_" + topic.getId() + "_" + slot);
                if (raw != null && !raw.isBlank()) {
                    candidateIds.add(Long.valueOf(raw));
                }
            }
            result.put(topic.getId(), candidateIds);
        }
        return result;
    }

    private List<Long> padToSlots(List<Long> values, int slotCount) {
        List<Long> padded = new ArrayList<>(values);
        while (padded.size() < slotCount) {
            padded.add(null);
        }
        return padded;
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
