package com.anonranker.web.dto;

import java.util.ArrayList;
import java.util.List;

public class BallotForm {

    private List<Long> candidateMemberIds = new ArrayList<>();

    public List<Long> getCandidateMemberIds() {
        return candidateMemberIds;
    }

    public void setCandidateMemberIds(List<Long> candidateMemberIds) {
        this.candidateMemberIds = candidateMemberIds;
    }
}
