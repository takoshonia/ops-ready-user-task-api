package com.tamar.user_task_api.repository;

import com.tamar.user_task_api.entity.Task;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskRepository extends JpaRepository<Task, Long> {
}
