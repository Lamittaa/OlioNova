package com.project.queue_service.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.project.queue_service.model.QueueType;

@Service
public class TellerSessionService {

    private final Map<QueueType, Set<Long>> activeTellers = new HashMap<>();

    // 🟢 login
    public void login(Long userId, QueueType queueType) {
        activeTellers
                .computeIfAbsent(queueType, k -> new HashSet<>())
                .add(userId);
    }

    // 🔴 logout
    public void logout(Long userId, QueueType queueType) {
        Set<Long> tellers = activeTellers.get(queueType);
        if (tellers != null) {
            tellers.remove(userId);
        }
    }

    // 🔥 check
    public boolean isActive(Long userId, QueueType queueType) {
        return activeTellers
                .getOrDefault(queueType, Collections.emptySet())
                .contains(userId);
    }
}