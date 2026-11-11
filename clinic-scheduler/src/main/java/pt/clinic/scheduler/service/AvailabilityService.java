package pt.clinic.scheduler.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.clinic.scheduler.domain.Booking;
import pt.clinic.scheduler.domain.ClinicService;
import pt.clinic.scheduler.domain.Equipment;
import pt.clinic.scheduler.domain.EquipmentType;
import pt.clinic.scheduler.domain.Professional;
import pt.clinic.scheduler.domain.Room;
import pt.clinic.scheduler.domain.WorkingPeriod;
import pt.clinic.scheduler.dto.AvailabilitySlot;
import pt.clinic.scheduler.dto.DayAvailability;
import pt.clinic.scheduler.dto.MonthlyAvailabilityResponse;
import pt.clinic.scheduler.repository.BookingRepository;
import pt.clinic.scheduler.repository.ClinicServiceRepository;
import pt.clinic.scheduler.repository.ProfessionalRepository;
import pt.clinic.scheduler.repository.RoomRepository;

@Service
public class AvailabilityService {

    private static final int SLOT_STEP_MINUTES = 15;

    private static final ZoneId CLINIC_ZONE =
            ZoneId.of("Europe/Lisbon");

    private final ClinicServiceRepository clinicServiceRepository;
    private final ProfessionalRepository professionalRepository;
    private final RoomRepository roomRepository;
    private final BookingRepository bookingRepository;

    public AvailabilityService(
            ClinicServiceRepository clinicServiceRepository,
            ProfessionalRepository professionalRepository,
            RoomRepository roomRepository,
            BookingRepository bookingRepository) {

        this.clinicServiceRepository = clinicServiceRepository;
        this.professionalRepository = professionalRepository;
        this.roomRepository = roomRepository;
        this.bookingRepository = bookingRepository;
    }

    // Coordena a pesquisa de disponibilidade do mês.
    @Transactional(readOnly = true)
    public MonthlyAvailabilityResponse getMonthlyAvailability(
            Long serviceId,
            YearMonth month,
            Long professionalId) {

        if (serviceId == null || month == null) {
            throw new IllegalArgumentException(
                    "O serviço e o mês são obrigatórios.");
        }

        ClinicService clinicService = findClinicService(serviceId);

        List<Professional> professionals =
                findProfessionals(serviceId, professionalId);

        List<Room> rooms = findCompatibleRooms(
                clinicService.getRequiredEquipmentTypes());

        List<Booking> bookings = findMonthBookings(month);

        Map<Long, List<Booking>> bookingsByProfessional =
                bookings.stream().collect(Collectors.groupingBy(
                        booking -> booking.getProfessional().getId()));

        Map<Long, List<Booking>> bookingsByRoom =
                bookings.stream().collect(Collectors.groupingBy(
                        booking -> booking.getRoom().getId()));

        LocalDateTime now = LocalDateTime.now(CLINIC_ZONE);
        List<DayAvailability> days = new ArrayList<>();

        for (int day = 1; day <= month.lengthOfMonth(); day++) {
            days.add(findDayAvailability(
                    month.atDay(day),
                    clinicService.getDurationMinutes(),
                    professionals,
                    rooms,
                    bookingsByProfessional,
                    bookingsByRoom,
                    now));
        }

        return new MonthlyAvailabilityResponse(
                clinicService.getId(),
                clinicService.getDurationMinutes(),
                days);
    }

    // Procura o serviço e verifica a sua duração.
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

    // Procura profissionais habilitados, respeitando a preferência.
    private List<Professional> findProfessionals(
            Long serviceId,
            Long professionalId) {

        List<Professional> candidates;

        if (professionalId == null) {
            candidates = professionalRepository.findAll();
        } else {
            Professional professional = professionalRepository
                    .findById(professionalId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Profissional não encontrado: "
                                    + professionalId));

            candidates = List.of(professional);
        }

        List<Professional> qualified = candidates.stream()
                .filter(professional -> professional.getServices()
                        .stream()
                        .anyMatch(service ->
                                service.getId().equals(serviceId)))
                .toList();

        if (professionalId != null && qualified.isEmpty()) {
            throw new IllegalArgumentException(
                    "O profissional escolhido não realiza este serviço.");
        }

