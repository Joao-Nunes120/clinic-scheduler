package pt.clinic.scheduler.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "bookings")
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String clientName;

    @Column(nullable = false)
    private String clientEmail;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @ManyToOne
    @JoinColumn(name = "clinic_service_id", nullable = false)
    private ClinicService clinicService;

    @ManyToOne
    @JoinColumn(name = "professional_id", nullable = false)
    private Professional professional;

    @ManyToOne
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    protected Booking() {
    }

    public Booking(
            String clientName,
            String clientEmail,
            LocalDateTime startTime,
            LocalDateTime endTime,
            ClinicService clinicService,
            Professional professional,
            Room room) {
        this.clientName = clientName;
        this.clientEmail = clientEmail;
        this.startTime = startTime;
        this.endTime = endTime;
        this.clinicService = clinicService;
        this.professional = professional;
        this.room = room;
    }

    public Long getId() {
        return id;
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public ClinicService getClinicService() {
        return clinicService;
    }

    public Professional getProfessional() {
        return professional;
    }

    public Room getRoom() {
        return room;
    }
}