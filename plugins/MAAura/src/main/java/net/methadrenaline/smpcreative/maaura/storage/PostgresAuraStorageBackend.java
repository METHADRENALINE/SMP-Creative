package net.methadrenaline.smpcreative.maaura.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

final class PostgresAuraStorageBackend implements AuraStorageBackend {
    private static final String YAML_MIGRATION_KEY = "yaml_import_v1";

    private final JavaPlugin plugin;
    private final String jdbcUrl;
    private final String username;
    private final String password;
    private final String prefix;
    private final Map<UUID, PlayerData> players = new HashMap<>();
    private final Map<String, String> codes = new HashMap<>();
    private HikariDataSource dataSource;
    private long nextReloadAt;

    PostgresAuraStorageBackend(JavaPlugin plugin) {
        this.plugin = plugin;
        Properties properties = databaseProperties();
        jdbcUrl = property(properties, "jdbc-url", plugin.getConfig().getString("storage.postgres.jdbc-url", ""));
        username = property(properties, "username", plugin.getConfig().getString("storage.postgres.username", ""));
        password = property(properties, "password", plugin.getConfig().getString("storage.postgres.password", ""));
        prefix = tablePrefix(plugin.getConfig().getString("storage.postgres.table-prefix", "ma_aura_"));
    }

    @Override
    public void load() {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("storage.postgres.jdbc-url is empty");
        }

        try {
            Class.forName("org.postgresql.Driver");
            initializePool();
            try (Connection connection = connection()) {
                createSchema(connection);
            }
            if (plugin.getConfig().getBoolean("storage.postgres.import-yaml", false)) {
                importYamlOnce();
            }
            reloadCache();
            plugin.getLogger().info("Using PostgreSQL aura storage with a persistent connection pool.");
        } catch (ClassNotFoundException exception) {
            close();
            throw new IllegalStateException("PostgreSQL JDBC driver is missing", exception);
        } catch (SQLException exception) {
            close();
            throw new IllegalStateException("Could not initialize PostgreSQL aura storage: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void close() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
    }

    @Override
    public void syncValidCodes(Map<String, String> validCodes) {
        if (validCodes.isEmpty()) {
            return;
        }

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            for (Map.Entry<String, String> entry : validCodes.entrySet()) {
                upsertCode(connection, entry.getKey(), entry.getValue());
            }
            connection.commit();
            reloadCache();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not sync aura codes: " + exception.getMessage(), exception);
        }
    }

    @Override
    public void loadIfChanged() {
        long now = System.currentTimeMillis();
        if (now < nextReloadAt) {
            return;
        }
        nextReloadAt = now + 5000L;
        try {
            reloadCache();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not refresh PostgreSQL aura cache: " + exception.getMessage());
        }
    }

    @Override
    public boolean ownsAura(Player player, String auraId) {
        loadIfChanged();
        PlayerData data = players.get(player.getUniqueId());
        return data != null && data.ownedAuras.contains(auraId);
    }

    @Override
    public Set<String> auraOwners(String auraId) {
        loadIfChanged();
        Set<String> owners = new HashSet<>();
        for (PlayerData data : players.values()) {
            if (data.ownedAuras.contains(auraId)) {
                owners.add(data.name);
            }
        }
        return owners;
    }

    @Override
    public boolean auraEnabled(Player player, String auraId) {
        loadIfChanged();
        PlayerData data = players.get(player.getUniqueId());
        return data != null && data.enabledAuras.contains(auraId);
    }

