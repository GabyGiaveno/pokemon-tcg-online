package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for Badge catalogue. */
public interface BadgeRepository extends JpaRepository<Badge, String> {
}
