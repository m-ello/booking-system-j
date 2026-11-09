package com.mello.bookingsystem.booking;

import java.time.Instant;

public record BookingFilter(BookingStatus status, Long tableId, String email, Instant from, Instant to) {}