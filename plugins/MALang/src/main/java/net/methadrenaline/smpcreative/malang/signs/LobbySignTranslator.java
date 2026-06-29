package net.methadrenaline.smpcreative.malang.signs;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.methadrenaline.smpcreative.malang.MALangPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.sign.Side;
import org.bukkit.block.sign.SignSide;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class LobbySignTranslator {
    private final MALangPlugin plugin;

    public LobbySignTranslator(MALangPlugin plugin) {
        this.plugin = plugin;
    }

    public void schedule(Player player, int delayTicks) {
        if (!enabled()) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                updateVisible(player);
            }
        }, Math.max(1, delayTicks));
    }

    public void schedule(Player player, Chunk chunk, int delayTicks) {
        if (!enabled()) {
            return;
        }

        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && chunk.isLoaded() && player.getWorld().equals(chunk.getWorld())) {
                updateInChunk(player, chunk.getX(), chunk.getZ());
            }
        }, Math.max(1, delayTicks));
    }

    private void updateVisible(Player player) {
        int radius = Math.max(0, Math.min(12, plugin.getConfig().getInt("lobby-signs.refresh-radius-chunks", 5)));
        int centerX = player.getLocation().getBlockX() >> 4;
        int centerZ = player.getLocation().getBlockZ() >> 4;

        for (int x = centerX - radius; x <= centerX + radius; x++) {
            for (int z = centerZ - radius; z <= centerZ + radius; z++) {
                updateInChunk(player, x, z);
            }
        }
    }

    private void updateInChunk(Player player, int chunkX, int chunkZ) {
        ConfigurationSection signs = plugin.getConfig().getConfigurationSection("lobby-signs.signs");
        if (signs == null) {
            return;
        }

        for (String id : signs.getKeys(false)) {
            String base = "lobby-signs.signs." + id + ".";
            World world = signWorld(player, base);
            if (world == null || !player.getWorld().equals(world)) {
                continue;
            }

            int x = plugin.getConfig().getInt(base + "x");
            int z = plugin.getConfig().getInt(base + "z");
            if ((x >> 4) == chunkX && (z >> 4) == chunkZ) {
                update(player, base);
            }
        }
    }

    private void update(Player player, String base) {
        World world = signWorld(player, base);
        if (world == null || !player.getWorld().equals(world)) {
            return;
        }

        int x = plugin.getConfig().getInt(base + "x");
        int y = plugin.getConfig().getInt(base + "y");
        int z = plugin.getConfig().getInt(base + "z");
        if (!world.isChunkLoaded(x >> 4, z >> 4)) {
            return;
        }

        Location location = new Location(world, x, y, z);
        BlockState state = location.getBlock().getState();
        if (!(state instanceof Sign sign)) {
            return;
        }

        apply(player, sign, base);
        try {
            player.sendBlockUpdate(location, sign);
        } catch (IllegalArgumentException exception) {
            plugin.getLogger().warning("Could not send translated sign at " + location + ": " + exception.getMessage());
        }
    }

    private void apply(Player player, Sign sign, String base) {
        String language = plugin.getLanguageCode(player);
        if (MALangPlugin.ENGLISH.equals(language)) {
            return;
        }

        List<String> lines = translatedLines(base, language);
        Set<Integer> preserveLines = preserveLines(base, language);
        if (lines.isEmpty()) {
            int line = Math.max(0, Math.min(3, plugin.getConfig().getInt(base + "line", 1)));
            lines = new ArrayList<>(List.of("", "", "", ""));
            lines.set(line, plugin.getConfig().getString(base + language + "-text", ""));
        }

        applySide(sign.getSide(Side.FRONT), lines, preserveLines);
    }

    private List<String> translatedLines(String base, String language) {
        List<String> lines = plugin.getConfig().getStringList(base + "languages." + language + ".lines");
        if (lines.isEmpty() && MALangPlugin.UKRAINIAN.equals(language)) {
            lines = plugin.getConfig().getStringList(base + "ua-lines");
        }
        return lines;
    }

    private Set<Integer> preserveLines(String base, String language) {
        List<Integer> lines = plugin.getConfig().getIntegerList(base + "languages." + language + ".preserve-lines");
        if (lines.isEmpty()) {
            lines = plugin.getConfig().getIntegerList(base + "preserve-lines");
        }
        return new HashSet<>(lines);
    }

    private void applySide(SignSide side, List<String> lines, Set<Integer> preserveLines) {
        for (int line = 0; line < 4; line++) {
            if (preserveLines.contains(line)) {
                continue;
            }

            String text = line < lines.size() ? lines.get(line) : "";
            side.line(line, Component.text(text == null ? "" : text));
        }
    }

    private World signWorld(Player player, String base) {
        String worldName = plugin.getConfig().getString(base + "world", player.getWorld().getName());
        World world = Bukkit.getWorld(worldName);
        return world == null ? player.getWorld() : world;
    }

    private boolean enabled() {
        return plugin.getConfig().getBoolean("lobby-signs.enabled", false);
    }
}
