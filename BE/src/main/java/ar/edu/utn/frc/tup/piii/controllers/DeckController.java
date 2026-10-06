package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.request.CreateDeckRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.DeckValidationResponse;
import ar.edu.utn.frc.tup.piii.security.CurrentPlayerService;
import ar.edu.utn.frc.tup.piii.services.DeckService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * Handles deck CRUD and validation endpoints (/api/decks).
 */
@Tag(name = "Decks", description = "Deck CRUD and rule validation. All endpoints require authentication.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/decks")
public class DeckController {

    private final DeckService deckService;
    private final CurrentPlayerService currentPlayerService;

    public DeckController(DeckService deckService, CurrentPlayerService currentPlayerService) {
        this.deckService = deckService;
        this.currentPlayerService = currentPlayerService;
    }

    @Operation(summary = "List decks", description = "Returns all decks owned by the authenticated player.")
    @ApiResponse(responseCode = "200", description = "List of decks")
    @GetMapping
    public ResponseEntity<List<DeckResponse>> getDecks() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        List<DeckResponse> decks = deckService.getDecksByPlayer(playerId);
        return ResponseEntity.ok(decks);
    }

    @Operation(summary = "Get deck by ID")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Deck found"),
        @ApiResponse(responseCode = "403", description = "Deck belongs to another player"),
        @ApiResponse(responseCode = "404", description = "Deck not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<DeckResponse> getDeckById(
            @Parameter(description = "Deck ID") @PathVariable Long id) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(deckService.getDeckById(playerId, id));
    }

    @Operation(summary = "Create deck", description = "Creates a new deck for the authenticated player.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Deck created"),
        @ApiResponse(responseCode = "400", description = "Validation error in request body")
    })
    @PostMapping
    public ResponseEntity<DeckResponse> createDeck(
            @Valid @RequestBody CreateDeckRequest request) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        DeckResponse response = deckService.createDeck(playerId, request);
        return ResponseEntity.created(URI.create("/api/decks/" + response.getId()))
                .body(response);
    }

    @Operation(summary = "Update deck", description = "Replaces the deck contents. The player must own the deck.")
    @ApiResponse(responseCode = "200", description = "Deck updated")
    @PutMapping("/{id}")
    public ResponseEntity<DeckResponse> updateDeck(
            @Parameter(description = "Deck ID") @PathVariable Long id,
            @Valid @RequestBody CreateDeckRequest request) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        DeckResponse response = deckService.updateDeck(playerId, id, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete deck")
    @ApiResponse(responseCode = "204", description = "Deck deleted")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDeck(
            @Parameter(description = "Deck ID") @PathVariable Long id) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        deckService.deleteDeck(playerId, id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Validate deck", description = "Runs the deck through all XY1 ruleset validators (60 cards, copy limits, at least one Basic). Returns detailed error list.")
    @ApiResponse(responseCode = "200", description = "Validation result (valid: true/false with error details)")
    @PostMapping("/{id}/validate")
    public ResponseEntity<DeckValidationResponse> validateDeck(
            @Parameter(description = "Deck ID") @PathVariable Long id) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(deckService.validateDeck(playerId, id));
    }
}
