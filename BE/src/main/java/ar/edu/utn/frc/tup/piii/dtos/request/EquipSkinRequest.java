package ar.edu.utn.frc.tup.piii.dtos.request;

/** Request DTO for PUT /api/players/me/skin (skinId). */
public class EquipSkinRequest {
    private String skinId;

    public String getSkinId() { return skinId; }
    public void setSkinId(String skinId) { this.skinId = skinId; }
}
