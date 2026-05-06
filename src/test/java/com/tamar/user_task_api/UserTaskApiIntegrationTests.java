package com.tamar.user_task_api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserTaskApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void userCrudFlow_shouldCreateReadUpdateListAndDelete() throws Exception {
        Long userId = createUser("Tamar One", "tamar.one@example.com");

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Tamar One"))
                .andExpect(jsonPath("$.email").value("tamar.one@example.com"));

        mockMvc.perform(put("/api/users/{id}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Tamar Updated",
                                  "email": "tamar.updated@example.com"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Tamar Updated"))
                .andExpect(jsonPath("$.email").value("tamar.updated@example.com"));

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/users/{id}", userId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/users/{id}", userId))
                .andExpect(status().isNotFound());
    }

    @Test
    void taskCrudFlow_shouldCreateReadUpdateListAndDelete() throws Exception {
        Long userId = createUser("Task Owner", "task.owner@example.com");
        Long taskId = createTask("Initial title", "Initial description", "TODO", userId);

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("Initial title"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.userId").value(userId));

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated title",
                                  "description": "Updated description",
                                  "status": "IN_PROGRESS",
                                  "userId": %d
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(taskId))
                .andExpect(jsonPath("$.title").value("Updated title"))
                .andExpect(jsonPath("$.description").value("Updated description"))
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.userId").value(userId));

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated title",
                                  "description": "Updated description",
                                  "status": "DONE",
                                  "userId": %d
                                }
                                """.formatted(userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"));

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(delete("/api/tasks/{id}", taskId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/{id}", taskId))
                .andExpect(status().isNotFound());
    }

    @Test
    void createUser_withInvalidPayload_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors", hasKey("name")))
                .andExpect(jsonPath("$.validationErrors", hasKey("email")));
    }

    @Test
    void createTask_withInvalidPayload_shouldReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "x",
                                  "status": null,
                                  "userId": null
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.validationErrors", hasKey("title")))
                .andExpect(jsonPath("$.validationErrors", hasKey("status")))
                .andExpect(jsonPath("$.validationErrors", hasKey("userId")));
    }

    @Test
    void getMissingUser_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/users/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99999"));
    }

    @Test
    void getMissingTask_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/tasks/{id}", 99999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Task not found with id: 99999"));
    }

    @Test
    void createTask_forMissingUser_shouldReturnNotFound() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Missing user task",
                                  "description": "desc",
                                  "status": "TODO",
                                  "userId": 99999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with id: 99999"));
    }

    @Test
    void createUser_withDuplicateEmail_shouldFail() throws Exception {
        createUser("First User", "duplicate@example.com");

        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Second User",
                                  "email": "duplicate@example.com"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONFLICT"))
                .andExpect(jsonPath("$.message").value("Email already exists: duplicate@example.com"))
                .andExpect(jsonPath("$.path").value("/api/users"))
                .andExpect(jsonPath("$.requestId").isNotEmpty());
    }

    @Test
    void updateTask_withInvalidStatusTransition_shouldReturnUnprocessableEntity() throws Exception {
        Long userId = createUser("Status User", "status.user@example.com");
        Long taskId = createTask("Transition task", "desc", "TODO", userId);

        mockMvc.perform(put("/api/tasks/{id}", taskId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Transition task",
                                  "description": "desc",
                                  "status": "DONE",
                                  "userId": %d
                                }
                                """.formatted(userId)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value("BUSINESS_RULE_VIOLATION"))
                .andExpect(jsonPath("$.message").value("Invalid task status transition: TODO -> DONE"));
    }

    @Test
    void createUser_withSameIdempotencyKey_shouldReturnCachedResponse() throws Exception {
        String payload = """
                {
                  "name": "Idempotent User",
                  "email": "idempotent@example.com"
                }
                """;

        MvcResult first = mockMvc.perform(post("/api/users")
                        .header("Idempotency-Key", "idem-user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        MvcResult second = mockMvc.perform(post("/api/users")
                        .header("Idempotency-Key", "idem-user-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        Long firstId = getIdFromResponse(first);
        Long secondId = getIdFromResponse(second);

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        org.junit.jupiter.api.Assertions.assertEquals(firstId, secondId);
    }

    private Long createUser(String name, String email) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "email": "%s"
                                }
                                """.formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn();

        return getIdFromResponse(mvcResult);
    }

    private Long createTask(String title, String description, String status, Long userId) throws Exception {
        MvcResult mvcResult = mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "%s",
                                  "description": "%s",
                                  "status": "%s",
                                  "userId": %d
                                }
                                """.formatted(title, description, status, userId)))
                .andExpect(status().isCreated())
                .andReturn();

        return getIdFromResponse(mvcResult);
    }

    private Long getIdFromResponse(MvcResult mvcResult) throws Exception {
        JsonNode root = objectMapper.readTree(mvcResult.getResponse().getContentAsString());
        return root.get("id").asLong();
    }
}
