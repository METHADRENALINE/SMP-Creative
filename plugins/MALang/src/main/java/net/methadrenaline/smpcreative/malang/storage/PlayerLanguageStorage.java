package net.methadrenaline.smpcreative.malang.storage;

import java.util.UUID;
import net.methadrenaline.smpcreative.malang.lang.LanguageCode;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlayerLanguageStorage {
    private final JavaPlugin plugin;
    private LanguageStorageBackend backend;

    public PlayerLanguageStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        backend().load();
    }

    public void close() {
        if (backend != null) {
            backend.close();
        }
    }

    public void loadIfChanged() {
        backend().loadIfChanged();
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
        LanguagePreference preference = backend().preference(player.getUniqueId());
        if (preference != null) {
            return preference.language();
        }

        if (!plugin.getConfig().getBoolean("settings.auto-detect-client-language", true)) {
            return LanguageCode.normalize(plugin.getConfig().getString("settings.default-language", LanguageCode.ENGLISH));
        }
        return LanguageCode.detect(player.getLocale());
    }

    public boolean hasManualOverride(UUID uuid) {
        loadIfChanged();
        LanguagePreference preference = backend().preference(uuid);
        return preference != null && preference.manual();
    }

    public String manualOverride(UUID uuid) {
        loadIfChanged();
        LanguagePreference preference = backend().preference(uuid);
        return preference != null && preference.manual() ? preference.language() : null;
    }

    public String setClientLanguage(Player player, String language) {
        loadIfChanged();
        LanguagePreference previous = backend().preference(player.getUniqueId());
        backend().save(player, language, "client");
        return previous != null && !previous.manual() ? previous.language() : null;
    }

    public void setAutoLanguage(Player player) {
        backend().save(player, LanguageCode.detect(player.getLocale()), "client");
    }

    public void setManualLanguage(Player player, String language) {
        backend().save(player, language, "manual");
    }

    public int manualCount() {
        return backend().manualCount();
    }

    public int clientCount() {
        return backend().clientCount();
    }

    private LanguageStorageBackend backend() {
        if (backend == null) {
            String type = plugin.getConfig().getString("storage.type", "yaml");
            backend = "postgres".equalsIgnoreCase(type) || "postgresql".equalsIgnoreCase(type)
                    ? new PostgresLanguageStorageBackend(plugin)
                    : new YamlLanguageStorageBackend(plugin);
        }
        return backend;
    }
}
