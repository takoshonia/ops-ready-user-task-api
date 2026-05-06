package com.tamar.user_task_api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tamar.user_task_api.exception.ConflictException;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

@Service
public class IdempotencyService {

    private final ObjectMapper objectMapper;
    private final Map<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    public IdempotencyService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public <T> ResponseEntity<T> execute(String operation,
                                         String idempotencyKey,
                                         Object requestPayload,
                                         Supplier<ResponseEntity<T>> action) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return action.get();
        }

        String recordKey = operation + ":" + idempotencyKey.trim();
        String fingerprint = toFingerprint(requestPayload);
        IdempotencyRecord existing = store.get(recordKey);

        if (existing != null) {
            if (!existing.fingerprint().equals(fingerprint)) {
                throw new ConflictException("Idempotency key reused with different payload");
            }
            @SuppressWarnings("unchecked")
            ResponseEntity<T> cached = (ResponseEntity<T>) existing.responseEntity();
            return cached;
        }

        ResponseEntity<T> current = action.get();
        store.put(recordKey, new IdempotencyRecord(fingerprint, current));
        return current;
    }

    private String toFingerprint(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to compute idempotency fingerprint", ex);
        }
    }

    private record IdempotencyRecord(String fingerprint, ResponseEntity<?> responseEntity) {
    }
}
