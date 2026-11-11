package pt.clinic.scheduler.exception;

import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.transaction.TransactionTimedOutException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(BookingConflictException.class)
    public ResponseEntity<ProblemDetail> handleBookingConflict(
            BookingConflictException exception) {

        return error(
                HttpStatus.CONFLICT,
                exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleInvalidArgument(
            IllegalArgumentException exception) {

        return error(
                HttpStatus.BAD_REQUEST,
                exception.getMessage());
    }

    @ExceptionHandler(RejectedExecutionException.class)
    public ResponseEntity<ProblemDetail> handleOverload(
            RejectedExecutionException exception) {

        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "O nó está sobrecarregado e não aceitou o pedido.");
    }

    @ExceptionHandler({
        DataAccessResourceFailureException.class,
        CannotCreateTransactionException.class,
        QueryTimeoutException.class,
        TransactionTimedOutException.class,
        PessimisticLockingFailureException.class
    })
    public ResponseEntity<ProblemDetail> handleDatabaseUnavailable(
            Exception exception) {

        return error(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Não foi possível confirmar o resultado da operação "
                        + "devido a indisponibilidade temporária.");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnexpectedError(
            Exception exception) {

        LOGGER.error("Erro inesperado ao processar o pedido", exception);

        return error(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno ao processar o pedido.");
    }

    private ResponseEntity<ProblemDetail> error(
            HttpStatus status,
            String message) {

        ProblemDetail problem =
                ProblemDetail.forStatusAndDetail(status, message);

        return ResponseEntity.status(status).body(problem);
    }
}