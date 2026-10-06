package ar.edu.utn.frc.tup.piii.dtos.request;

/** Request DTO for PUT /api/players/me (bio, favoriteRegion, favoritePokemon). */
public class UpdateProfileRequest {
    private String bio;
    private String favoriteRegion;
    private String favoritePokemon;

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public String getFavoriteRegion() { return favoriteRegion; }
    public void setFavoriteRegion(String favoriteRegion) { this.favoriteRegion = favoriteRegion; }

    public String getFavoritePokemon() { return favoritePokemon; }
    public void setFavoritePokemon(String favoritePokemon) { this.favoritePokemon = favoritePokemon; }
}
