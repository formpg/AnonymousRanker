package com.anonranker.web.dto;

import com.anonranker.domain.Member;

import java.util.List;

/**
 * One topic's row on the single-page ballot: its eligible candidates, the
 * voter's current pick per slot (padded to {@code votesPerTopic} length,
 * {@code null} where unset so the template can safely index by slot), and
 * whether it's locked because the organizer has already started announcing
 * it live.
 */
public record TopicBallotView(
        Long topicId,
        String prompt,
        List<Member> candidates,
        List<Long> slotValues,
        boolean locked,
        String error
) {
}
