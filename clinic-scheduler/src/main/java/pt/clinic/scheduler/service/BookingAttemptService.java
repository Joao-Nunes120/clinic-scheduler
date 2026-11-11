package pt.clinic.scheduler.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import pt.clinic.scheduler.domain.Booking;
import pt.clinic.scheduler.domain.ClinicService;
import pt.clinic.scheduler.domain.Equipment;
import pt.clinic.scheduler.domain.EquipmentType;
import pt.clinic.scheduler.domain.Professional;
import pt.clinic.scheduler.domain.Room;
import pt.clinic.scheduler.dto.BookingResponse;
import pt.clinic.scheduler.dto.CreateBookingRequest;
import pt.clinic.scheduler.exception.BookingConflictException;
import pt.clinic.scheduler.repository.BookingRepository;
import pt.clinic.scheduler.repository.ClinicServiceRepository;
import pt.clinic.scheduler.repository.ProfessionalRepository;
import pt.clinic.scheduler.repository.RoomRepository;

@Service
public class BookingAttemptService {

    private static final ZoneId CLINIC_ZONE =
            ZoneId.of("Europe/Lisbon");

    private static final Duration SLOT_STEP =
            Duration.ofMinutes(15);

    private final ClinicServiceRepository clinicServiceRepository;
    private final ProfessionalRepository professionalRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;
    private final ResourceSelectionPolicy selectionPolicy;

    public BookingAttemptService(
            ClinicServiceRepository clinicServiceRepository,
            ProfessionalRepository professionalRepository,
            RoomRepository roomRepository,
            BookingRepository bookingRepository,
            ResourceSelectionPolicy selectionPolicy) {

        this.clinicServiceRepository = clinicServiceRepository;
        this.professionalRepository = professionalRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
        this.selectionPolicy = selectionPolicy;
    }

    // Cada chamada executa uma tentativa numa transação independente.
    @Transactional(
            propagation = Propagation.REQUIRES_NEW,
            isolation = Isolation.READ_COMMITTED
    )
    public BookingResponse attemptBooking(
            CreateBookingRequest request) {

        validateRequest(request);

        ClinicService clinicService =
                findClinicService(request.serviceId());

        LocalDateTime start = request.startTime();
        LocalDateTime end = start.plusMinutes(
                clinicService.getDurationMinutes());

        LocalDate date = start.toLocalDate();

        List<Booking> dayBookings = findDayBookings(date);

        List<Professional> freeProfessionals =
                findFreeProfessionals(
                        clinicService.getId(),
                        request.professionalId(),
                        dayBookings,
                        start,
                        end);

        if (freeProfessionals.isEmpty()) {
            throw new BookingConflictException(
                    "Não existe profissional disponível para este horário.");
        }

        List<Room> freeRooms = findFreeRooms(
                clinicService.getRequiredEquipmentTypes(),
                dayBookings,
                start,
                end);

        if (freeRooms.isEmpty()) {
            throw new BookingConflictException(
                    "Não existe sala disponível para este horário.");
        }

        Room room = selectionPolicy.selectRoom(
                freeRooms, dayBookings, date);

        Professional professional =
                selectionPolicy.selectProfessional(
                        freeProfessionals, dayBookings, date);

        Booking booking = new Booking(
                request.clientName().trim(),
                request.clientEmail().trim(),
                start,
                end,
                clinicService,
                professional,
                room);

        // Executa o INSERT. As restrições SQL verificam os conflitos.
        Booking saved = bookingRepository.saveAndFlush(booking);

        return toResponse(saved);
    }

    private void validateRequest(CreateBookingRequest request) {

        if (request == null
                || request.serviceId() == null
                || request.startTime() == null
                || request.clientName() == null
                || request.clientName().isBlank()
                || request.clientEmail() == null
                || request.clientEmail().isBlank()) {

            throw new IllegalArgumentException(
                    "Os dados obrigatórios da reserva não foram preenchidos.");
        }

        if (!request.startTime().isAfter(
                LocalDateTime.now(CLINIC_ZONE))) {

            throw new IllegalArgumentException(
                    "A reserva deve começar numa data e hora futuras.");
        }
    }

