package com.anonranker.repository;

import com.anonranker.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * {@code members} is fetched eagerly here (via JOIN FETCH) because callers
     * routinely read {@code session.getMembers()} well after this read-only
     * transaction has closed (open-in-view is disabled) - without eager
     * loading it would throw LazyInitializationException.
     */
    @Query("select s from Session s left join fetch s.members where s.adminToken = :adminToken")
    Optional<Session> findByAdminToken(@Param("adminToken") String adminToken);

    @Query("select s from Session s left join fetch s.members where s.votingToken = :votingToken")
    Optional<Session> findByVotingToken(@Param("votingToken") String votingToken);
}
