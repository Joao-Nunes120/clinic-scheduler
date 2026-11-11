package pt.clinic.scheduler.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import pt.clinic.scheduler.domain.Booking;
import pt.clinic.scheduler.domain.Professional;
import pt.clinic.scheduler.domain.Room;

@Component
public class ResourceSelectionPolicy {

    public Room selectRoom(
            List<Room> freeRooms,
            List<Booking> dayBookings,
            LocalDate date) {

        Map<Long, Long> occupiedMinutes = new HashMap<>();

        for (Booking booking : dayBookings) {
            occupiedMinutes.merge(
                    booking.getRoom().getId(),
                    minutesWithinDay(booking, date),
                    Long::sum);
        }

        return freeRooms.stream()
                .min(Comparator
                        .comparingLong((Room room) ->
                                occupiedMinutes.getOrDefault(
                                        room.getId(), 0L))
                        .thenComparing(Room::getId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Não existem salas livres para selecionar."));
    }

    public Professional selectProfessional(
            List<Professional> freeProfessionals,
            List<Booking> dayBookings,
            LocalDate date) {

        Map<Long, Long> occupiedMinutes = new HashMap<>();

        for (Booking booking : dayBookings) {
            occupiedMinutes.merge(
                    booking.getProfessional().getId(),
                    minutesWithinDay(booking, date),
                    Long::sum);
        }

        return freeProfessionals.stream()
                .min(Comparator
                        .comparingLong((Professional professional) ->
                                occupiedMinutes.getOrDefault(
                                        professional.getId(), 0L))
                        .thenComparing(Professional::getId))
                .orElseThrow(() -> new IllegalArgumentException(
                        "Não existem profissionais livres para selecionar."));
    }

    private long minutesWithinDay(
            Booking booking,
            LocalDate date) {

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        LocalDateTime start = booking.getStartTime();
        LocalDateTime end = booking.getEndTime();

        if (start.isBefore(dayStart)) {
            start = dayStart;
        }

        if (end.isAfter(dayEnd)) {
            end = dayEnd;
        }

        if (!start.isBefore(end)) {
            return 0;
        }

        return Duration.between(start, end).toMinutes();
    }
}