    private ClinicService findClinicService(Long serviceId) {

        ClinicService clinicService = clinicServiceRepository
                .findById(serviceId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Serviço não encontrado: " + serviceId));

        if (clinicService.getDurationMinutes() <= 0) {
            throw new IllegalStateException(
                    "O serviço deve ter uma duração positiva.");
        }

        return clinicService;
    }

    private List<Booking> findDayBookings(LocalDate date) {

        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        return bookingRepository
                .findAllByStartTimeLessThanAndEndTimeGreaterThan(
                        dayEnd,
                        dayStart);
    }

    private List<Professional> findFreeProfessionals(
            Long serviceId,
            Long preferredProfessionalId,
            List<Booking> dayBookings,
            LocalDateTime start,
            LocalDateTime end) {

        List<Professional> candidates;

        if (preferredProfessionalId == null) {
            candidates = professionalRepository.findAll();
        } else {
            Professional preferred = professionalRepository
                    .findById(preferredProfessionalId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Profissional não encontrado: "
                                    + preferredProfessionalId));

            if (!canPerformService(preferred, serviceId)) {
                throw new IllegalArgumentException(
                        "O profissional escolhido não realiza este serviço.");
            }

            candidates = List.of(preferred);
        }

        return candidates.stream()
                .filter(professional ->
                        canPerformService(professional, serviceId))
                .filter(professional ->
                        isWorking(professional, start, end))
                .filter(professional ->
                        isProfessionalFree(
                                professional, dayBookings, start, end))
                .toList();
    }

    private boolean canPerformService(
            Professional professional,
            Long serviceId) {

        return professional.getServices().stream()
                .anyMatch(service -> service.getId().equals(serviceId));
    }

    private boolean isWorking(
            Professional professional,
            LocalDateTime start,
            LocalDateTime end) {

        LocalDate date = start.toLocalDate();

        return professional.getWorkingPeriods().stream()
                .anyMatch(period -> {

                    if (period.getDayOfWeek() != date.getDayOfWeek()) {
                        return false;
                    }

                    LocalDateTime periodStart =
                            date.atTime(period.getStartTime());

                    LocalDateTime periodEnd =
                            date.atTime(period.getEndTime());

                    return !start.isBefore(periodStart)
                            && !end.isAfter(periodEnd)
                            && Duration.between(periodStart, start)
                                    .toNanos() % SLOT_STEP.toNanos() == 0;
                });
    }

    private boolean isProfessionalFree(
            Professional professional,
            List<Booking> dayBookings,
            LocalDateTime start,
            LocalDateTime end) {

        return dayBookings.stream().noneMatch(booking ->
                booking.getProfessional().getId()
                        .equals(professional.getId())
                        && overlaps(booking, start, end));
    }

    private List<Room> findFreeRooms(
            Set<EquipmentType> requiredTypes,
            List<Booking> dayBookings,
            LocalDateTime start,
            LocalDateTime end) {

        return roomRepository.findAll().stream()
                .filter(room -> hasRequiredEquipment(room, requiredTypes))
                .filter(room -> isRoomFree(room, dayBookings, start, end))
                .toList();
    }

    private boolean hasRequiredEquipment(
            Room room,
            Set<EquipmentType> requiredTypes) {

        Set<EquipmentType> availableTypes = room.getEquipment().stream()
                .map(Equipment::getType)
                .collect(Collectors.toSet());

        return availableTypes.containsAll(requiredTypes);
    }

    private boolean isRoomFree(
            Room room,
            List<Booking> dayBookings,
            LocalDateTime start,
            LocalDateTime end) {

        return dayBookings.stream().noneMatch(booking ->
                booking.getRoom().getId().equals(room.getId())
                        && overlaps(booking, start, end));
    }

    private boolean overlaps(
            Booking booking,
            LocalDateTime start,
            LocalDateTime end) {

        return booking.getStartTime().isBefore(end)
                && booking.getEndTime().isAfter(start);
    }

    private BookingResponse toResponse(Booking booking) {

        return new BookingResponse(
                booking.getId(),
                booking.getClientName(),
                booking.getClientEmail(),
                booking.getClinicService().getId(),
                booking.getClinicService().getName(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getProfessional().getId(),
                booking.getProfessional().getName());
    }
}