package com.anonranker.repository;

import com.anonranker.domain.Member;
import com.anonranker.domain.Topic;
import com.anonranker.domain.VoteSubmission;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VoteSubmissionRepository extends JpaRepository<VoteSubmission, Long> {
    boolean existsByTopicAndVoterMember(Topic topic, Member voterMember);

    void deleteByTopicAndVoterMember(Topic topic, Member voterMember);

    long countByTopic(Topic topic);
}
