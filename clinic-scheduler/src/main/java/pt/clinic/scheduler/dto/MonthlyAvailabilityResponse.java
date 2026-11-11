package pt.clinic.scheduler.dto;

import java.util.List;

public record MonthlyAvailabilityResponse(
        Long serviceId,
        int durationMinutes,
        List<DayAvailability> days
) {
}