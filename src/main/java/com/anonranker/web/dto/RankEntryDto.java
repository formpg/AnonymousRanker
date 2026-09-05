package com.anonranker.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * One row of an announced ranking. {@code count} is {@code null} whenever the
 * session's voting rule has vote-count reveal turned off; {@code @JsonInclude}
 * drops null fields from serialization so the value is never actually sent to
 * the browser in that case, not merely hidden by the UI.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RankEntryDto {

    private final int rank;
    private final String name;
    private final Long count;

    public RankEntryDto(int rank, String name, Long count) {
        this.rank = rank;
        this.name = name;
        this.count = count;
    }

    public int getRank() {
        return rank;
    }

    public String getName() {
        return name;
    }

    public Long getCount() {
        return count;
    }
}
