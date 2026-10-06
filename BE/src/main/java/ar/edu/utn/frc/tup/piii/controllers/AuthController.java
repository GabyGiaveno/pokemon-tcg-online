package ar.edu.utn.frc.tup.piii.controllers;

import ar.edu.utn.frc.tup.piii.dtos.request.ForgotPasswordRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.LoginRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.RegisterRequest;
import ar.edu.utn.frc.tup.piii.dtos.request.ResetPasswordRequest;
import ar.edu.utn.frc.tup.piii.dtos.response.PasswordResetResponse;
import ar.edu.utn.frc.tup.piii.dtos.response.PlayerResponse;
import ar.edu.utn.frc.tup.piii.services.PlayerService;
import ar.edu.utn.frc.tup.piii.services.RecoveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

/** Authentication endpoints — register, login, and password recovery. */
@Tag(name = "Auth", description = "Registration, login, and password recovery")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final PlayerService playerService;
    private final RecoveryService recoveryService;

    public AuthController(PlayerService playerService, RecoveryService recoveryService) {
        this.playerService = playerService;
        this.recoveryService = recoveryService;
    }

    @Operation(summary = "Register a new player", description = "Creates a player account and provisions a starter deck. Returns a JWT token.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Player created"),
        @ApiResponse(responseCode = "400", description = "Validation error or username/email already taken")
    })
    @PostMapping("/register")
    public ResponseEntity<PlayerResponse> register(@Valid @RequestBody RegisterRequest request) {
        PlayerResponse response = playerService.register(
                request.getUsername(),
                request.getEmail(),
                request.getPassword()
        );
        return ResponseEntity.created(URI.create("/api/auth/register"))
                .body(response);
    }

    @Operation(summary = "Login", description = "Authenticates a player and returns a JWT token valid for 24 hours.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Login successful"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/login")
    public ResponseEntity<PlayerResponse> login(@Valid @RequestBody LoginRequest request) {
        PlayerResponse response = playerService.login(
                request.getUsername(),
                request.getPassword()
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Request password reset", description = "Sends a one-time reset link to the email if it exists. Always returns 200 to prevent email enumeration.")
    @ApiResponse(responseCode = "200", description = "Reset email sent (or silently ignored if email not found)")
    @PostMapping("/forgot-password")
    public ResponseEntity<PasswordResetResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        recoveryService.requestRecovery(request.getEmail());
        PasswordResetResponse response = new PasswordResetResponse(
                "Si el correo existe, recibirás un enlace de recuperación."
        );
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Reset password", description = "Resets the password using a single-use token received by email.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Password reset successfully"),
        @ApiResponse(responseCode = "400", description = "Token invalid or expired")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<PasswordResetResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        recoveryService.resetPassword(request.getToken(), request.getNewPassword());
        PasswordResetResponse response = new PasswordResetResponse(
                "Contraseña restablecida correctamente."
        );
        return ResponseEntity.ok(response);
    }
}