    @Override
    public void setAuraEnabled(Player player, String auraId, boolean enabled) {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            upsertPlayer(connection, player.getUniqueId(), player.getName(), privacy(player), showOwn(player), showOthers(player), scope(player));
            if (enabled) {
                disableAllAuras(connection, player.getUniqueId());
            }
            upsertAura(connection, player.getUniqueId(), auraId, ownsAura(player, auraId), enabled);
            connection.commit();
            reloadCache();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not save aura enabled state: " + exception.getMessage());
        }
    }

    @Override
    public void setExclusiveAuraEnabled(Player player, String auraId, Set<String> auraIds) {
        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            upsertPlayer(connection, player.getUniqueId(), player.getName(), privacy(player), showOwn(player), showOthers(player), scope(player));
            disableAllAuras(connection, player.getUniqueId());
            upsertAura(connection, player.getUniqueId(), auraId, ownsAura(player, auraId), true);
            connection.commit();
            reloadCache();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not save exclusive aura state: " + exception.getMessage());
        }
    }

    @Override
    public String redeemAuraCode(Player player, String code, String auraId, Set<String> auraIds) {
        if (code == null || code.isBlank()) {
            return null;
        }

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            String resolvedAuraId = auraIdForCode(connection, code);
            if ((resolvedAuraId == null || resolvedAuraId.isBlank()) && auraId != null && !auraId.isBlank()) {
                upsertCode(connection, code, auraId);
                resolvedAuraId = auraIdForCode(connection, code);
            }
            if (resolvedAuraId == null || resolvedAuraId.isBlank() || isRedeemed(connection, code) || ownsAura(player, resolvedAuraId)) {
                connection.rollback();
                return null;
            }

            upsertPlayer(connection, player.getUniqueId(), player.getName(), privacy(player), showOwn(player), showOthers(player), scope(player));
            disableAllAuras(connection, player.getUniqueId());
            upsertAura(connection, player.getUniqueId(), resolvedAuraId, true, true);
            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into " + redeemedTable() + " (code, aura_id, player_uuid, player_name, redeemed_at) values (?, ?, ?, ?, now()) on conflict do nothing")) {
                statement.setString(1, code);
                statement.setString(2, resolvedAuraId);
                statement.setObject(3, player.getUniqueId());
                statement.setString(4, player.getName());
                if (statement.executeUpdate() == 0) {
                    connection.rollback();
                    return null;
                }
            }
            if (!deactivateCode(connection, code)) {
                connection.rollback();
                return null;
            }
            connection.commit();
            reloadCache();
            return resolvedAuraId;
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not redeem aura code: " + exception.getMessage());
            return null;
        }
    }

    @Override
    public String privacy(Player player) {
        loadIfChanged();
        PlayerData data = players.get(player.getUniqueId());
        return data == null ? "public" : data.privacy;
    }

    @Override
    public void setPrivacy(Player player, String privacy) {
        savePlayerSettings(player, normalized(privacy, "public"), showOwn(player), showOthers(player), scope(player));
    }

    @Override
    public boolean showOwn(Player player) {
        loadIfChanged();
        PlayerData data = players.get(player.getUniqueId());
        return data == null || data.showOwn;
    }

    @Override
    public void setShowOwn(Player player, boolean enabled) {
        savePlayerSettings(player, privacy(player), enabled, showOthers(player), scope(player));
    }

    @Override
    public boolean showOthers(Player player) {
        loadIfChanged();
        PlayerData data = players.get(player.getUniqueId());
        return data == null || data.showOthers;
    }

    @Override
    public void setShowOthers(Player player, boolean enabled) {
        savePlayerSettings(player, privacy(player), showOwn(player), enabled, scope(player));
    }

    @Override
    public String scope(Player player) {
        loadIfChanged();
        PlayerData data = players.get(player.getUniqueId());
        return data == null ? "all" : data.visibility;
    }

    @Override
    public void setScope(Player player, String scope) {
        savePlayerSettings(player, privacy(player), showOwn(player), showOthers(player), normalized(scope, "all"));
    }

    private void initializePool() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
            config.setPassword(password == null ? "" : password);
        }
        int maximumSize = Math.max(1, plugin.getConfig().getInt("storage.postgres.pool.maximum-size", 3));
        config.setMaximumPoolSize(maximumSize);
        config.setMinimumIdle(Math.min(maximumSize, Math.max(0, plugin.getConfig().getInt("storage.postgres.pool.minimum-idle", 1))));
        config.setConnectionTimeout(Math.max(250L, plugin.getConfig().getLong("storage.postgres.pool.connection-timeout-ms", 3000L)));
        config.setValidationTimeout(Math.max(250L, plugin.getConfig().getLong("storage.postgres.pool.validation-timeout-ms", 1000L)));
        config.setIdleTimeout(Math.max(10000L, plugin.getConfig().getLong("storage.postgres.pool.idle-timeout-ms", 600000L)));
        config.setMaxLifetime(Math.max(30000L, plugin.getConfig().getLong("storage.postgres.pool.max-lifetime-ms", 1800000L)));
        config.setKeepaliveTime(Math.max(30000L, plugin.getConfig().getLong("storage.postgres.pool.keepalive-time-ms", 300000L)));
        config.setPoolName("MAAura-PostgreSQL");
        dataSource = new HikariDataSource(config);
    }

    private void savePlayerSettings(Player player, String privacy, boolean showOwn, boolean showOthers, String visibility) {
        try (Connection connection = connection()) {
            upsertPlayer(connection, player.getUniqueId(), player.getName(), privacy, showOwn, showOthers, visibility);
            reloadCache();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not save aura player settings: " + exception.getMessage());
        }
    }

    private void importYamlOnce() throws SQLException {
        File file = storageFile();
        if (!file.exists()) {
            return;
        }

        try (Connection connection = connection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into " + migrationsTable() + " (migration_key, completed_at) values (?, now()) on conflict do nothing")) {
                statement.setString(1, YAML_MIGRATION_KEY);
                if (statement.executeUpdate() == 0) {
                    connection.rollback();
                    return;
                }
            }
            importYaml(connection, file);
            connection.commit();
        }
    }

    private void importYaml(Connection connection, File file) throws SQLException {
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection playersSection = yaml.getConfigurationSection("players");
        if (playersSection != null) {
            for (String uuidText : playersSection.getKeys(false)) {
                UUID uuid = parseUuid(uuidText);
                if (uuid == null) {
                    continue;
                }
                String base = "players." + uuidText;
                upsertPlayer(connection, uuid,
                        yaml.getString(base + ".name", uuidText),
                        yaml.getString(base + ".privacy", "public"),
                        yaml.getBoolean(base + ".show-own", true),
                        yaml.getBoolean(base + ".show-others", true),
                        yaml.getString(base + ".visibility", "all"));
                ConfigurationSection auras = yaml.getConfigurationSection(base + ".auras");
                if (auras != null) {
                    for (String auraId : auras.getKeys(false)) {
                        upsertAura(connection, uuid, auraId,
                                auras.getBoolean(auraId + ".owned", false),
                                auras.getBoolean(auraId + ".enabled", false));
                    }
                }
            }
        }

        ConfigurationSection redeemed = yaml.getConfigurationSection("redeemed-codes");
        if (redeemed == null) {
            return;
        }
        for (String code : redeemed.getKeys(false)) {
            String base = "redeemed-codes." + code;
            String auraId = yaml.getString(base + ".aura", "");
            UUID uuid = parseUuid(yaml.getString(base + ".player", ""));
            if (auraId.isBlank() || uuid == null) {
                continue;
            }
            String name = yaml.getString(base + ".name", uuid.toString());
            upsertCode(connection, code, auraId);
            try (PreparedStatement statement = connection.prepareStatement(
                    "insert into " + redeemedTable() + " (code, aura_id, player_uuid, player_name, redeemed_at) values (?, ?, ?, ?, ?) on conflict (code) do nothing")) {
                statement.setString(1, code);
                statement.setString(2, auraId);
                statement.setObject(3, uuid);
                statement.setString(4, name);
                statement.setTimestamp(5, Timestamp.from(Instant.ofEpochMilli(yaml.getLong(base + ".time", System.currentTimeMillis()))));
                statement.executeUpdate();
            }
            deactivateCode(connection, code);
        }
    }

    private void reloadCache() throws SQLException {
        Map<UUID, PlayerData> loadedPlayers = new HashMap<>();
        Map<String, String> loadedCodes = new HashMap<>();
        try (Connection connection = connection()) {
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("select player_uuid, name, privacy, show_own, show_others, visibility from " + playersTable())) {
                while (result.next()) {
                    UUID uuid = result.getObject("player_uuid", UUID.class);
                    loadedPlayers.put(uuid, new PlayerData(
                            result.getString("name"),
                            result.getString("privacy"),
                            result.getBoolean("show_own"),
                            result.getBoolean("show_others"),
                            result.getString("visibility")));
                }
            }
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("select player_uuid, aura_id, owned, enabled from " + aurasTable())) {
                while (result.next()) {
                    UUID uuid = result.getObject("player_uuid", UUID.class);
                    PlayerData data = loadedPlayers.computeIfAbsent(uuid, key -> new PlayerData(uuid.toString(), "public", true, true, "all"));
                    String auraId = result.getString("aura_id");
                    if (result.getBoolean("owned")) {
                        data.ownedAuras.add(auraId);
                    }
                    if (result.getBoolean("enabled")) {
                        data.enabledAuras.add(auraId);
                    }
                }
            }
            try (Statement statement = connection.createStatement();
                    ResultSet result = statement.executeQuery("select code, aura_id from " + codesTable() + " where active = true")) {
                while (result.next()) {
                    loadedCodes.put(result.getString("code"), result.getString("aura_id"));
                }
            }
        }

        synchronized (this) {
            players.clear();
            players.putAll(loadedPlayers);
            codes.clear();
            codes.putAll(loadedCodes);
        }
    }

    private void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("create table if not exists " + playersTable() + " (player_uuid uuid primary key, name text not null, privacy text not null default 'public' check (privacy in ('public', 'private')), show_own boolean not null default true, show_others boolean not null default true, visibility text not null default 'all' check (visibility in ('all', 'lobby', 'survival', 'creative')), updated_at timestamptz not null default now())");
            statement.executeUpdate("create table if not exists " + aurasTable() + " (player_uuid uuid not null references " + playersTable() + " (player_uuid) on delete cascade, aura_id text not null check (aura_id ~ '^[a-z0-9-]+$'), owned boolean not null default false, enabled boolean not null default false, updated_at timestamptz not null default now(), primary key (player_uuid, aura_id), check (not enabled or owned))");
            statement.executeUpdate("create table if not exists " + codesTable() + " (code text primary key check (code ~ '^[A-Z0-9]{5}(-[A-Z0-9]{5}){4}$'), aura_id text not null check (aura_id ~ '^[a-z0-9-]+$'), active boolean not null default true, created_at timestamptz not null default now())");
            statement.executeUpdate("create table if not exists " + redeemedTable() + " (code text primary key references " + codesTable() + " (code) on delete restrict, aura_id text not null check (aura_id ~ '^[a-z0-9-]+$'), player_uuid uuid not null references " + playersTable() + " (player_uuid) on delete restrict, player_name text not null, redeemed_at timestamptz not null default now())");
            statement.executeUpdate("create table if not exists " + migrationsTable() + " (migration_key text primary key, completed_at timestamptz not null default now())");
            statement.executeUpdate("update " + codesTable() + " c set active = false where c.active and exists (select 1 from " + redeemedTable() + " r where r.code = c.code)");
            statement.executeUpdate("create unique index if not exists " + prefix + "one_enabled_aura_per_player on " + aurasTable() + " (player_uuid) where enabled");
            statement.executeUpdate("create index if not exists " + prefix + "redeemed_codes_player_uuid_idx on " + redeemedTable() + " (player_uuid)");
        }
    }

    private void upsertPlayer(Connection connection, UUID uuid, String name, String privacy, boolean showOwn, boolean showOthers, String visibility) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + playersTable() + " (player_uuid, name, privacy, show_own, show_others, visibility, updated_at) values (?, ?, ?, ?, ?, ?, now()) on conflict (player_uuid) do update set name = excluded.name, privacy = excluded.privacy, show_own = excluded.show_own, show_others = excluded.show_others, visibility = excluded.visibility, updated_at = now()")) {
            statement.setObject(1, uuid);
            statement.setString(2, normalized(name, uuid.toString()));
            statement.setString(3, normalized(privacy, "public"));
            statement.setBoolean(4, showOwn);
            statement.setBoolean(5, showOthers);
            statement.setString(6, normalized(visibility, "all"));
            statement.executeUpdate();
        }
    }

    private void upsertAura(Connection connection, UUID uuid, String auraId, boolean owned, boolean enabled) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + aurasTable() + " (player_uuid, aura_id, owned, enabled, updated_at) values (?, ?, ?, ?, now()) on conflict (player_uuid, aura_id) do update set owned = " + aurasTable() + ".owned or excluded.owned, enabled = excluded.enabled, updated_at = now()")) {
            statement.setObject(1, uuid);
            statement.setString(2, auraId);
            statement.setBoolean(3, owned || enabled);
            statement.setBoolean(4, enabled);
            statement.executeUpdate();
        }
    }

    private void disableAllAuras(Connection connection, UUID uuid) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "update " + aurasTable() + " set enabled = false, updated_at = now() where player_uuid = ? and enabled")) {
            statement.setObject(1, uuid);
            statement.executeUpdate();
        }
    }

    private void upsertCode(Connection connection, String code, String auraId) throws SQLException {
        if (code == null || code.isBlank() || auraId == null || auraId.isBlank()) {
            return;
        }

        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + codesTable() + " (code, aura_id, active) values (?, ?, true) on conflict (code) do update set aura_id = excluded.aura_id")) {
            statement.setString(1, code);
            statement.setString(2, auraId);
            statement.executeUpdate();
        }
    }

    private String auraIdForCode(Connection connection, String code) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select aura_id from " + codesTable() + " where code = ? and active = true")) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return result.getString("aura_id");
                }
            }
        }
        return codes.get(code);
    }

    private boolean isRedeemed(Connection connection, String code) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("select 1 from " + redeemedTable() + " where code = ?")) {
            statement.setString(1, code);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        }
    }

    private boolean deactivateCode(Connection connection, String code) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("update " + codesTable() + " set active = false where code = ? and active = true")) {
            statement.setString(1, code);
            return statement.executeUpdate() == 1;
        }
    }

    private Connection connection() throws SQLException {
        if (dataSource == null) {
            throw new SQLException("PostgreSQL connection pool is not initialized");
        }
        return dataSource.getConnection();
    }

    private Properties databaseProperties() {
        String configured = plugin.getConfig().getString("storage.postgres.properties-file", "");
        if (configured == null || configured.isBlank()) {
            return new Properties();
        }
        File file = new File(configured);
        if (!file.isAbsolute()) {
            file = new File(plugin.getDataFolder(), configured);
        }
        Properties properties = new Properties();
        try (Reader reader = Files.newBufferedReader(file.toPath(), StandardCharsets.UTF_8)) {
            properties.load(reader);
            return properties;
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read PostgreSQL properties file: " + file, exception);
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

    private String playersTable() {
        return prefix + "players";
    }

    private String aurasTable() {
        return prefix + "player_auras";
    }

    private String codesTable() {
        return prefix + "codes";
    }

    private String redeemedTable() {
        return prefix + "redeemed_codes";
    }

    private String migrationsTable() {
        return prefix + "migrations";
    }

    private static String property(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String tablePrefix(String configured) {
        String value = configured == null || configured.isBlank() ? "ma_aura_" : configured.toLowerCase(Locale.ROOT);
        if (!value.matches("[a-z0-9_]+")) {
            return "ma_aura_";
        }
        return value;
    }

    private static String normalized(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static UUID parseUuid(String value) {
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static final class PlayerData {
        private final String name;
        private final String privacy;
        private final boolean showOwn;
        private final boolean showOthers;
        private final String visibility;
        private final Set<String> ownedAuras = new HashSet<>();
        private final Set<String> enabledAuras = new HashSet<>();

        private PlayerData(String name, String privacy, boolean showOwn, boolean showOthers, String visibility) {
            this.name = normalized(name, "");
            this.privacy = normalized(privacy, "public");
            this.showOwn = showOwn;
            this.showOthers = showOthers;
            this.visibility = normalized(visibility, "all");
        }
    }
}
