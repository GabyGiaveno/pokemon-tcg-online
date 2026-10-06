package ar.edu.utn.frc.tup.piii.repositories;

import ar.edu.utn.frc.tup.piii.entities.TrainerSkin;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data JPA repository for TrainerSkin catalogue. */
public interface TrainerSkinRepository extends JpaRepository<TrainerSkin, String> {
}
