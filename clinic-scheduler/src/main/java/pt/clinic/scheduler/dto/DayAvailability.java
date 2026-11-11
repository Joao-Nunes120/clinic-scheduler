package pt.clinic.scheduler.dto;

import java.time.LocalDate;
import java.util.List;

public record DayAvailability(
        LocalDate date,
        List<AvailabilitySlot> slots
) {
}