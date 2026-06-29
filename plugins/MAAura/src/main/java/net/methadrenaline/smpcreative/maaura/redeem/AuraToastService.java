package net.methadrenaline.smpcreative.maaura.redeem;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.advancement.Advancement;
import org.bukkit.advancement.AdvancementProgress;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import net.methadrenaline.smpcreative.maaura.lang.AuraMessages;
import net.methadrenaline.smpcreative.maaura.menu.AuraItemFactory;

public final class AuraToastService {
    private final JavaPlugin plugin;
    private final AuraMessages messages;

    public AuraToastService(JavaPlugin plugin, AuraMessages messages) {
        this.plugin = plugin;
        this.messages = messages;
    }

    public void show(Player player, String auraId) {
        player.playSound(player.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        NamespacedKey key = new NamespacedKey(plugin, "redeem_" + player.getUniqueId().toString().replace("-", ""));
        Bukkit.getUnsafe().removeAdvancement(key);
        Bukkit.getUnsafe().loadAdvancement(key, advancementJson(player, auraId));
        Advancement advancement = Bukkit.getAdvancement(key);
        if (advancement == null) {
            return;
        }

        AdvancementProgress progress = player.getAdvancementProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            progress.awardCriteria(criterion);
        }
        Bukkit.getScheduler().runTaskLater(plugin, () -> Bukkit.getUnsafe().removeAdvancement(key), 40L);
    }

    private String advancementJson(Player player, String auraId) {
        Material material = AuraItemFactory.material(
                plugin.getConfig().getString("auras." + auraId + ".material", "CHERRY_SAPLING"),
                Material.CHERRY_SAPLING
        );
        return "{"
                + "\"criteria\":{\"redeemed\":{\"trigger\":\"minecraft:impossible\"}},"
                + "\"display\":{"
                + "\"icon\":{\"id\":\"" + material.getKey() + "\"},"
                + "\"title\":{\"text\":\"" + jsonEscape(messages.auraName(player, auraId)) + "\",\"color\":\"light_purple\",\"italic\":false},"
                + "\"description\":{\"text\":\"" + jsonEscape(messages.text(player, "aura-unlocked")) + "\",\"color\":\"gray\",\"italic\":false},"
                + "\"frame\":\"challenge\","
                + "\"show_toast\":true,"
                + "\"announce_to_chat\":false,"
                + "\"hidden\":true"
                + "}"
                + "}";
    }

    private String jsonEscape(String text) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < (text == null ? "" : text).length(); i++) {
            char character = text.charAt(i);
            switch (character) {
                case '"' -> builder.append("\\\"");
                case '\\' -> builder.append("\\\\");
                case '\b' -> builder.append("\\b");
                case '\f' -> builder.append("\\f");
                case '\n' -> builder.append("\\n");
                case '\r' -> builder.append("\\r");
                case '\t' -> builder.append("\\t");
                default -> {
                    if (character < 0x20) {
                        builder.append(String.format("\\u%04x", (int) character));
                    } else {
                        builder.append(character);
                    }
                }
            }
        }
        return builder.toString();
    }
}
