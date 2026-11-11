package pt.clinic.scheduler.domain;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;

@Entity
@Table(name = "clinic_services")
public class ClinicService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private int durationMinutes;

    @ElementCollection
    @CollectionTable(
        name = "clinic_service_equipment_types",
        joinColumns = @JoinColumn(name = "clinic_service_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "equipment_type", nullable = false)
    private Set<EquipmentType> requiredEquipmentTypes = new HashSet<>();

    protected ClinicService() {
    }

    public ClinicService(String name, int durationMinutes,
                         Set<EquipmentType> requiredEquipmentTypes) {
        this.name = name;
        this.durationMinutes = durationMinutes;
        this.requiredEquipmentTypes = new HashSet<>(requiredEquipmentTypes);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public Set<EquipmentType> getRequiredEquipmentTypes() {
        return Set.copyOf(requiredEquipmentTypes);
    }
}