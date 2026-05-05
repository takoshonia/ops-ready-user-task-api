package com.tamar.user_task_api.dto.response;

public record UserResponse(
        Long id,
        String name,
        String email
) {
}
