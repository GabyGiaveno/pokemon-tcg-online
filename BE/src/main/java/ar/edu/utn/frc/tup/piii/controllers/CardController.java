package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.response.CardPageResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.CardResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.SyncResponse;
import ar.edu.utn.frc.tup.piii.services.CardCacheService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for card cache query and sync endpoints ({@code /api/cards}).
 */
@Tag(name = "Cards", description = "Query the local card cache (XY1 set). No authentication required.")
@SecurityRequirements
@RestController
@RequestMapping("/api/cards")
public class CardController {

    private final CardCacheService cardCacheService;

    public CardController(CardCacheService cardCacheService) {
        this.cardCacheService = cardCacheService;
    }

    /**
     * Paginated search of the local card cache with optional filters.
     *
     * @param name      filter by name (contains, case-insensitive)
     * @param set       filter by card set ID
     * @param supertype filter by supertype
     * @param type      filter by type
     * @param pageable  pagination and sort from {@code page}, {@code size}, {@code sort} query params
     * @return a stable paginated card response for frontend consumption
     */
    @Operation(summary = "Search cards", description = "Paginated search of the local XY1 card cache. All filters are optional and combinable.")
    @ApiResponse(responseCode = "200", description = "Paginated card list")
    @GetMapping
    public ResponseEntity<CardPageResponse> getCards(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) String set,
            @RequestParam(required = false) String supertype,
            @RequestParam(required = false) String type,
            Pageable pageable) {
        Page<CardResponse> cards = cardCacheService.getCards(name, set, supertype, type, pageable);

        CardPageResponse response = new CardPageResponse();
        response.setData(cards.getContent());
        response.setTotal(cards.getTotalElements());
        response.setPage(cards.getNumber());
        response.setSize(cards.getSize());

        return ResponseEntity.ok(response);
    }

    /**
     * Retrieves a single card by its API ID from the local cache.
     *
     * @param id the card ID (e.g. "xy1-1")
     * @return the card response
     */
    @Operation(summary = "Get card by ID", description = "Returns a single card from the local cache by its pokemontcg.io ID (e.g. xy1-1).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Card found"),
        @ApiResponse(responseCode = "404", description = "Card not found in cache")
    })
    @GetMapping("/{id}")
    public ResponseEntity<CardResponse> getCardById(
            @Parameter(description = "Card ID in pokemontcg.io format, e.g. xy1-1") @PathVariable String id) {
        return ResponseEntity.ok(cardCacheService.getCardById(id));
    }

    /**
     * Triggers a full sync of the given set from the external API into the local cache.
     *
     * @param set the set ID to sync (e.g. "xy1")
     * @return sync result with the set ID and number of imported cards
     */
    @Operation(summary = "Sync set from pokemontcg.io", description = "Fetches all cards of the given set from the external API and upserts them into the local cache.")
    @ApiResponse(responseCode = "200", description = "Sync completed with count of imported cards")
    @PostMapping("/sync")
    public ResponseEntity<SyncResponse> syncSet(
            @Parameter(description = "Set ID to sync, e.g. xy1") @RequestParam String set) {
        int count = cardCacheService.syncSet(set);
        return ResponseEntity.ok(new SyncResponse("Sync completed", set, count));
    }
}
