package com.tamar.user_task_api.repository;

import com.tamar.user_task_api.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
}
