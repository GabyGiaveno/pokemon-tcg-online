package ar.edu.utn.frc.tup.piii.entities;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerCustomizationId implements Serializable {
    private Long playerId;
    private String itemId;
}
