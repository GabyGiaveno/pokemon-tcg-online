package ar.edu.utn.frc.tup.piii.dtos.response;

/** Response DTO for player data (id, username, JWT token). */
public class PlayerResponse {
    private Long id;
    private String username;
    private String token;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
