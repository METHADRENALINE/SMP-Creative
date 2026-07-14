package net.methadrenaline.smpcreative.maaura.storage;

import java.util.Map;
import java.util.Set;
import org.bukkit.entity.Player;

interface AuraStorageBackend extends AutoCloseable {
    void load();

    void syncValidCodes(Map<String, String> validCodes);

    void loadIfChanged();

    boolean ownsAura(Player player, String auraId);

    Set<String> auraOwners(String auraId);

    boolean auraEnabled(Player player, String auraId);

    void setAuraEnabled(Player player, String auraId, boolean enabled);

    void setExclusiveAuraEnabled(Player player, String auraId, Set<String> auraIds);

    String redeemAuraCode(Player player, String code, String auraId, Set<String> auraIds);

    String privacy(Player player);

    void setPrivacy(Player player, String privacy);

    boolean showOwn(Player player);

    void setShowOwn(Player player, boolean enabled);

    boolean showOthers(Player player);

    void setShowOthers(Player player, boolean enabled);

    String scope(Player player);

    void setScope(Player player, String scope);

    @Override
    default void close() {
    }
}
