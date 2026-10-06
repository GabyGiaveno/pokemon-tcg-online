package ar.edu.utn.frc.tup.piii.dtos.response;

/** Response DTO for a trainer skin (catalogue entry + equipped status). */
public class SkinResponse {
    private String id;
    private String name;
    private String hatColor;
    private String shirtColor;
    private String pantsColor;
    private String skinTone;
    private boolean equipped;
    private String character;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHatColor() { return hatColor; }
    public void setHatColor(String hatColor) { this.hatColor = hatColor; }

    public String getShirtColor() { return shirtColor; }
    public void setShirtColor(String shirtColor) { this.shirtColor = shirtColor; }

    public String getPantsColor() { return pantsColor; }
    public void setPantsColor(String pantsColor) { this.pantsColor = pantsColor; }

    public String getSkinTone() { return skinTone; }
    public void setSkinTone(String skinTone) { this.skinTone = skinTone; }

    public boolean isEquipped() { return equipped; }
    public void setEquipped(boolean equipped) { this.equipped = equipped; }

    public String getCharacter() { return character; }
    public void setCharacter(String character) { this.character = character; }
}
