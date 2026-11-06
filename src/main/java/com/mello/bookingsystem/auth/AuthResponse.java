package com.mello.bookingsystem.auth;

public record AuthResponse(String accessToken, String tokenType, long expiresInSeconds) {}