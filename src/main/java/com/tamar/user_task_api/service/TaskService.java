package com.tamar.user_task_api.service;

import com.tamar.user_task_api.dto.request.TaskCreateRequest;
import com.tamar.user_task_api.dto.response.TaskResponse;
import com.tamar.user_task_api.entity.Task;
import com.tamar.user_task_api.entity.TaskStatus;
import com.tamar.user_task_api.entity.User;
import com.tamar.user_task_api.exception.BusinessRuleViolationException;
import com.tamar.user_task_api.exception.ResourceNotFoundException;
import com.tamar.user_task_api.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;
    private final UserService userService;

    public TaskResponse create(TaskCreateRequest request) {
        User user = userService.getUserEntityById(request.userId());

        Task task = new Task();
        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status());
        task.setUser(user);

        Task saved = taskRepository.save(task);
        log.info("event=task_create requestId={} taskId={} status={} userId={}",
                MDC.get("requestId"), saved.getId(), saved.getStatus(), saved.getUser().getId());
        return toResponse(saved);
    }

    public List<TaskResponse> findAll() {
        return taskRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public TaskResponse findById(Long id) {
        return toResponse(getTaskEntityById(id));
    }

    public TaskResponse update(Long id, TaskCreateRequest request) {
        Task existingTask = getTaskEntityById(id);
        User user = userService.getUserEntityById(request.userId());
        validateStatusTransition(existingTask.getStatus(), request.status());

        existingTask.setTitle(request.title());
        existingTask.setDescription(request.description());
        existingTask.setStatus(request.status());
        existingTask.setUser(user);

        Task saved = taskRepository.save(existingTask);
        log.info("event=task_update requestId={} taskId={} status={} userId={}",
                MDC.get("requestId"), saved.getId(), saved.getStatus(), saved.getUser().getId());
        return toResponse(saved);
    }

    public void delete(Long id) {
        Task existingTask = getTaskEntityById(id);
        taskRepository.delete(existingTask);
        log.info("event=task_delete requestId={} taskId={}", MDC.get("requestId"), id);
    }

    private Task getTaskEntityById(Long id) {
        return taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task not found with id: " + id));
    }

    private TaskResponse toResponse(Task task) {
        return new TaskResponse(
                task.getId(),
                task.getTitle(),
                task.getDescription(),
                task.getStatus(),
                task.getUser().getId(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }

    private void validateStatusTransition(TaskStatus current, TaskStatus next) {
        if (current == next) {
            return;
        }
        boolean isValid = (current == TaskStatus.TODO && next == TaskStatus.IN_PROGRESS)
                || (current == TaskStatus.IN_PROGRESS && next == TaskStatus.DONE);
        if (!isValid) {
            throw new BusinessRuleViolationException(
                    "Invalid task status transition: " + current + " -> " + next
            );
        }
    }
}
