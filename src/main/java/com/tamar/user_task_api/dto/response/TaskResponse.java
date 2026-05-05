package com.tamar.user_task_api.dto.response;

import com.tamar.user_task_api.entity.TaskStatus;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Long userId
) {
}
