package net.methadrenaline.smpcreative.malang.storage;

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
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import net.methadrenaline.smpcreative.malang.lang.LanguageCode;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

final class PostgresLanguageStorageBackend implements LanguageStorageBackend {
    private static final String YAML_MIGRATION_KEY = "yaml_import_v1";

    private final JavaPlugin plugin;
    private final Map<UUID, LanguagePreference> preferences = new HashMap<>();
    private HikariDataSource dataSource;
    private String prefix;
    private long nextReloadAt;

    PostgresLanguageStorageBackend(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public void load() {
        try {
            if (dataSource == null) {
                Class.forName("org.postgresql.Driver");
                initializePool();
                try (Connection connection = connection()) {
                    createSchema(connection);
                }
                if (plugin.getConfig().getBoolean("storage.postgres.import-yaml", false)) {
                    importYamlOnce();
                }
                plugin.getLogger().info("Using PostgreSQL language storage with a persistent connection pool.");
            }
            reloadCache();
        } catch (ClassNotFoundException exception) {
            close();
            throw new IllegalStateException("PostgreSQL JDBC driver is missing", exception);
        } catch (SQLException exception) {
            close();
            throw new IllegalStateException("Could not initialize PostgreSQL language storage: " + exception.getMessage(), exception);
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
    public void loadIfChanged() {
        long now = System.currentTimeMillis();
        if (now < nextReloadAt) {
            return;
        }
        nextReloadAt = now + 1000L;
        try {
            reloadCache();
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not refresh PostgreSQL language cache: " + exception.getMessage());
        }
    }

    @Override
    public LanguagePreference preference(UUID uuid) {
        return preferences.get(uuid);
    }

    @Override
    public void save(Player player, String language, String source) {
        String normalizedLanguage = LanguageCode.normalize(language);
        String normalizedSource = normalizeSource(source);
        if (!LanguageCode.isSupported(normalizedLanguage)) {
            throw new IllegalArgumentException("Unsupported language code: " + language);
        }

        try (Connection connection = connection();
                PreparedStatement statement = connection.prepareStatement(
                        "insert into " + languagesTable() + " (player_uuid, player_name, language_code, source, updated_at) values (?, ?, ?, ?, now()) on conflict (player_uuid) do update set player_name = excluded.player_name, language_code = excluded.language_code, source = excluded.source, updated_at = now()")) {
            statement.setObject(1, player.getUniqueId());
            statement.setString(2, player.getName());
            statement.setString(3, normalizedLanguage);
            statement.setString(4, normalizedSource);
            statement.executeUpdate();
            synchronized (this) {
                preferences.put(player.getUniqueId(), new LanguagePreference(normalizedLanguage, normalizedSource));
            }
        } catch (SQLException exception) {
            plugin.getLogger().warning("Could not save language preference for " + player.getUniqueId() + ": " + exception.getMessage());
        }
    }

    @Override
    public int manualCount() {
        return (int) preferences.values().stream().filter(LanguagePreference::manual).count();
    }

    @Override
    public int clientCount() {
        return preferences.size() - manualCount();
    }

    private void initializePool() {
        Properties properties = databaseProperties();
        String jdbcUrl = property(properties, "jdbc-url", plugin.getConfig().getString("storage.postgres.jdbc-url", ""));
        String username = property(properties, "username", plugin.getConfig().getString("storage.postgres.username", ""));
        String password = property(properties, "password", plugin.getConfig().getString("storage.postgres.password", ""));
        prefix = tablePrefix(plugin.getConfig().getString("storage.postgres.table-prefix", "ma_"));
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("storage.postgres.jdbc-url is empty");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        if (username != null && !username.isBlank()) {
            config.setUsername(username);
            config.setPassword(password == null ? "" : password);
        }
        int maximumSize = Math.max(1, plugin.getConfig().getInt("storage.postgres.pool.maximum-size", 2));
        config.setMaximumPoolSize(maximumSize);
        config.setMinimumIdle(Math.min(maximumSize, Math.max(0, plugin.getConfig().getInt("storage.postgres.pool.minimum-idle", 1))));
        config.setConnectionTimeout(Math.max(250L, plugin.getConfig().getLong("storage.postgres.pool.connection-timeout-ms", 3000L)));
        config.setValidationTimeout(Math.max(250L, plugin.getConfig().getLong("storage.postgres.pool.validation-timeout-ms", 1000L)));
        config.setIdleTimeout(Math.max(10000L, plugin.getConfig().getLong("storage.postgres.pool.idle-timeout-ms", 600000L)));
        config.setMaxLifetime(Math.max(30000L, plugin.getConfig().getLong("storage.postgres.pool.max-lifetime-ms", 1800000L)));
        config.setKeepaliveTime(Math.max(30000L, plugin.getConfig().getLong("storage.postgres.pool.keepalive-time-ms", 300000L)));
        config.setPoolName("MALang-PostgreSQL");
        dataSource = new HikariDataSource(config);
    }

    private void reloadCache() throws SQLException {
        Map<UUID, LanguagePreference> loaded = new HashMap<>();
        try (Connection connection = connection();
                Statement statement = connection.createStatement();
                ResultSet result = statement.executeQuery("select player_uuid, language_code, source from " + languagesTable())) {
            while (result.next()) {
                String language = LanguageCode.normalize(result.getString("language_code"));
                if (LanguageCode.isSupported(language)) {
                    loaded.put(result.getObject("player_uuid", UUID.class),
                            new LanguagePreference(language, normalizeSource(result.getString("source"))));
                }
            }
        }
        synchronized (this) {
            preferences.clear();
            preferences.putAll(loaded);
        }
    }

    private void createSchema(Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("create table if not exists " + languagesTable() + " (player_uuid uuid primary key, player_name text not null, language_code text not null check (language_code in ('en', 'ua', 'ru')), source text not null check (source in ('client', 'manual')), updated_at timestamptz not null default now())");
            statement.executeUpdate("create table if not exists " + migrationsTable() + " (migration_key text primary key, completed_at timestamptz not null default now())");
            statement.executeUpdate("create index if not exists " + prefix + "player_languages_source_idx on " + languagesTable() + " (source)");
        }
    }

    private void importYamlOnce() throws SQLException {
        File file = playersFile();
        if (!file.exists()) {
            return;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
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
            ConfigurationSection section = yaml.getConfigurationSection("players");
            if (section != null) {
                for (String uuidText : section.getKeys(false)) {
                    UUID uuid = parseUuid(uuidText);
                    if (uuid == null) {
                        continue;
                    }
                    String language = LanguageCode.normalize(section.getString(uuidText + ".language", ""));
                    if (!LanguageCode.isSupported(language)) {
                        continue;
                    }
                    String name = plugin.getServer().getOfflinePlayer(uuid).getName();
                    upsertLanguage(connection, uuid, name == null ? uuidText : name, language,
                            normalizeSource(section.getString(uuidText + ".source", "manual")));
                }
            }
            connection.commit();
        }
    }

    private void upsertLanguage(Connection connection, UUID uuid, String name, String language, String source) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(
                "insert into " + languagesTable() + " (player_uuid, player_name, language_code, source, updated_at) values (?, ?, ?, ?, now()) on conflict (player_uuid) do update set player_name = excluded.player_name, language_code = excluded.language_code, source = excluded.source, updated_at = now()")) {
            statement.setObject(1, uuid);
            statement.setString(2, name);
            statement.setString(3, language);
            statement.setString(4, source);
            statement.executeUpdate();
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

    private File playersFile() {
        String configured = plugin.getConfig().getString("settings.players-file", "");
        if (configured != null && !configured.isBlank()) {
            File file = new File(configured);
            return file.isAbsolute() ? file : new File(plugin.getDataFolder(), configured);
        }
        return new File(plugin.getDataFolder(), "players.yml");
    }

    private String languagesTable() {
        return prefix + "player_languages";
    }

    private String migrationsTable() {
        return prefix + "language_migrations";
    }

    private static String property(Properties properties, String key, String fallback) {
        String value = properties.getProperty(key);
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String tablePrefix(String configured) {
        String value = configured == null || configured.isBlank() ? "ma_" : configured.toLowerCase(Locale.ROOT);
        return value.matches("[a-z0-9_]+") ? value : "ma_";
    }

    private static String normalizeSource(String source) {
        return "client".equalsIgnoreCase(source) ? "client" : "manual";
    }

    private static UUID parseUuid(String value) {
        try {
            return value == null || value.isBlank() ? null : UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
