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

public final class PlayerLanguageStorage {
    private final JavaPlugin plugin;
    private final Map<UUID, String> overrides = new HashMap<>();
    private final Map<UUID, String> clientLanguages = new HashMap<>();
    private File playersFile;
    private long playersFileLastModified = -1L;

    public PlayerLanguageStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        playersFile = playersFile();
        YamlConfiguration playersConfig = YamlConfiguration.loadConfiguration(playersFile);
        playersFileLastModified = playersFile.exists() ? playersFile.lastModified() : -1L;
        overrides.clear();
        clientLanguages.clear();

        ConfigurationSection section = playersConfig.getConfigurationSection("players");
        if (section == null) {
            return;
        }

        for (String uuidText : section.getKeys(false)) {
            try {
                UUID uuid = UUID.fromString(uuidText);
                String rawLanguage = section.getString(uuidText + ".language", "");
                if (rawLanguage == null || rawLanguage.isBlank()) {
                    continue;
                }

                String language = LanguageCode.normalize(rawLanguage);
                if (LanguageCode.isSupported(language)) {
                    String source = section.getString(uuidText + ".source", "manual");
                    if ("client".equalsIgnoreCase(source)) {
                        clientLanguages.put(uuid, language);
                    } else {
                        overrides.put(uuid, language);
                    }
                }
            } catch (IllegalArgumentException ignored) {
                plugin.getLogger().warning("Ignoring invalid UUID in players.yml: " + uuidText);
            }
        }
    }

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

    public String getLanguageCode(Player player) {
        loadIfChanged();
        return languageCodeFromCache(player);
    }

    public String getFreshLanguageCode(Player player) {
        load();
        return languageCodeFromCache(player);
    }

    public String languageCodeFromCache(Player player) {
        String override = overrides.get(player.getUniqueId());
        if (override != null) {
            return override;
        }

        if (!plugin.getConfig().getBoolean("settings.auto-detect-client-language", true)) {
            return LanguageCode.normalize(plugin.getConfig().getString("settings.default-language", LanguageCode.ENGLISH));
        }

        String clientLanguage = clientLanguages.get(player.getUniqueId());
        if (clientLanguage != null) {
            return clientLanguage;
        }

        return LanguageCode.detect(player.getLocale());
    }

    public boolean hasManualOverride(UUID uuid) {
        return overrides.containsKey(uuid);
    }

    public String manualOverride(UUID uuid) {
        return overrides.get(uuid);
    }

    public String setClientLanguage(UUID uuid, String language) {
        String previous = clientLanguages.put(uuid, language);
        saveLanguagePreference(uuid, language, "client");
        return previous;
    }

    public void setAutoLanguage(Player player) {
        UUID uuid = player.getUniqueId();
        overrides.remove(uuid);
        String detected = LanguageCode.detect(player.getLocale());
        clientLanguages.put(uuid, detected);
        saveLanguagePreference(uuid, detected, "client");
    }

    public void setManualLanguage(Player player, String language) {
        UUID uuid = player.getUniqueId();
        overrides.put(uuid, language);
        clientLanguages.remove(uuid);
        saveLanguagePreference(uuid, language, "manual");
    }

    public int manualCount() {
        return overrides.size();
    }

    public int clientCount() {
        return clientLanguages.size();
    }

    private File playersFile() {
        String configured = plugin.getConfig().getString("settings.players-file", "");
        if (configured != null && !configured.isBlank()) {
            File file = new File(configured);
            if (file.isAbsolute()) {
                return file;
            }
            return new File(plugin.getDataFolder(), configured);
        }

        return new File(plugin.getDataFolder(), "players.yml");
    }

    private void saveLanguagePreference(UUID uuid, String language, String source) {
        YamlConfiguration latestConfig = YamlConfiguration.loadConfiguration(playersFile);
        latestConfig.set("players." + uuid + ".language", language);
        latestConfig.set("players." + uuid + ".source", source);
        try {
            File parent = playersFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create language storage folder: " + parent);
            }
            latestConfig.save(playersFile);
            playersFileLastModified = playersFile.exists() ? playersFile.lastModified() : -1L;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save language preference for " + uuid + ": " + exception.getMessage());
        }
    }
}
