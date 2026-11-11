package pt.clinic.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.clinic.scheduler.domain.Equipment;

public interface EquipmentRepository
        extends JpaRepository<Equipment, Long> {
}