package com.anonranker.service;

import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.domain.Vote;
import com.anonranker.domain.VoteSubmission;
import com.anonranker.domain.VotingRule;
import com.anonranker.repository.MemberRepository;
import com.anonranker.repository.VoteRepository;
import com.anonranker.repository.VoteSubmissionRepository;
import com.anonranker.service.exception.AlreadyVotedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class VotingService {

    private final VoteRepository voteRepository;
    private final VoteSubmissionRepository voteSubmissionRepository;
    private final MemberRepository memberRepository;
    private final VotingRuleService votingRuleService;
    private final BallotValidator ballotValidator;

    public VotingService(VoteRepository voteRepository, VoteSubmissionRepository voteSubmissionRepository,
                          MemberRepository memberRepository, VotingRuleService votingRuleService,
                          BallotValidator ballotValidator) {
        this.voteRepository = voteRepository;
        this.voteSubmissionRepository = voteSubmissionRepository;
        this.memberRepository = memberRepository;
        this.votingRuleService = votingRuleService;
        this.ballotValidator = ballotValidator;
    }

    @Transactional(readOnly = true)
    public boolean hasSubmitted(Topic topic, Member voter) {
        return voteSubmissionRepository.existsByTopicAndVoterMember(topic, voter);
    }

    @Transactional
    public void castBallot(Session session, Topic topic, Member voter, List<Long> candidateMemberIds) {
        if (hasSubmitted(topic, voter)) {
            throw new AlreadyVotedException();
        }

        VotingRule rule = votingRuleService.getRule(session);
        List<Member> sessionMembers = memberRepository.findBySessionOrderByDisplayOrderAsc(session);
        ballotValidator.validate(rule, voter, sessionMembers, candidateMemberIds);

        for (Long candidateId : candidateMemberIds) {
            Member candidate = sessionMembers.stream()
                    .filter(m -> m.getId().equals(candidateId))
                    .findFirst()
                    .orElseThrow();
            voteRepository.save(new Vote(topic, voter, candidate));
        }
        voteSubmissionRepository.save(new VoteSubmission(topic, voter));
    }
}
