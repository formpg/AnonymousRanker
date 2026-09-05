package com.anonranker.service;

import com.anonranker.web.dto.RankEntryDto;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * In-memory, server-authoritative progress of a live announcement, shared
 * between the organizer's admin screen (which drives it) and any number of
 * voters' read-only "watch" screens (which poll it) so everyone sees the
 * same reveal sequence together. Deliberately not persisted - a live
 * announcement is a one-off presentation moment tied to the current server
 * process, not durable state that needs to survive a restart.
 */
@Service
public class AnnouncementStateService {

    private final Map<Long, Long> currentTopicIdBySessionId = new ConcurrentHashMap<>();
    private final Map<Long, List<RankEntryDto>> revealedByTopicId = new ConcurrentHashMap<>();

    public void startTopic(Long sessionId, Long topicId) {
        currentTopicIdBySessionId.put(sessionId, topicId);
        revealedByTopicId.put(topicId, new CopyOnWriteArrayList<>());
    }

    public synchronized RankEntryDto revealNext(Long topicId, List<RankEntryDto> fullSequence) {
        List<RankEntryDto> revealed = revealedByTopicId.computeIfAbsent(topicId, id -> new CopyOnWriteArrayList<>());
        if (revealed.size() >= fullSequence.size()) {
            return null;
        }
        RankEntryDto next = fullSequence.get(revealed.size());
        revealed.add(next);
        return next;
    }

    public Long getCurrentTopicId(Long sessionId) {
        return currentTopicIdBySessionId.get(sessionId);
    }

    public List<RankEntryDto> getRevealed(Long topicId) {
        return revealedByTopicId.getOrDefault(topicId, List.of());
    }

    public boolean hasAnyReveal(Long topicId) {
        return !getRevealed(topicId).isEmpty();
    }
}
