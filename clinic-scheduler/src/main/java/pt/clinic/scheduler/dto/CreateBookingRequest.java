package pt.clinic.scheduler.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateBookingRequest(

        @NotNull
        Long serviceId,

        @NotNull
        @Future
        LocalDateTime startTime,

        @NotBlank
        String clientName,

        @NotBlank
        @Email
        String clientEmail,

        Long professionalId
) {
}