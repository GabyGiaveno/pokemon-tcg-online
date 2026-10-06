package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.request.EquipSkinRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.UpdateProfileRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.AchievementResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.BadgeResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.CustomizationItemResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.ProfileResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.SkinResponse;
import ar.edu.utn.frc.tup.piii.security.CurrentPlayerService;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "Players", description = "Player profile, badges, achievements, and customization. All endpoints require authentication.")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/players")
public class PlayerController {

    private final PlayerService playerService;
    private final CurrentPlayerService currentPlayerService;

    public PlayerController(PlayerService playerService, CurrentPlayerService currentPlayerService) {
        this.playerService = playerService;
        this.currentPlayerService = currentPlayerService;
    }

    @Operation(summary = "Get my profile", description = "Returns the full profile of the authenticated player including stats, equipped skin, and recent badges.")
    @ApiResponse(responseCode = "200", description = "Player profile")
    @GetMapping("/me")
    public ResponseEntity<ProfileResponse> getMyProfile() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.getProfile(playerId));
    }

    @Operation(summary = "Update profile")
    @ApiResponse(responseCode = "200", description = "Updated profile")
    @PutMapping("/me")
    public ResponseEntity<ProfileResponse> updateProfile(@RequestBody @Valid UpdateProfileRequest request) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.updateProfile(playerId, request));
    }

    @Operation(summary = "Get badges")
    @ApiResponse(responseCode = "200", description = "List of unlocked badges")
    @GetMapping("/me/badges")
    public ResponseEntity<List<BadgeResponse>> getBadges() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.getBadges(playerId));
    }

    @Operation(summary = "Get achievements")
    @ApiResponse(responseCode = "200", description = "List of unlocked achievements")
    @GetMapping("/me/achievements")
    public ResponseEntity<List<AchievementResponse>> getAchievements() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.getAchievements(playerId));
    }

    @Operation(summary = "Get available skins", description = "Returns all trainer skins available to the player.")
    @ApiResponse(responseCode = "200", description = "List of skins")
    @GetMapping("/me/skins")
    public ResponseEntity<List<SkinResponse>> getSkins() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.getSkins(playerId));
    }

    @Operation(summary = "Equip skin", description = "Equips a trainer skin. Enforces a unique constraint: only one skin can be equipped at a time.")
    @ApiResponse(responseCode = "200", description = "Skin equipped")
    @PutMapping("/me/skin")
    public ResponseEntity<Void> equipSkin(@RequestBody @Valid EquipSkinRequest request) {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        playerService.equipSkin(playerId, request.getSkinId());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Get customization items", description = "Returns available customization items grouped by category: clothes, accessory, pose, background.")
    @ApiResponse(responseCode = "200", description = "List of customization items")
    @GetMapping("/me/customization")
    public ResponseEntity<List<CustomizationItemResponse>> getCustomizationItems() {
        Long playerId = currentPlayerService.getCurrentPlayerId();
        return ResponseEntity.ok(playerService.getCustomizationItems(playerId));
    }
}
