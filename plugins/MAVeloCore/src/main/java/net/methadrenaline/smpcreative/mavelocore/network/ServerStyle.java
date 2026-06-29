package net.methadrenaline.smpcreative.mavelocore.network;

import net.kyori.adventure.text.format.NamedTextColor;

public record ServerStyle(String icon, NamedTextColor iconColor, String label, NamedTextColor labelColor) {
    public static ServerStyle fromServerId(String serverId) {
        return switch (ServerId.normalize(serverId)) {
            case "lobby" -> new ServerStyle("🏛", NamedTextColor.YELLOW, "Lobby", NamedTextColor.GOLD);
            case "survival", "smp" -> new ServerStyle("⛏", NamedTextColor.DARK_RED, "SMP", NamedTextColor.RED);
            case "creative" -> new ServerStyle("🏗", NamedTextColor.DARK_AQUA, "Creative", NamedTextColor.AQUA);
            default -> new ServerStyle("🏛", NamedTextColor.YELLOW, "Lobby", NamedTextColor.GOLD);
        };
    }
}
