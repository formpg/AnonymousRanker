package com.anonranker.service;

import com.anonranker.domain.Member;
import com.anonranker.domain.VotingRule;
import com.anonranker.service.exception.InvalidBallotException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared eligibility/count checks for a ballot, used by both the real voting
 * flow and the preview flow so the two can never drift apart.
 */
@Component
public class BallotValidator {

    public void validate(VotingRule rule, Member voter, List<Member> sessionMembers, List<Long> candidateMemberIds) {
        if (candidateMemberIds == null || candidateMemberIds.size() != rule.getVotesPerTopic()) {
            throw new InvalidBallotException("持ち票数と同じ人数を選択してください（%d人）".formatted(rule.getVotesPerTopic()));
        }

        Set<Long> distinct = new HashSet<>(candidateMemberIds);
        if (distinct.size() != candidateMemberIds.size()) {
            throw new InvalidBallotException("同じ人に複数投票することはできません");
        }

        Set<Long> validMemberIds = sessionMembers.stream().map(Member::getId).collect(java.util.stream.Collectors.toSet());
        for (Long candidateId : candidateMemberIds) {
            if (!validMemberIds.contains(candidateId)) {
                throw new InvalidBallotException("このグループのメンバーではない候補が含まれています");
            }
            if (!rule.isAllowSelfVote() && candidateId.equals(voter.getId())) {
                throw new InvalidBallotException("このお題では自分自身に投票することはできません");
            }
        }
    }
}
