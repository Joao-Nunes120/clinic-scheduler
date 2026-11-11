package pt.clinic.scheduler.config;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import pt.clinic.scheduler.domain.Booking;
import pt.clinic.scheduler.domain.ClinicService;
import pt.clinic.scheduler.domain.Equipment;
import pt.clinic.scheduler.domain.EquipmentType;
import pt.clinic.scheduler.domain.Professional;
import pt.clinic.scheduler.domain.Room;
import pt.clinic.scheduler.domain.WorkingPeriod;
import pt.clinic.scheduler.repository.BookingRepository;
import pt.clinic.scheduler.repository.ClinicServiceRepository;
import pt.clinic.scheduler.repository.EquipmentRepository;
import pt.clinic.scheduler.repository.ProfessionalRepository;
import pt.clinic.scheduler.repository.RoomRepository;
import pt.clinic.scheduler.repository.WorkingPeriodRepository;

@Component
@ConditionalOnProperty(
        name = "app.seed-data",
        havingValue = "true"
)
public class InitialDataLoader implements CommandLineRunner {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(InitialDataLoader.class);

    private static final List<BookingTemplate> WEEKDAY_BOOKINGS = List.of(
            new BookingTemplate(
                    "Ana", "Depilação a laser", "Sala 1", 9),
            new BookingTemplate(
                    "Beatriz", "Remoção de tatuagem", "Sala 2", 9),
            new BookingTemplate(
                    "Carla", "Massagem", "Sala 4", 9),
            new BookingTemplate(
                    "Diana", "HIFU facial", "Sala 3", 9),
            new BookingTemplate(
                    "Eva", "Manicure com máquina", "Sala 5", 9),
            new BookingTemplate(
                    "Filipe", "Corte de cabelo", "Sala 6", 9),
            new BookingTemplate(
                    "Carla", "Radiofrequência facial", "Sala 4", 14),
            new BookingTemplate(
                    "Diana", "Microagulhamento", "Sala 6", 14),
            new BookingTemplate(
                    "Eva", "Solário", "Sala 7", 14),
            new BookingTemplate(
                    "Filipe", "Pressoterapia", "Sala 4", 15),
            new BookingTemplate(
                    "Diana", "Micropigmentação de sobrancelhas", "Sala 6", 16),
            new BookingTemplate(
                    "Carla", "Hidratação facial", "Sala 4", 16)
    );

    private static final List<BookingTemplate> SATURDAY_BOOKINGS = List.of(
            new BookingTemplate(
                    "Ana", "Depilação a laser", "Sala 1", 9),
            new BookingTemplate(
                    "Carla", "Massagem", "Sala 4", 9),
            new BookingTemplate(
                    "Eva", "Manicure com máquina", "Sala 5", 9)
    );

    private final ClinicServiceRepository clinicServiceRepository;
    private final ProfessionalRepository professionalRepository;
    private final RoomRepository roomRepository;
    private final EquipmentRepository equipmentRepository;
    private final WorkingPeriodRepository workingPeriodRepository;
    private final BookingRepository bookingRepository;
    private final YearMonth seedMonth;

    public InitialDataLoader(
            ClinicServiceRepository clinicServiceRepository,
            ProfessionalRepository professionalRepository,
            RoomRepository roomRepository,
            EquipmentRepository equipmentRepository,
            WorkingPeriodRepository workingPeriodRepository,
            BookingRepository bookingRepository,
            @Value("${app.seed-month:2026-12}") String seedMonth) {

        this.clinicServiceRepository = clinicServiceRepository;
        this.professionalRepository = professionalRepository;
        this.roomRepository = roomRepository;
        this.equipmentRepository = equipmentRepository;
        this.workingPeriodRepository = workingPeriodRepository;
        this.bookingRepository = bookingRepository;
        this.seedMonth = YearMonth.parse(seedMonth);
    }

    @Override
    @Transactional
    public void run(String... args) {
        initializeBaseData();
        initializeBookings();
    }

