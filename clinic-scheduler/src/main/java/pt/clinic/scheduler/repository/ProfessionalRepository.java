package pt.clinic.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.clinic.scheduler.domain.Professional;

public interface ProfessionalRepository
        extends JpaRepository<Professional, Long> {
}