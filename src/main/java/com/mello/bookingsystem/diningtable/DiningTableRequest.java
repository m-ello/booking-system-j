package com.mello.bookingsystem.diningtable;

import jakarta.validation.constraints.*;

public record DiningTableRequest(
        @NotBlank @Size(max = 100) String label,
        @Size(max = 500) String description,
        @Min(1) @Max(50) int seats,
        @NotNull TableZone zone
) {}