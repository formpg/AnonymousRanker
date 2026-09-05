package com.anonranker.web.dto;

/**
 * Anonymity-safe voting progress for the admin dashboard: how many of the
 * session's members have submitted a ballot for a topic, with no indication
 * of which members those are or how they voted.
 */
public record SubmissionProgressDto(Long topicId, String topicPrompt, long submittedCount, long totalMembers) {
}