        return qualified;
    }

    // Seleciona salas com todos os tipos de equipamento necessários.
    private List<Room> findCompatibleRooms(
            Set<EquipmentType> requiredTypes) {

        return roomRepository.findAll().stream()
                .filter(room -> {
                    Set<EquipmentType> availableTypes =
                            room.getEquipment().stream()
                                    .map(Equipment::getType)
                                    .collect(Collectors.toSet());

                    return availableTypes.containsAll(requiredTypes);
                })
                .toList();
    }

    // Consulta as reservas que se sobrepõem ao mês.
    private List<Booking> findMonthBookings(YearMonth month) {

        LocalDateTime start =
                month.atDay(1).atStartOfDay();

        LocalDateTime end =
                month.plusMonths(1).atDay(1).atStartOfDay();

        return bookingRepository
                .findAllByStartTimeLessThanAndEndTimeGreaterThan(
                        end,
                        start);
    }

    // Junta os horários disponíveis de todos os profissionais num dia.
    private DayAvailability findDayAvailability(
            LocalDate date,
            int durationMinutes,
            List<Professional> professionals,
            List<Room> rooms,
            Map<Long, List<Booking>> bookingsByProfessional,
            Map<Long, List<Booking>> bookingsByRoom,
            LocalDateTime now) {

        if (date.isBefore(now.toLocalDate())) {
            return new DayAvailability(date, List.of());
        }

        // Mantém os horários ordenados e sem inícios repetidos.
        Map<LocalDateTime, AvailabilitySlot> slotsByStart =
                new TreeMap<>();

        for (Professional professional : professionals) {

            List<Booking> professionalBookings =
                    bookingsByProfessional.getOrDefault(
                            professional.getId(),
                            List.of());

            for (WorkingPeriod period
                    : professional.getWorkingPeriods()) {

                if (period.getDayOfWeek() != date.getDayOfWeek()) {
                    continue;
                }

                List<AvailabilitySlot> periodSlots =
                        findSlotsForPeriod(
                                date,
                                period,
                                durationMinutes,
                                professionalBookings,
                                rooms,
                                bookingsByRoom,
                                now);

                for (AvailabilitySlot slot : periodSlots) {
                    slotsByStart.putIfAbsent(slot.start(), slot);
                }
            }
        }

        return new DayAvailability(
                date,
                new ArrayList<>(slotsByStart.values()));
    }

    // Experimenta inícios de 15 em 15 minutos num período de trabalho.
    private List<AvailabilitySlot> findSlotsForPeriod(
            LocalDate date,
            WorkingPeriod period,
            int durationMinutes,
            List<Booking> professionalBookings,
            List<Room> rooms,
            Map<Long, List<Booking>> bookingsByRoom,
            LocalDateTime now) {

        List<AvailabilitySlot> slots = new ArrayList<>();

        LocalDateTime start = date.atTime(period.getStartTime());
        LocalDateTime periodEnd = date.atTime(period.getEndTime());

        while (!start.plusMinutes(durationMinutes).isAfter(periodEnd)) {

            LocalDateTime end = start.plusMinutes(durationMinutes);

            boolean professionalFree =
                    !hasConflict(professionalBookings, start, end);

            if (start.isAfter(now)
                    && professionalFree
                    && hasFreeRoom(rooms, bookingsByRoom, start, end)) {

                slots.add(new AvailabilitySlot(start, end));
            }

            start = start.plusMinutes(SLOT_STEP_MINUTES);
        }

        return slots;
    }

    // Basta existir uma sala compatível livre para o intervalo.
    private boolean hasFreeRoom(
            List<Room> rooms,
            Map<Long, List<Booking>> bookingsByRoom,
            LocalDateTime start,
            LocalDateTime end) {

        for (Room room : rooms) {

            List<Booking> roomBookings =
                    bookingsByRoom.getOrDefault(
                            room.getId(),
                            List.of());

            if (!hasConflict(roomBookings, start, end)) {
                return true;
            }
        }

        return false;
    }

    // Verifica se alguma reserva se sobrepõe ao intervalo.
    private boolean hasConflict(
            List<Booking> bookings,
            LocalDateTime start,
            LocalDateTime end) {

        return bookings.stream().anyMatch(booking ->
                booking.getStartTime().isBefore(end)
                        && booking.getEndTime().isAfter(start));
    }
}