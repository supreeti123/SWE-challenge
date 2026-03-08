package com.todoservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request payload for updating the description of an existing todo item.
 */
public record UpdateDescriptionRequest(
        @NotBlank(message = "Description must not be blank")
        @Size(max = 500, message = "Description must be at most 500 characters")
        String description) {
}
