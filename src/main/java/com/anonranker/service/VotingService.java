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
    public List<Long> getExistingCandidateIds(Topic topic, Member voter) {
        return voteRepository.findByTopicAndVoterMember(topic, voter).stream()
                .map(vote -> vote.getCandidateMember().getId())
                .toList();
    }

    /**
     * Casts (or overwrites) a member's ballot for a topic. Voting is
     * intentionally re-submittable - a fresh call always replaces whatever
     * that member previously chose for this topic, rather than being
     * rejected as a duplicate.
     */
    @Transactional
    public void upsertBallot(Session session, Topic topic, Member voter, List<Long> candidateMemberIds) {
        VotingRule rule = votingRuleService.getRule(session);
        List<Member> sessionMembers = memberRepository.findBySessionOrderByDisplayOrderAsc(session);
        ballotValidator.validate(rule, voter, sessionMembers, candidateMemberIds);

        // Hibernate flushes pending inserts before pending deletes regardless of call
        // order, so without an explicit flush here, re-inserting a row with the same
        // natural key (e.g. re-picking the same candidate, or just re-submitting the
        // same topic) would violate the unique constraint before the old row is gone.
        voteRepository.deleteByTopicAndVoterMember(topic, voter);
        voteSubmissionRepository.deleteByTopicAndVoterMember(topic, voter);
        voteRepository.flush();
        voteSubmissionRepository.flush();

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
