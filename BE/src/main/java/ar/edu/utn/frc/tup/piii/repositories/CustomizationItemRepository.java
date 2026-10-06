package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.CustomizationItem;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for CustomizationItem catalogue. */
public interface CustomizationItemRepository extends JpaRepository<CustomizationItem, String> {
}
