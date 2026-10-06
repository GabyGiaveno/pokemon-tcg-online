package ar.edu.utn.frc.tup.piii.dtos.request;

import jakarta.validation.constraints.NotBlank;

/** Request DTO for POST /api/auth/login (username, password). */
public class LoginRequest {
    @NotBlank(message = "El usuario o email es obligatorio.")
    private String username;
    @NotBlank(message = "La contraseña es obligatoria.")
    private String password;

    // Getters and Setters
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
