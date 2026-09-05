package com.anonranker.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public class CreateSessionForm {

    @NotBlank(message = "タイトルを入力してください")
    private String title;

    @NotBlank(message = "グループIDを入力してください")
    private String groupId;

    @NotNull(message = "開催日を入力してください")
    private LocalDate eventDate;

    @NotBlank(message = "パスワードを入力してください")
    private String password;

    @NotBlank(message = "メンバーを1人以上入力してください")
    private String memberNames = "";

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public void setEventDate(LocalDate eventDate) {
        this.eventDate = eventDate;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Newline-separated member names, as typed into a single textarea.
     */
    public String getMemberNames() {
        return memberNames;
    }

    public void setMemberNames(String memberNames) {
        this.memberNames = memberNames;
    }
}
