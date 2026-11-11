package pt.clinic.scheduler.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.clinic.scheduler.domain.Booking;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    List<Booking> findAllByStartTimeLessThanAndEndTimeGreaterThan(
            LocalDateTime end,
            LocalDateTime start
    );
}