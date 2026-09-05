package com.anonranker.service;

import com.anonranker.domain.Session;
import com.anonranker.domain.Topic;
import com.anonranker.repository.TopicRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<Topic> listTopics(Session session) {
        return topicRepository.findBySessionOrderByDisplayOrderAsc(session);
    }

    @Transactional(readOnly = true)
    public Topic getTopic(Session session, Long topicId) {
        return topicRepository.findByIdAndSession(topicId, session)
                .orElseThrow(() -> new EntityNotFoundException("Topic not found: " + topicId));
    }

    @Transactional
    public Topic addTopic(Session session, String prompt) {
        int nextOrder = topicRepository.findBySessionOrderByDisplayOrderAsc(session).size();
        return topicRepository.save(new Topic(session, prompt, nextOrder));
    }

    @Transactional
    public void updateTopic(Session session, Long topicId, String prompt) {
        Topic topic = getTopic(session, topicId);
        topic.setPrompt(prompt);
    }

    @Transactional
    public void deleteTopic(Session session, Long topicId) {
        Topic topic = getTopic(session, topicId);
        topicRepository.delete(topic);
    }
}
