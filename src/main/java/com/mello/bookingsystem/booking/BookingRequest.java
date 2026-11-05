package com.mello.bookingsystem.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

// userId is TEMPORARY: in Step 6 it comes from the authenticated user instead
public record BookingRequest(
        @NotNull Long userId,
        @NotNull Long tableId,
        @NotNull Instant startTime,
        @NotNull Instant endTime,
        @Min(1) int partySize
) {}