    private void initializeBaseData() {

        if (clinicServiceRepository.count() > 0) {
            LOGGER.info(
                    "Os serviços já existem. Mantidos os dados base atuais.");
            return;
        }

        if (professionalRepository.count() > 0
                || roomRepository.count() > 0
                || equipmentRepository.count() > 0
                || workingPeriodRepository.count() > 0) {

            throw new IllegalStateException(
                    "Existem dados base parciais. "
                            + "Verificar a base de dados antes de continuar.");
        }

        // Serviços.
        ClinicService haircut = createService(
                "Corte de cabelo", 30);

        ClinicService massage = createService(
                "Massagem", 60);

        ClinicService facial = createService(
                "Hidratação facial", 45);

        ClinicService manicure = createService(
                "Manicure com máquina", 60,
                EquipmentType.MANICURE_MACHINE);

        ClinicService hairRemoval = createService(
                "Depilação a laser", 45,
                EquipmentType.HAIR_REMOVAL_LASER);

        ClinicService tattooRemoval = createService(
                "Remoção de tatuagem", 45,
                EquipmentType.TATTOO_REMOVAL_LASER);

        ClinicService pressotherapy = createService(
                "Pressoterapia", 45,
                EquipmentType.PRESSOTHERAPY_MACHINE);

        ClinicService radiofrequency = createService(
                "Radiofrequência facial", 45,
                EquipmentType.RADIOFREQUENCY_MACHINE);

        ClinicService hifu = createService(
                "HIFU facial", 60,
                EquipmentType.HIFU_MACHINE);

        ClinicService microneedling = createService(
                "Microagulhamento", 60,
                EquipmentType.MICRONEEDLING_DEVICE);

        ClinicService micropigmentation = createService(
                "Micropigmentação de sobrancelhas", 90,
                EquipmentType.MICROPIGMENTATION_DEVICE);

        ClinicService solarium = createService(
                "Solário", 20,
                EquipmentType.SOLARIUM);

        // Salas e equipamentos fixos.
        createRoom(
                "Sala 1",
                EquipmentType.HAIR_REMOVAL_LASER);

        createRoom(
                "Sala 2",
                EquipmentType.HAIR_REMOVAL_LASER,
                EquipmentType.TATTOO_REMOVAL_LASER);

        createRoom(
                "Sala 3",
                EquipmentType.RADIOFREQUENCY_MACHINE,
                EquipmentType.HIFU_MACHINE);

        createRoom(
                "Sala 4",
                EquipmentType.PRESSOTHERAPY_MACHINE,
                EquipmentType.RADIOFREQUENCY_MACHINE);

        createRoom(
                "Sala 5",
                EquipmentType.MANICURE_MACHINE);

        createRoom(
                "Sala 6",
                EquipmentType.MICRONEEDLING_DEVICE,
                EquipmentType.MICROPIGMENTATION_DEVICE);

        createRoom(
                "Sala 7",
                EquipmentType.SOLARIUM);

        // Profissionais e serviços que realizam.
        Professional ana = createProfessional(
                "Ana",
                hairRemoval, tattooRemoval, radiofrequency);

        Professional beatriz = createProfessional(
                "Beatriz",
                hairRemoval, tattooRemoval, hifu);

        Professional carla = createProfessional(
                "Carla",
                massage, facial, radiofrequency, pressotherapy);

        Professional diana = createProfessional(
                "Diana",
                facial, microneedling, micropigmentation, hifu);

        Professional eva = createProfessional(
                "Eva",
                manicure, haircut, solarium);

        Professional filipe = createProfessional(
                "Filipe",
                haircut, massage, pressotherapy, solarium);

        // Segunda a sexta: 09:00–13:00 e 14:00–18:00.
        for (Professional professional :
                List.of(ana, beatriz, carla, diana, eva, filipe)) {

            addWeeklySchedule(professional);
        }

        // Estes profissionais trabalham também ao sábado de manhã.
        for (Professional professional :
                List.of(ana, carla, eva, filipe)) {

            createWorkingPeriod(
                    professional,
                    DayOfWeek.SATURDAY,
                    LocalTime.of(9, 0),
                    LocalTime.of(13, 0));
        }

        LOGGER.info(
                "Dados base preparados: 12 serviços, "
                        + "6 profissionais, 7 salas e 11 aparelhos.");
    }

