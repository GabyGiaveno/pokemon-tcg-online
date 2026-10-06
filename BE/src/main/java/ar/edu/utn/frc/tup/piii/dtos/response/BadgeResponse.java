package ar.edu.utn.frc.tup.piii.dtos.response;

/** Response DTO for a badge (catalogue entry + player unlock status). */
public class BadgeResponse {
    private String id;
    private String label;
    private String icon;
    private boolean unlocked;
    private String description;
    private String howToUnlock;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public boolean isUnlocked() { return unlocked; }
    public void setUnlocked(boolean unlocked) { this.unlocked = unlocked; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getHowToUnlock() { return howToUnlock; }
    public void setHowToUnlock(String howToUnlock) { this.howToUnlock = howToUnlock; }
}
