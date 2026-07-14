package net.methadrenaline.smpcreative.malang.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.methadrenaline.smpcreative.malang.lang.LanguageCode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

final class YamlLanguageStorageBackend implements LanguageStorageBackend {
    private final JavaPlugin plugin;
    private final Map<UUID, LanguagePreference> preferences = new HashMap<>();
    private File playersFile;
    private long playersFileLastModified = -1L;

    YamlLanguageStorageBackend(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        playersFile = playersFile();
        YamlConfiguration playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        playersFileLastModified = playersFile.exists() ? playersFile.lastModified() : -1L;
        preferences.clear();
        ConfigurationSection section = playersConfig.getConfigurationSection("players");
        if (section == null) {
            return;
        }

        for (String uuidText : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                String language = LanguageCode.normalize(section.getString(uuidText + ".language", ""));
                if (!LanguageCode.isSupported(language)) {
                    continue;
                }
                String source = normalizeSource(section.getString(uuidText + ".source", "manual"));
                preferences.put(uuid, new LanguagePreference(language, source));
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid UUID in players.yml: " + uuidText);
            }
        }
    }

    @Override
    public void loadIfChanged() {
        if (playersFile == null) {
            load();
            return;
        }
        long lastModified = playersFile.exists() ? playersFile.lastModified() : -1L;
        if (lastModified != playersFileLastModified) {
            load();
        }
    }

    @Override
    public LanguagePreference preference(UUID uuid) {
        return preferences.get(uuid);
    }

    @Override
    public void save(Player player, String language, String source) {
        String normalizedSource = normalizeSource(source);
        YamlConfiguration latestConfig = YamlConfiguration.loadConfiguration(playersFile);
        String base = "players." + player.getUniqueId();
        latestConfig.set(base + ".language", language);
        latestConfig.set(base + ".source", normalizedSource);
        try {
            File parent = playersFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create language storage folder: " + parent);
            }
            latestConfig.save(playersFile);
            playersFileLastModified = playersFile.exists() ? playersFile.lastModified() : -1L;
            preferences.put(player.getUniqueId(), new LanguagePreference(language, normalizedSource));
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save language preference for " + player.getUniqueId() + ": " + exception.getMessage());
        }
    }

    @Override
    public int manualCount() {
        return (int) preferences.values().stream().filter(LanguagePreference::manual).count();
    }

    @Override
    public int clientCount() {
        return preferences.size() - manualCount();
    }

    private File playersFile() {
        String configured = plugin.getConfig().getString("settings.players-file", "");
        if (configured != null && !configured.isBlank()) {
            File file = new File(configured);
            return file.isAbsolute() ? file : new File(plugin.getDataFolder(), configured);
        }
        return new File(plugin.getDataFolder(), "players.yml");
    }

    private static String normalizeSource(String source) {
        return "client".equalsIgnoreCase(source) ? "client" : "manual";
    }
}
