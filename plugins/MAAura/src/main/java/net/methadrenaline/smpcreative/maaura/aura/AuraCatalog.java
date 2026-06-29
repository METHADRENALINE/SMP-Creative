package net.methadrenaline.smpcreative.maaura.aura;

import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;

public final class AuraCatalog {
    public static final String FIND_ZEN = "find-your-zen";
    public static final String LOYALTY = "loyalty-aura";

    private AuraCatalog() {
    }

    public static List<String> ids(FileConfiguration config) {
        ConfigurationSection section = config.getConfigurationSection("auras");
        if (section == null) {
            return List.of(FIND_ZEN, LOYALTY);
        }
        return new ArrayList<>(section.getKeys(false));
    }
}
