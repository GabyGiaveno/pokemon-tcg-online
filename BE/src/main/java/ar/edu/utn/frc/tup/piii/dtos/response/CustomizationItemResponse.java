package ar.edu.utn.frc.tup.piii.dtos.response;

/** Response DTO for a customization item (catalogue entry + unlock status). */
public class CustomizationItemResponse {
    private String id;
    private String name;
    private String category;
    private boolean unlocked;
    private String color;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }
}
