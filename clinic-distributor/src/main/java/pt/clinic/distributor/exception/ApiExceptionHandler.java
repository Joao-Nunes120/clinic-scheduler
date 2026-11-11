package pt.clinic.distributor.exception;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log =
            LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleFailure(Exception exception) {
        Throwable cause = unwrap(exception);

        if (cause instanceof RejectedExecutionException) {
            return problem(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "A fila do distribuidor está cheia ou o executor está encerrado."
            );
        }

        if (cause instanceof HttpTimeoutException) {
            return problem(
                    HttpStatus.GATEWAY_TIMEOUT,
                    "O nó não respondeu dentro do prazo. "
                            + "Se pediu uma reserva, o resultado pode ser desconhecido."
            );
        }

        if (cause instanceof IOException) {
            return problem(
                    HttpStatus.BAD_GATEWAY,
                    "Ocorreu uma falha de comunicação com o nó. "
                            + "Se pediu uma reserva, o resultado pode ser desconhecido."
            );
        }

        log.error("Erro inesperado no distribuidor.", exception);

        return problem(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocorreu um erro interno no distribuidor."
        );
    }

    @Override
    protected ResponseEntity<Object> handleAsyncRequestTimeoutException(
            AsyncRequestTimeoutException exception,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request
    ) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(
                HttpStatus.GATEWAY_TIMEOUT,
                "O prazo de espera no distribuidor terminou. "
                        + "O processamento pode continuar e o resultado "
                        + "de uma reserva pode ser desconhecido."
        );

        return handleExceptionInternal(
                exception,
                body,
                headers,
                HttpStatus.GATEWAY_TIMEOUT,
                request
        );
    }

    private Throwable unwrap(Throwable exception) {
        Throwable current = exception;

        while ((current instanceof CompletionException
                || current instanceof ExecutionException)
                && current.getCause() != null) {
            current = current.getCause();
        }

        return current;
    }

    private ResponseEntity<ProblemDetail> problem(
            HttpStatus status,
            String detail
    ) {
        ProblemDetail body =
                ProblemDetail.forStatusAndDetail(status, detail);

        return ResponseEntity.status(status).body(body);
    }
}