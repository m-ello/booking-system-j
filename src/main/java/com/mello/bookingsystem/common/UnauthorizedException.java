package com.mello.bookingsystem.common;

public class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) { super(message); }
}