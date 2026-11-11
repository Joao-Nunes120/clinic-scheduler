package pt.clinic.distributor.controller;

import java.net.http.HttpResponse;
import java.time.YearMonth;
import java.util.concurrent.CompletableFuture;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.constraints.Positive;
import pt.clinic.distributor.service.DistributionService;
import java.util.List;

@RestController
public class DistributorController {

    private final DistributionService distributionService;

    public DistributorController(
            DistributionService distributionService
    ) {
        this.distributionService = distributionService;
    }

    @GetMapping("/availability")
    public CompletableFuture<ResponseEntity<String>> getAvailability(
            @RequestParam(name = "serviceId")
            @Positive Long serviceId,

            @RequestParam(name = "month")
            @DateTimeFormat(pattern = "uuuu-MM") YearMonth month,

            @RequestParam(name = "professionalId", required = false)
            @Positive Long professionalId
    ) {
        String pathAndQuery = "/availability"
                + "?serviceId=" + serviceId
                + "&month=" + month;

        if (professionalId != null) {
            pathAndQuery += "&professionalId=" + professionalId;
        }

        return distributionService.get(pathAndQuery)
                .thenApply(this::toResponseEntity);
    }

    @PostMapping(
            value = "/bookings",
            consumes = MediaType.APPLICATION_JSON_VALUE
    )
    public CompletableFuture<ResponseEntity<String>> createBooking(
            @RequestBody String jsonBody
    ) {
        return distributionService.post("/bookings", jsonBody)
                .thenApply(this::toResponseEntity);
    }

    @GetMapping("/distributor/metrics")
    public DistributionService.Metrics getMetrics() {
        return distributionService.getMetrics();
    }

    private ResponseEntity<String> toResponseEntity(
            HttpResponse<String> nodeResponse
    ) {
        HttpHeaders headers = new HttpHeaders();

        nodeResponse.headers()
                .firstValue(HttpHeaders.CONTENT_TYPE)
                .ifPresent(contentType ->
                        headers.set(HttpHeaders.CONTENT_TYPE, contentType)
                );

        return ResponseEntity
                .status(nodeResponse.statusCode())
                .headers(headers)
                .body(nodeResponse.body());
    }

    @GetMapping("/distributor/nodes")
    public List<DistributionService.NodeMetrics> getNodeMetrics() {
        return distributionService.getNodeMetrics();
    }
}