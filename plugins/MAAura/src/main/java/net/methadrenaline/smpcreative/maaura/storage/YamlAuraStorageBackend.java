package net.methadrenaline.smpcreative.maaura.storage;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

final class YamlAuraStorageBackend implements AuraStorageBackend {
    private final JavaPlugin plugin;
    private File storageFile;
    private YamlConfiguration storage;
    private long storageLastModified = -1L;

    YamlAuraStorageBackend(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        storageFile = storageFile();
        storage = YamlConfiguration.loadConfiguration(storageFile);
        storageLastModified = storageFile.exists() ? storageFile.lastModified() : -1L;
    }

    @Override
    public void syncValidCodes(Map<String, String> validCodes) {
    }

    @Override
    public void loadIfChanged() {
        if (storageFile == null) {
            load();
            return;
        }

        long lastModified = storageFile.exists() ? storageFile.lastModified() : -1L;
        if (lastModified != storageLastModified) {
            load();
        }
    }

    @Override
    public boolean ownsAura(Player player, String auraId) {
        loadIfChanged();
        return storage.getBoolean(playerPath(player) + ".auras." + auraId + ".owned", false);
    }

    @Override
    public Set<String> auraOwners(String auraId) {
        Set<String> owners = new HashSet<>();
        loadIfChanged();
        ConfigurationSection players = storage.getConfigurationSection("players");
        if (players != null) {
            for (String uuid : players.getKeys(false)) {
                if (players.getBoolean(uuid + ".auras." + auraId + ".owned", false)) {
                    owners.add(players.getString(uuid + ".name", uuid));
                }
            }
        }
        return owners;
    }

    @Override
    public boolean auraEnabled(Player player, String auraId) {
        loadIfChanged();
        return storage.getBoolean(playerPath(player) + ".auras." + auraId + ".enabled", false);
    }

    @Override
    public void setAuraEnabled(Player player, String auraId, boolean enabled) {
        loadIfChanged();
        storage.set(playerPath(player) + ".name", player.getName());
        storage.set(playerPath(player) + ".auras." + auraId + ".enabled", enabled);
        save();
    }

    @Override
    public void setExclusiveAuraEnabled(Player player, String auraId, Set<String> auraIds) {
        loadIfChanged();
        applyExclusiveAuraEnabled(player, auraId, auraIds);
        save();
    }

    @Override
    public String redeemAuraCode(Player player, String code, String auraId, Set<String> auraIds) {
        if (code == null || code.isBlank() || auraId == null || auraId.isBlank()) {
            return null;
        }

        load();
        if (storage.isConfigurationSection("redeemed-codes." + code) || ownsAura(player, auraId)) {
            return null;
        }

        String path = playerPath(player);
        storage.set("redeemed-codes." + code + ".aura", auraId);
        storage.set("redeemed-codes." + code + ".player", player.getUniqueId().toString());
        storage.set("redeemed-codes." + code + ".name", player.getName());
        storage.set("redeemed-codes." + code + ".time", System.currentTimeMillis());
        storage.set(path + ".name", player.getName());
        storage.set(path + ".auras." + auraId + ".owned", true);
        applyExclusiveAuraEnabled(player, auraId, auraIds);
        save();
        return auraId;
    }

    @Override
    public String privacy(Player player) {
        loadIfChanged();
        return storage.getString(playerPath(player) + ".privacy", "public");
    }

    @Override
    public void setPrivacy(Player player, String privacy) {
        loadIfChanged();
        storage.set(playerPath(player) + ".name", player.getName());
        storage.set(playerPath(player) + ".privacy", privacy);
        save();
    }

    @Override
    public boolean showOwn(Player player) {
        loadIfChanged();
        return storage.getBoolean(playerPath(player) + ".show-own", true);
    }

    @Override
    public void setShowOwn(Player player, boolean enabled) {
        loadIfChanged();
        storage.set(playerPath(player) + ".name", player.getName());
        storage.set(playerPath(player) + ".show-own", enabled);
        save();
    }

    @Override
    public boolean showOthers(Player player) {
        loadIfChanged();
        return storage.getBoolean(playerPath(player) + ".show-others", true);
    }

    @Override
    public void setShowOthers(Player player, boolean enabled) {
        loadIfChanged();
        storage.set(playerPath(player) + ".name", player.getName());
        storage.set(playerPath(player) + ".show-others", enabled);
        save();
    }

    @Override
    public String scope(Player player) {
        loadIfChanged();
        return storage.getString(playerPath(player) + ".visibility", "all");
    }

    @Override
    public void setScope(Player player, String scope) {
        loadIfChanged();
        storage.set(playerPath(player) + ".name", player.getName());
        storage.set(playerPath(player) + ".visibility", scope);
        save();
    }

    private void applyExclusiveAuraEnabled(Player player, String auraId, Set<String> auraIds) {
        String path = playerPath(player);
        storage.set(path + ".name", player.getName());
        for (String currentAuraId : auraIds) {
            storage.set(path + ".auras." + currentAuraId + ".enabled", currentAuraId.equals(auraId));
        }
    }

    private void save() {
        try {
            File parent = storageFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                plugin.getLogger().warning("Could not create aura storage folder: " + parent);
            }
            storage.save(storageFile);
            storageLastModified = storageFile.exists() ? storageFile.lastModified() : -1L;
        } catch (IOException exception) {
            plugin.getLogger().warning("Could not save aura storage: " + exception.getMessage());
        }
    }

    private File storageFile() {
        String configured = plugin.getConfig().getString("settings.storage-file", "ma-aura-players.yml");
        File file = new File(configured == null ? "ma-aura-players.yml" : configured);
        if (file.isAbsolute()) {
            return file;
        }

        return new File(plugin.getDataFolder(), file.getPath());
    }

    private String playerPath(Player player) {
        return "players." + player.getUniqueId();
    }
}
