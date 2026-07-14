package net.methadrenaline.smpcreative.malang.storage;

import java.util.UUID;
import org.bukkit.entity.Player;

interface LanguageStorageBackend extends AutoCloseable {
    void load();

    void loadIfChanged();

    LanguagePreference preference(UUID uuid);

    void save(Player player, String language, String source);

    int manualCount();

    int clientCount();

    @Override
    default void close() {
    }
}
