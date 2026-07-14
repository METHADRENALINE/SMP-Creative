package net.methadrenaline.smpcreative.maaura.storage;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.methadrenaline.smpcreative.maaura.aura.AuraCatalog;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class AuraStorage {
    private final JavaPlugin plugin;
    private AuraStorageBackend backend;

    public AuraStorage(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void load() {
        backend = createBackend();
        backend.load();
        backend.syncValidCodes(validCodes());
    }

    public void close() {
        if (backend != null) {
            backend.close();
        }
    }

    public void loadIfChanged() {
        backend().loadIfChanged();
    }

    public boolean ownsAura(Player player, String auraId) {
        String name = player.getName().toLowerCase(Locale.ROOT);
        if (configuredAuraOwners(auraId).stream().map(owner -> owner.toLowerCase(Locale.ROOT)).anyMatch(name::equals)) {
            return true;
        }
        return backend().ownsAura(player, auraId);
    }

    public Set<String> auraOwners(String auraId) {
        Set<String> owners = new HashSet<>(configuredAuraOwners(auraId));
        owners.addAll(backend().auraOwners(auraId));
        return owners;
    }

    public boolean auraEnabled(Player player, String auraId) {
        return backend().auraEnabled(player, auraId);
    }

    public void setAuraEnabled(Player player, String auraId, boolean enabled) {
        backend().setAuraEnabled(player, auraId, enabled);
    }

    public void setExclusiveAuraEnabled(Player player, String auraId) {
        backend().setExclusiveAuraEnabled(player, auraId, Set.copyOf(AuraCatalog.ids(plugin.getConfig())));
    }

    public String redeemAuraCode(Player player, String code, String auraId) {
        return backend().redeemAuraCode(player, code, auraId, Set.copyOf(AuraCatalog.ids(plugin.getConfig())));
    }

    public String privacy(Player player) {
        return backend().privacy(player).toLowerCase(Locale.ROOT);
    }

    public void setPrivacy(Player player, String privacy) {
        backend().setPrivacy(player, privacy);
    }

    public boolean showOwn(Player player) {
        return backend().showOwn(player);
    }

    public void setShowOwn(Player player, boolean enabled) {
        backend().setShowOwn(player, enabled);
    }

    public boolean showOthers(Player player) {
        return backend().showOthers(player);
    }

    public void setShowOthers(Player player, boolean enabled) {
        backend().setShowOthers(player, enabled);
    }

    public String scope(Player player) {
        return backend().scope(player).toLowerCase(Locale.ROOT);
    }

    public void setScope(Player player, String scope) {
        backend().setScope(player, scope);
    }

    private AuraStorageBackend backend() {
        if (backend == null) {
            load();
        }
        return backend;
    }

    private AuraStorageBackend createBackend() {
        String type = plugin.getConfig().getString("storage.type", "yaml");
        if ("postgres".equalsIgnoreCase(type) || "postgresql".equalsIgnoreCase(type)) {
            return new PostgresAuraStorageBackend(plugin);
        }
        return new YamlAuraStorageBackend(plugin);
    }

    private Set<String> configuredAuraOwners(String auraId) {
        Set<String> owners = new HashSet<>(plugin.getConfig().getStringList("auras." + auraId + ".owners"));
        String owner = plugin.getConfig().getString("auras." + auraId + ".owner", "");
        if (owner != null && !owner.isBlank()) {
            owners.add(owner);
        }
        return owners;
    }

    private Map<String, String> validCodes() {
        Map<String, String> codes = new HashMap<>();
        ConfigurationSection section = plugin.getConfig().getConfigurationSection("redeem.codes");
        if (section == null) {
            return codes;
        }
        for (String code : section.getKeys(false)) {
            String auraId = section.getString(code, "");
            if (auraId != null && !auraId.isBlank()) {
                codes.put(code, auraId);
            }
        }
        return codes;
    }
}
