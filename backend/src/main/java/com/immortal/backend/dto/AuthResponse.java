package com.immortal.backend.dto;

public record AuthResponse(String token, Long id, String username, Integer rating) {}
