package pt.clinic.scheduler.dto;

import java.time.LocalDateTime;

public record AvailabilitySlot(
        LocalDateTime start,
        LocalDateTime end
) {
}