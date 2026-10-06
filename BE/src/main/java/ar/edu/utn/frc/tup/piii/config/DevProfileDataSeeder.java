package ar.edu.utn.frc.tup.piii.config;

import ar.edu.utn.frc.tup.piii.entities.Achievement;
import ar.edu.utn.frc.tup.piii.entities.Badge;
import ar.edu.utn.frc.tup.piii.entities.CustomizationItem;
import ar.edu.utn.frc.tup.piii.entities.TrainerSkin;
import ar.edu.utn.frc.tup.piii.repositories.AchievementRepository;
import ar.edu.utn.frc.tup.piii.repositories.BadgeRepository;
import ar.edu.utn.frc.tup.piii.repositories.CustomizationItemRepository;
import ar.edu.utn.frc.tup.piii.repositories.TrainerSkinRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Profile({"dev", "local"})
public class DevProfileDataSeeder implements CommandLineRunner {

    private final BadgeRepository badgeRepository;
    private final AchievementRepository achievementRepository;
    private final TrainerSkinRepository trainerSkinRepository;
    private final CustomizationItemRepository customizationItemRepository;

    public DevProfileDataSeeder(BadgeRepository badgeRepository,
                                AchievementRepository achievementRepository,
                                TrainerSkinRepository trainerSkinRepository,
                                CustomizationItemRepository customizationItemRepository) {
        this.badgeRepository = badgeRepository;
        this.achievementRepository = achievementRepository;
        this.trainerSkinRepository = trainerSkinRepository;
        this.customizationItemRepository = customizationItemRepository;
    }

    @Override
    public void run(String... args) {
        seedBadges();
        seedAchievements();
        seedSkins();
        seedCustomizationItems();
    }

    private void seedBadges() {
        if (badgeRepository.count() > 0) {
            return;
        }

        badgeRepository.saveAll(List.of(
                createBadge("fire", "Insignia Llama", "Otorgada por el líder del Gimnasio Fuego", "🔥", "Gana una partida con un mazo de tipo Fuego"),
                createBadge("water", "Insignia Cascada", "Otorgada por el líder del Gimnasio Agua", "💧", "Gana una partida con un mazo de tipo Agua"),
                createBadge("grass", "Insignia Bosque", "Otorgada por el líder del Gimnasio Planta", "🌿", "Gana una partida con un mazo de tipo Planta"),
                createBadge("electric", "Insignia Trueno", "Otorgada por el líder del Gimnasio Eléctrico", "⚡", "Gana una partida con un mazo de tipo Eléctrico"),
                createBadge("psychic", "Insignia Mente", "Otorgada por el líder del Gimnasio Psíquico", "🔮", "Gana una partida con un mazo de tipo Psíquico"),
                createBadge("fighting", "Insignia Lucha", "Otorgada por el líder del Gimnasio Lucha", "🥊", "Gana una partida con un mazo de tipo Lucha"),
                createBadge("dark", "Insignia Oscuridad", "Otorgada por el líder del Gimnasio Siniestro", "🌑", "Gana una partida con un mazo de tipo Siniestro"),
                createBadge("dragon", "Insignia Dragón", "Otorgada por el líder del Gimnasio Dragón", "🐉", "Gana 10 partidas consecutivas")
        ));
    }

    private void seedAchievements() {
        if (achievementRepository.count() > 0) {
            return;
        }

        achievementRepository.saveAll(List.of(
                createAchievement("first-win", "Campeón Novato", "Obtén tu primera victoria.", "🏆"),
                createAchievement("collector-100", "Coleccionista de Cartas", "Consigue 100 cartas.", "📚"),
                createAchievement("deck-master", "Maestro de Mazos", "Crea 10 mazos.", "🃏"),
                createAchievement("veteran", "Veterano Pokémon", "Juega 100 partidas.", "⭐"),
                createAchievement("lucky", "Golpe de Suerte", "Gana una partida con una carta decisiva.", "🍀")
        ));
    }

    private void seedSkins() {
        if (trainerSkinRepository.count() > 0) {
            return;
        }

        trainerSkinRepository.saveAll(List.of(
                createSkin("default", "Ash",       "ash",   "#e74c3c", "#2980b9", "#2c3e50", "#f5d0a9"),
                createSkin("fire",    "Fuego",      "ash",   "#e74c3c", "#c0392b", "#8e44ad", "#f5d0a9"),
                createSkin("water",   "Agua",       "ash",   "#3498db", "#2980b9", "#1a5276", "#f5d0a9"),
                createSkin("electric","Eléctrico",  "ash",   "#f1c40f", "#2c3e50", "#7f8c8d", "#f5d0a9"),
                createSkin("dark",    "Oscuro",     "ash",   "#2c3e50", "#1a1a2e", "#16213e", "#d4a574"),
                createSkin("brock",   "Brock",      "brock", "#2a1a0e", "#4a7c3f", "#3d2b1f", "#c8a165"),
                createSkin("misty",   "Misty",      "misty", "#ff6600", "#e63946", "#20b2aa", "#f5d0a9")
        ));
    }

    private void seedCustomizationItems() {
        if (customizationItemRepository.count() > 0) {
            return;
        }

        customizationItemRepository.saveAll(List.of(
                createItem("shirt-blue", "Camisa Azul", "clothes", "#105189"),
                createItem("shirt-red", "Camisa Roja", "clothes", "#c0392b"),
                createItem("shirt-black", "Camisa Negra", "clothes", "#2c3e50"),
                createItem("hat-cap", "Gorra Clásica", "accessory", "#DE940E"),
                createItem("hat-beanie", "Gorro Invernal", "accessory", "#3498db"),
                createItem("glasses", "Gafas Oscuras", "accessory", null),
                createItem("pose-1", "Firme", "pose", null),
                createItem("pose-2", "Con Puño", "pose", null),
                createItem("pose-3", "Saludando", "pose", null),
                createItem("bg-stadium", "Estadio", "background", null),
                createItem("bg-beach", "Playa", "background", null),
                createItem("bg-mountain", "Montaña", "background", null)
        ));
    }

    private Badge createBadge(String id, String label, String description, String icon, String howToUnlock) {
        Badge badge = new Badge();
        badge.setId(id);
        badge.setLabel(label);
        badge.setDescription(description);
        badge.setIcon(icon);
        badge.setHowToUnlock(howToUnlock);
        return badge;
    }

    private Achievement createAchievement(String id, String name, String description, String icon) {
        Achievement achievement = new Achievement();
        achievement.setId(id);
        achievement.setName(name);
        achievement.setDescription(description);
        achievement.setIcon(icon);
        return achievement;
    }

    private TrainerSkin createSkin(String id, String name, String character, String hatColor, String shirtColor, String pantsColor, String skinTone) {
        TrainerSkin skin = new TrainerSkin();
        skin.setId(id);
        skin.setName(name);
        skin.setCharacter(character);
        skin.setHatColor(hatColor);
        skin.setShirtColor(shirtColor);
        skin.setPantsColor(pantsColor);
        skin.setSkinTone(skinTone);
        return skin;
    }

    private CustomizationItem createItem(String id, String name, String category, String color) {
        CustomizationItem item = new CustomizationItem();
        item.setId(id);
        item.setName(name);
        item.setCategory(category);
        item.setColor(color);
        return item;
    }
}
