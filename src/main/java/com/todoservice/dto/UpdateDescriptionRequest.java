package com.todoservice.dto;

import jakarta.validation.constraints.NotBlank;

public class UpdateDescriptionRequest {

    @NotBlank(message = "Description must not be blank")
    private String description;

    public UpdateDescriptionRequest() {
    }

    public UpdateDescriptionRequest(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
