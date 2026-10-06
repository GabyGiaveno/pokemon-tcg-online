package ar.edu.utn.frc.tup.piii.dtos.response;

/** Response DTO for an achievement (catalogue entry + player unlock status). */
public class AchievementResponse {
    private String id;
    private String name;
    private String description;
    private String icon;
    private boolean unlocked;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }
}
