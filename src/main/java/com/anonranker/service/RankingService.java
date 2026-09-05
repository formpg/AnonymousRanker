package com.anonranker.service;

import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.domain.VotingRule;
import com.anonranker.repository.VoteRepository;
import com.anonranker.web.dto.RankEntryDto;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns raw {@link com.anonranker.domain.Vote} rows into an aggregated,
 * anonymity-safe ranking - only candidate name + vote count ever leave this
 * class, never who cast a given vote.
 */
@Service
public class RankingService {

    private final VoteRepository voteRepository;

    public RankingService(VoteRepository voteRepository) {
        this.voteRepository = voteRepository;
    }

    @Transactional(readOnly = true)
    public List<RankEntryDto> computeRanking(Session session, Topic topic, VotingRule rule) {
        Map<Long, Long> countsByMemberId = new HashMap<>();
        for (Member member : session.getMembers()) {
            countsByMemberId.put(member.getId(), 0L);
        }
        for (VoteRepository.CandidateVoteCount row : voteRepository.countVotesByCandidate(topic)) {
            countsByMemberId.put(row.getCandidate().getId(), row.getVoteCount());
        }

        List<Member> ordered = new ArrayList<>(session.getMembers());
        ordered.sort(
                Comparator.<Member>comparingLong(m -> countsByMemberId.get(m.getId()))
                        .reversed()
                        .thenComparing(Member::getName)
        );

        List<RankEntryDto> result = new ArrayList<>();
        int rank = 0;
        long previousCount = -1;
        int position = 0;
        for (Member member : ordered) {
            position++;
            long count = countsByMemberId.get(member.getId());
            if (count != previousCount) {
                rank = position;
                previousCount = count;
            }
            if (rank > rule.getTopN()) {
                break;
            }
            result.add(new RankEntryDto(rank, member.getName(), rule.isRevealCounts() ? count : null));
        }
        return result;
    }
}