    private void initializeBookings() {

        if (bookingRepository.count() > 0) {
            LOGGER.info(
                    "Já existem reservas. Mantida a agenda atual.");
            return;
        }

        Map<String, ClinicService> services =
                clinicServiceRepository.findAll().stream()
                        .collect(Collectors.toMap(
                                ClinicService::getName,
                                Function.identity()));

        Map<String, Professional> professionals =
                professionalRepository.findAll().stream()
                        .collect(Collectors.toMap(
                                Professional::getName,
                                Function.identity()));

        Map<String, Room> rooms =
                roomRepository.findAll().stream()
                        .collect(Collectors.toMap(
                                Room::getName,
                                Function.identity()));

        List<Booking> bookings = new ArrayList<>();

        for (int day = 1; day <= seedMonth.lengthOfMonth(); day++) {

            LocalDate date = seedMonth.atDay(day);

            for (BookingTemplate template : templatesFor(date)) {

                ClinicService service =
                        require(services, template.serviceName());

                Professional professional =
                        require(professionals, template.professionalName());

                Room room =
                        require(rooms, template.roomName());

                LocalDateTime start =
                        date.atTime(LocalTime.of(template.hour(), 0));

                LocalDateTime end =
                        start.plusMinutes(service.getDurationMinutes());

                int number = bookings.size() + 1;

                bookings.add(new Booking(
                        "Cliente Teste " + number,
                        "agenda." + number + "@example.com",
                        start,
                        end,
                        service,
                        professional,
                        room));
            }
        }

        bookingRepository.saveAllAndFlush(bookings);

        LOGGER.info(
                "Agenda inicial preparada: {} reservas para {}.",
                bookings.size(),
                seedMonth);
    }

    private ClinicService createService(
            String name,
            int durationMinutes,
            EquipmentType... requiredTypes) {

        ClinicService service = new ClinicService(
                name,
                durationMinutes,
                Set.of(requiredTypes));

        ClinicService saved =
                clinicServiceRepository.save(service);

        LOGGER.info(
                "Serviço: id={}, nome={}",
                saved.getId(),
                saved.getName());

        return saved;
    }

    private void createRoom(
            String name,
            EquipmentType... equipmentTypes) {

        Room room = roomRepository.save(new Room(name));

        for (EquipmentType type : equipmentTypes) {
            equipmentRepository.save(new Equipment(
                    type.name() + " - " + name,
                    type,
                    room));
        }
    }

    private Professional createProfessional(
            String name,
            ClinicService... services) {

        Professional professional = new Professional(name);

        for (ClinicService service : services) {
            professional.addService(service);
        }

        Professional saved =
                professionalRepository.save(professional);

        LOGGER.info(
                "Profissional: id={}, nome={}",
                saved.getId(),
                saved.getName());

        return saved;
    }

    private void addWeeklySchedule(Professional professional) {

        for (DayOfWeek day : List.of(
                DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY,
                DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY)) {

            createWorkingPeriod(
                    professional,
                    day,
                    LocalTime.of(9, 0),
                    LocalTime.of(13, 0));

            createWorkingPeriod(
                    professional,
                    day,
                    LocalTime.of(14, 0),
                    LocalTime.of(18, 0));
        }
    }

    private void createWorkingPeriod(
            Professional professional,
            DayOfWeek day,
            LocalTime start,
            LocalTime end) {

        workingPeriodRepository.save(new WorkingPeriod(
                day,
                start,
                end,
                professional));
    }

    private List<BookingTemplate> templatesFor(LocalDate date) {

        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            return List.of();
        }

        if (date.getDayOfWeek() == DayOfWeek.SATURDAY) {
            return SATURDAY_BOOKINGS;
        }

        return WEEKDAY_BOOKINGS;
    }

    private <T> T require(Map<String, T> items, String name) {

        T item = items.get(name);

        if (item == null) {
            throw new IllegalStateException(
                    "Falta um elemento dos dados base: " + name);
        }

        return item;
    }

    private record BookingTemplate(
            String professionalName,
            String serviceName,
            String roomName,
            int hour
    ) {
    }
}