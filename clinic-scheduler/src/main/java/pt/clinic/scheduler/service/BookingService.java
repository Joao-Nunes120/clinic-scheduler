package pt.clinic.scheduler.service;

import java.sql.SQLException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import pt.clinic.scheduler.dto.BookingResponse;
import pt.clinic.scheduler.dto.CreateBookingRequest;
import pt.clinic.scheduler.exception.BookingConflictException;

@Service
public class BookingService {

    private static final int MAX_ATTEMPTS = 3;

    private static final String EXCLUSION_VIOLATION = "23P01";

    private final BookingAttemptService bookingAttemptService;

    public BookingService(
            BookingAttemptService bookingAttemptService) {

        this.bookingAttemptService = bookingAttemptService;
    }

    public BookingResponse createBooking(
            CreateBookingRequest request) {

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {

            try {
                return bookingAttemptService.attemptBooking(request);

            } catch (DataIntegrityViolationException exception) {

                if (!isOverlapConflict(exception)) {
                    throw exception;
                }

                // A transação da tentativa já foi revertida.
                // A próxima chamada volta a consultar os recursos.
            }
        }

        throw new BookingConflictException(
                "Não foi possível confirmar a reserva devido "
                        + "a conflitos com outras marcações.");
    }

    private boolean isOverlapConflict(Throwable exception) {

        Throwable cause = exception;

        while (cause != null) {

            if (cause instanceof SQLException sqlException
                    && EXCLUSION_VIOLATION.equals(
                            sqlException.getSQLState())) {

                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}