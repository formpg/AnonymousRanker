package com.anonranker.web.dto;

import jakarta.validation.constraints.NotBlank;

public class TopicForm {

    @NotBlank(message = "お題を入力してください")
    private String prompt;

    public TopicForm() {
    }

    public TopicForm(String prompt) {
        this.prompt = prompt;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
