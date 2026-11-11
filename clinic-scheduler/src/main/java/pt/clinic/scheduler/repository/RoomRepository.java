package pt.clinic.scheduler.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import pt.clinic.scheduler.domain.Room;

public interface RoomRepository extends JpaRepository<Room, Long> {
}