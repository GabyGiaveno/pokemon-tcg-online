package ar.edu.utn.frc.tup.piii.security;

import ar.edu.utn.frc.tup.piii.entities.Player;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentPlayerService {

    public Player getCurrentPlayer() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof Player) {
            return (Player) principal;
        }
        throw new IllegalStateException("No authenticated player found");
    }

    public Long getCurrentPlayerId() {
        return getCurrentPlayer().getId();
    }
}
