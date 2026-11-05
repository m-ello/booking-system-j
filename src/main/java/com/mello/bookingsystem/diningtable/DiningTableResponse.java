package com.mello.bookingsystem.diningtable;

public record DiningTableResponse(
        Long id,
        String label,
        String description,
        int seats,
        TableZone zone,
        boolean active
) {
    static DiningTableResponse from(DiningTable t) {
        return new DiningTableResponse(t.getId(), t.getLabel(), t.getDescription(),
                t.getSeats(), t.getZone(), t.isActive());
    }
}