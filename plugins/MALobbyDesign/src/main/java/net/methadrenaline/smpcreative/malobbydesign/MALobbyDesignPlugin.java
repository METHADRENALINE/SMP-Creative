package net.methadrenaline.smpcreative.malobbydesign;

import java.io.File;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.plugin.java.JavaPlugin;

public final class MALobbyDesignPlugin extends JavaPlugin {
    private int taskId = -1;
    private final Set<String> warnedConfigPaths = new HashSet<>();

    @Override
    public void onEnable() {
        saveDefaultConfig();

        String serverId = detectServerId();
        if (!"lobby".equals(serverId)) {
            getLogger().info("MALobbyDesign is lobby-only; detected server id '" + serverId + "', tasks disabled.");
            return;
        }

        long interval = Math.max(1L, getConfig().getLong("tasks.interval-ticks", 20L));
        taskId = Bukkit.getScheduler().runTaskTimer(this, this::runDesignTasks, 20L, interval).getTaskId();
        getLogger().info("MALobbyDesign ambience tasks started with interval " + interval + " tick(s).");
    }

    @Override
    public void onDisable() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    private void runDesignTasks() {
        if (getConfig().getBoolean("Speed Effect.enabled", true)) {
            applySpeedEffect();
        }

        runPortalParticles("Survival Portal Effects");
        runPortalParticles("Creative Portal Effects");
    }

    private void applySpeedEffect() {
        int durationTicks = Math.max(1, getConfig().getInt("Speed Effect.duration-ticks", 3600));
        int amplifier = Math.max(0, getConfig().getInt("Speed Effect.amplifier", 1));
        boolean hideParticles = getConfig().getBoolean("Speed Effect.hide-particles", true);
        PotionEffect effect = new PotionEffect(PotionEffectType.SPEED, durationTicks, amplifier, true, !hideParticles, true);
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.addPotionEffect(effect);
        }
    }

    private void runPortalParticles(String path) {
        if (!getConfig().getBoolean(path + ".enabled", true)) {
            return;
        }

        World world = resolveWorld(path);
        Particle particle = resolveParticle(path);
        if (world == null || particle == null) {
            return;
        }

        world.spawnParticle(
                particle,
                getConfig().getDouble(path + ".x", 0D),
                getConfig().getDouble(path + ".y", 0D),
                getConfig().getDouble(path + ".z", 0D),
                getConfig().getInt(path + ".count", 1),
                getConfig().getDouble(path + ".offset-x", 0D),
                getConfig().getDouble(path + ".offset-y", 0D),
                getConfig().getDouble(path + ".offset-z", 0D),
                getConfig().getDouble(path + ".speed", 0D),
                null,
                "force".equalsIgnoreCase(getConfig().getString(path + ".mode", "force"))
        );
    }

    private World resolveWorld(String path) {
        String worldName = getConfig().getString(path + ".world", "world");
        World world = Bukkit.getWorld(worldName == null ? "world" : worldName);
        if (world != null) {
            return world;
        }

        if (warnedConfigPaths.add(path + ".world")) {
            getLogger().warning("World '" + worldName + "' for " + path + " was not found; using the first loaded world.");
        }

        return Bukkit.getWorlds().isEmpty() ? null : Bukkit.getWorlds().get(0);
    }

    private Particle resolveParticle(String path) {
        String raw = getConfig().getString(path + ".particle", "minecraft:cloud");
        String key = normalizeParticle(raw);
        try {
            return Particle.valueOf(key);
        } catch (IllegalArgumentException exception) {
            if (warnedConfigPaths.add(path + ".particle")) {
                getLogger().warning("Particle '" + raw + "' for " + path + " is not known by this server build.");
            }
            return null;
        }
    }

    private static String normalizeParticle(String raw) {
        String value = raw == null ? "cloud" : raw;
        int namespace = value.indexOf(':');
        if (namespace >= 0 && namespace < value.length() - 1) {
            value = value.substring(namespace + 1);
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_");
    }

    private String detectServerId() {
        String configured = getConfig().getString("server.id", "auto");
        if (configured != null && !"auto".equalsIgnoreCase(configured)) {
            return normalize(configured);
        }

        try {
            return normalize(new File(".").getCanonicalFile().getName());
        } catch (Exception ignored) {
            return "unknown";
        }
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replace(" ", "").replace("_", "").replace("-", "");
    }
}
