package net.methadrenaline.smpcreative.malang.util;

import com.destroystokyo.paper.profile.ProfileProperty;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public final class ItemFactory {
    private final LegacyComponentSerializer legacy;

    public ItemFactory(LegacyComponentSerializer legacy) {
        this.legacy = legacy;
    }

    public ItemStack customHead(String texture) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();
        if (meta instanceof SkullMeta skullMeta && texture != null && !texture.isBlank()) {
            com.destroystokyo.paper.profile.PlayerProfile profile = Bukkit.createProfile(
                    UUID.nameUUIDFromBytes(texture.getBytes(StandardCharsets.UTF_8)),
                    null
            );
            profile.setProperty(new ProfileProperty("textures", texture));
            skullMeta.setPlayerProfile(profile);
            item.setItemMeta(skullMeta);
        }
        return item;
    }

    public void applyItemText(ItemMeta meta, String name, List<String> lore) {
        if (meta == null) {
            return;
        }
        meta.displayName(itemText(name));
        meta.lore(itemTextList(lore));
    }

    public Component itemText(String text) {
        return legacy.deserialize(text == null ? "" : text)
                .decoration(TextDecoration.ITALIC, false);
    }

    public List<Component> itemTextList(List<String> lines) {
        List<Component> components = new ArrayList<>();
        for (String line : lines) {
            components.add(itemText(line));
        }
        return components;
    }

    public Material material(FileConfiguration config, String path, Material fallback) {
        String configured = config.getString(path, fallback.name());
        try {
            return Material.valueOf(configured.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
