package com.anonranker.repository;

import com.anonranker.domain.Member;
import com.anonranker.domain.Session;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    List<Member> findBySessionOrderByDisplayOrderAsc(Session session);

    Optional<Member> findByIdAndSession(Long id, Session session);

    long countBySession(Session session);
}
