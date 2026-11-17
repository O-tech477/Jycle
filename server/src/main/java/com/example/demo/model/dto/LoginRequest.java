package com.example.demo.model.dto;

public record LoginRequest(
    String email,
    String name,
    String googleToken
) {}
