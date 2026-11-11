package pt.clinic.scheduler.domain;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "professionals")
public class Professional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @ManyToMany
    @JoinTable(
        name = "professional_services",
        joinColumns = @JoinColumn(name = "professional_id"),
        inverseJoinColumns = @JoinColumn(name = "clinic_service_id")
    )
    private Set<ClinicService> services = new HashSet<>();

    @OneToMany(mappedBy = "professional")
    private List<WorkingPeriod> workingPeriods = new ArrayList<>();

    protected Professional() {
    }

    public Professional(String name) {
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Set<ClinicService> getServices() {
        return Set.copyOf(services);
    }

    public List<WorkingPeriod> getWorkingPeriods() {
        return List.copyOf(workingPeriods);
    }

    public void addService(ClinicService service) {
        services.add(service);
    }
}