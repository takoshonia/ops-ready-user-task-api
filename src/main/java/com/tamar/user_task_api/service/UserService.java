package com.tamar.user_task_api.service;

import com.tamar.user_task_api.dto.request.UserCreateRequest;
import com.tamar.user_task_api.dto.response.UserResponse;
import com.tamar.user_task_api.entity.User;
import com.tamar.user_task_api.exception.ConflictException;
import com.tamar.user_task_api.exception.ResourceNotFoundException;
import com.tamar.user_task_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;

    public UserResponse create(UserCreateRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists: " + request.email());
        }
        User user = new User();
        user.setName(request.name());
        user.setEmail(request.email());
        User saved = userRepository.save(user);
        log.info("event=user_create requestId={} userId={} email={}",
                MDC.get("requestId"), saved.getId(), saved.getEmail());
        return toResponse(saved);
    }

    public List<UserResponse> findAll() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public UserResponse findById(Long id) {
        return toResponse(getUserEntityById(id));
    }

    public UserResponse update(Long id, UserCreateRequest request) {
        User existingUser = getUserEntityById(id);
        if (userRepository.existsByEmailAndIdNot(request.email(), id)) {
            throw new ConflictException("Email already exists: " + request.email());
        }
        existingUser.setName(request.name());
        existingUser.setEmail(request.email());
        User saved = userRepository.save(existingUser);
        log.info("event=user_update requestId={} userId={} email={}",
                MDC.get("requestId"), saved.getId(), saved.getEmail());
        return toResponse(saved);
    }

    public void delete(Long id) {
        User existingUser = getUserEntityById(id);
        userRepository.delete(existingUser);
        log.info("event=user_delete requestId={} userId={}", MDC.get("requestId"), id);
    }

    public User getUserEntityById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}
