package com.mello.bookingsystem.booking;

import java.time.Instant;

public record BookingResponse(
        Long id,
        Long userId,
        Long tableId,
        String tableLabel,
        Instant startTime,
        Instant endTime,
        int partySize,
        BookingStatus status)
{
    static BookingResponse from(Booking b) {
        return new BookingResponse(b.getId(), b.getUser().getId(), b.getTable().getId(),
                b.getTable().getLabel(), b.getStartTime(), b.getEndTime(),
                b.getPartySize(), b.getStatus());
    }
}