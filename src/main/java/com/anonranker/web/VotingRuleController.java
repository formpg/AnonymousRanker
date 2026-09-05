package com.anonranker.web;

import com.anonranker.domain.Session;
import com.anonranker.domain.VotingRule;
import com.anonranker.service.SessionService;
import com.anonranker.service.VotingRuleService;
import com.anonranker.web.dto.VotingRuleForm;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/s/{adminToken}/rules")
public class VotingRuleController {

    private final SessionService sessionService;
    private final VotingRuleService votingRuleService;

    public VotingRuleController(SessionService sessionService, VotingRuleService votingRuleService) {
        this.sessionService = sessionService;
        this.votingRuleService = votingRuleService;
    }

    @GetMapping
    public String edit(@PathVariable String adminToken, Model model) {
        Session session = sessionService.getByAdminToken(adminToken);
        VotingRule rule = votingRuleService.getRule(session);

        VotingRuleForm form = new VotingRuleForm();
        form.setVotesPerTopic(rule.getVotesPerTopic());
        form.setAllowSelfVote(rule.isAllowSelfVote());
        form.setTopN(rule.getTopN());
        form.setRevealCounts(rule.isRevealCounts());
        form.setAnnounceOrder(rule.getAnnounceOrder());
        form.setPacing(rule.getPacing());
        form.setAutoIntervalMs(rule.getAutoIntervalMs());

        model.addAttribute("adminToken", adminToken);
        model.addAttribute("form", form);
        return "rules-form";
    }

    @PostMapping
    public String update(@PathVariable String adminToken, @Valid @ModelAttribute("form") VotingRuleForm form,
                          BindingResult bindingResult, Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("adminToken", adminToken);
            return "rules-form";
        }
        Session session = sessionService.getByAdminToken(adminToken);
        votingRuleService.updateRule(session, form);
        return "redirect:/s/" + adminToken + "/admin";
    }
}
