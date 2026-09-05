package com.anonranker.repository;

import com.anonranker.domain.Member;
import com.anonranker.domain.Topic;
import com.anonranker.domain.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    /**
     * Aggregated candidate vote counts for a topic. Deliberately never joins
     * back to the voter - only candidate identity and a count are exposed.
     */
    @Query("""
            select v.candidateMember as candidate, count(v) as voteCount
            from Vote v
            where v.topic = :topic
            group by v.candidateMember
            """)
    List<CandidateVoteCount> countVotesByCandidate(@Param("topic") Topic topic);

    List<Vote> findByTopicAndVoterMember(Topic topic, Member voterMember);

    void deleteByTopicAndVoterMember(Topic topic, Member voterMember);

    interface CandidateVoteCount {
        Member getCandidate();

        long getVoteCount();
    }
}
