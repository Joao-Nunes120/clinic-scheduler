package pt.clinic.scheduler.controller;

import java.time.YearMonth;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.clinic.scheduler.dto.BookingResponse;
import pt.clinic.scheduler.dto.CreateBookingRequest;
import pt.clinic.scheduler.dto.MonthlyAvailabilityResponse;
import pt.clinic.scheduler.execution.NodeTaskExecutor;
import pt.clinic.scheduler.service.AvailabilityService;
import pt.clinic.scheduler.service.BookingService;

@RestController
public class BookingController {

    private final AvailabilityService availabilityService;
    private final BookingService bookingService;
    private final NodeTaskExecutor nodeTaskExecutor;

    public BookingController(
            AvailabilityService availabilityService,
            BookingService bookingService,
            NodeTaskExecutor nodeTaskExecutor) {

        this.availabilityService = availabilityService;
        this.bookingService = bookingService;
        this.nodeTaskExecutor = nodeTaskExecutor;
    }

    @GetMapping("/availability")
    public CompletableFuture<MonthlyAvailabilityResponse> getAvailability(

            @RequestParam(name = "serviceId")
            @Positive
            Long serviceId,

            @RequestParam(name = "month")
            @DateTimeFormat(pattern = "uuuu-MM")
            YearMonth month,

            @RequestParam(name = "professionalId", required = false)
            @Positive
            Long professionalId) {

        return nodeTaskExecutor.submit(
                () -> availabilityService.getMonthlyAvailability(
                        serviceId,
                        month,
                        professionalId));
    }

    @PostMapping("/bookings")
    public CompletableFuture<ResponseEntity<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request) {

        return nodeTaskExecutor
                .submit(() -> bookingService.createBooking(request))
                .thenApply(response ->
                        ResponseEntity
                                .status(HttpStatus.CREATED)
                                .body(response));
    }
}