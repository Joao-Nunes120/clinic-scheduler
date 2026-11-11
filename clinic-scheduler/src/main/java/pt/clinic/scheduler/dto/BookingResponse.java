package pt.clinic.scheduler.dto;

import java.time.LocalDateTime;

public record BookingResponse(
        Long bookingId,
        String clientName,
        String clientEmail,
        Long serviceId,
        String serviceName,
        LocalDateTime startTime,
        LocalDateTime endTime,
        Long professionalId,
        String professionalName
) {
}