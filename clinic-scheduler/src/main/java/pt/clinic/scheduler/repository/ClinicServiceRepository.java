package pt.clinic.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.clinic.scheduler.domain.ClinicService;

public interface ClinicServiceRepository
        extends JpaRepository<ClinicService, Long> {
}