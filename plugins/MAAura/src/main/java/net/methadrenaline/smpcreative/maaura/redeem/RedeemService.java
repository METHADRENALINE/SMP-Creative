package net.methadrenaline.smpcreative.maaura.redeem;

import net.methadrenaline.smpcreative.maaura.storage.AuraStorage;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class RedeemService {
    private final JavaPlugin plugin;
    private final AuraStorage auraStorage;

    public RedeemService(JavaPlugin plugin, AuraStorage auraStorage) {
        this.plugin = plugin;
        this.auraStorage = auraStorage;
    }

    public String redeem(Player player, String rawCode) {
        String code = RedeemCodeNormalizer.normalize(rawCode);
        if (code == null || code.isBlank()) {
            return null;
        }

        String auraId = plugin.getConfig().getString("redeem.codes." + code, "");
        return auraStorage.redeemAuraCode(player, code, auraId);
    }
}
