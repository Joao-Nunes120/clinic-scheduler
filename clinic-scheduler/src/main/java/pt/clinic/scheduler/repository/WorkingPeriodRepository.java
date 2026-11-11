package pt.clinic.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.clinic.scheduler.domain.WorkingPeriod;

public interface WorkingPeriodRepository
        extends JpaRepository<WorkingPeriod, Long> {
}