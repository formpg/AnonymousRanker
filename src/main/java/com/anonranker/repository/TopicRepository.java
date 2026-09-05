package com.anonranker.repository;

import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findBySessionOrderByDisplayOrderAsc(Session session);

    Optional<Topic> findByIdAndSession(Long id, Session session);
}
