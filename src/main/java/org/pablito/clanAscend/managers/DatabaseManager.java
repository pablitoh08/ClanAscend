package org.pablito.clanAscend.managers;

import org.bukkit.Bukkit;
import org.pablito.clanAscend.ClanAscend;

import java.io.File;
import java.sql.*;
import java.util.function.Consumer;
import java.util.logging.Level;

public class DatabaseManager {

    private Connection connection;
    private String databaseType; // "sqlite" o "mysql"

    public void initDatabase() {
        try {
            databaseType = ClanAscend.getInstance().getConfig().getString("database.type", "sqlite");
            databaseType = databaseType.trim().toLowerCase();

            if (databaseType.equals("mysql")) {
                initMySQL();
            } else {
                initSQLite();
            }

            testConnection();
            createTables();

        } catch (Exception e) {
            ClanAscend.getInstance().getLogger().severe("Error crítico al inicializar base de datos: " + e.getMessage());
            ClanAscend.getInstance().getLogger().log(Level.SEVERE, "Error detallado:", e);
            ClanAscend.getInstance().getLogger().warning("El plugin continuará sin base de datos. Algunas funciones estarán limitadas.");
        }
    }

    private void initMySQL() throws SQLException {
        String host = ClanAscend.getInstance().getConfig().getString("database.host", "localhost");
        String port = String.valueOf(ClanAscend.getInstance().getConfig().getInt("database.port", 3306));
        String database = ClanAscend.getInstance().getConfig().getString("database.database", "clanascend");
        String username = ClanAscend.getInstance().getConfig().getString("database.username", "root");
        String password = ClanAscend.getInstance().getConfig().getString("database.password", "");

        String url = "jdbc:mysql://" + host + ":" + port + "/" + database +
                "?useSSL=false" +
                "&allowPublicKeyRetrieval=true" +
                "&characterEncoding=utf8" +
                "&useUnicode=true" +
                "&serverTimezone=UTC";

        connection = DriverManager.getConnection(url, username, password);
    }

    private void initSQLite() throws SQLException {
        File dataFolder = ClanAscend.getInstance().getDataFolder();
        if (!dataFolder.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dataFolder.mkdirs();
        }

        String dbPath = new File(dataFolder, "clans.db").getAbsolutePath();
        String url = "jdbc:sqlite:" + dbPath;
        connection = DriverManager.getConnection(url);

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("PRAGMA foreign_keys = ON");
            stmt.execute("PRAGMA journal_mode = WAL");
            stmt.execute("PRAGMA synchronous = NORMAL");
            stmt.execute("PRAGMA cache_size = -2000");
        }
    }

    private void testConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Conexión no establecida");
        }

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT 1")) {
            if (!rs.next()) {
                throw new SQLException("No se pudo verificar la conexión");
            }
        }
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {

            String createClansTable = "CREATE TABLE IF NOT EXISTS clans (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "name VARCHAR(32) UNIQUE NOT NULL," +
                    "tag VARCHAR(8) UNIQUE NOT NULL," +
                    "leader_uuid VARCHAR(36) NOT NULL," +
                    "leader_name VARCHAR(32) NOT NULL," +
                    "description TEXT DEFAULT ''," +
                    "power INTEGER DEFAULT 100," +
                    "max_power INTEGER DEFAULT 100," +
                    "level INTEGER DEFAULT 1," +
                    "experience INTEGER DEFAULT 0," +
                    "max_members INTEGER DEFAULT 10," +
                    "color VARCHAR(16) DEFAULT '&f'," +
                    "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ")";
            stmt.execute(createClansTable);

            String createMembersTable = "CREATE TABLE IF NOT EXISTS clan_members (" +
                    (databaseType.equals("mysql")
                            ? "id INT PRIMARY KEY AUTO_INCREMENT,"
                            : "id INTEGER PRIMARY KEY AUTOINCREMENT,") +
                    "clan_id VARCHAR(36) NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "player_name VARCHAR(32) NOT NULL," +
                    "role VARCHAR(20) DEFAULT 'MEMBER'," +
                    "kills INTEGER DEFAULT 0," +
                    "deaths INTEGER DEFAULT 0," +
                    "joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "last_active TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "UNIQUE(player_uuid)," +
                    "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                    ")";
            stmt.execute(createMembersTable);

            String createClaimsTable = "CREATE TABLE IF NOT EXISTS clan_claims (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "clan_id VARCHAR(36) NOT NULL," +
                    "world VARCHAR(64) NOT NULL," +
                    "chunk_x INTEGER NOT NULL," +
                    "chunk_z INTEGER NOT NULL," +
                    "claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE," +
                    "UNIQUE(world, chunk_x, chunk_z)" +
                    ")";
            stmt.execute(createClaimsTable);

            String createInvitationsTable = "CREATE TABLE IF NOT EXISTS clan_invitations (" +
                    "id VARCHAR(36) PRIMARY KEY," +
                    "clan_id VARCHAR(36) NOT NULL," +
                    "player_uuid VARCHAR(36) NOT NULL," +
                    "invited_by VARCHAR(36) NOT NULL," +
                    "invited_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                    "expires_at TIMESTAMP" +
                    (databaseType.equals("mysql")
                            ? " DEFAULT (CURRENT_TIMESTAMP + INTERVAL 1 DAY)"
                            : " DEFAULT (datetime('now', '+1 day'))") +
                    "," +
                    "FOREIGN KEY (clan_id) REFERENCES clans(id) ON DELETE CASCADE" +
                    ")";
            stmt.execute(createInvitationsTable);

            createIndex(stmt, "idx_members_clan_id", "clan_members", "clan_id");
            createIndex(stmt, "idx_members_player_uuid", "clan_members", "player_uuid");
            createIndex(stmt, "idx_claims_clan_id", "clan_claims", "clan_id");
            createIndex(stmt, "idx_claims_location", "clan_claims", "world, chunk_x, chunk_z");
            createIndex(stmt, "idx_clans_leader", "clans", "leader_uuid");
            createIndex(stmt, "idx_invitations_player", "clan_invitations", "player_uuid");

        } catch (SQLException e) {
            ClanAscend.getInstance().getLogger().log(Level.SEVERE, "Error al crear tablas: " + e.getMessage(), e);
            throw new RuntimeException("No se pudieron crear las tablas de la base de datos", e);
        }
    }

    private void createIndex(Statement stmt, String indexName, String tableName, String columns) throws SQLException {
        String sql = "CREATE INDEX IF NOT EXISTS " + indexName + " ON " + tableName + "(" + columns + ")";
        stmt.execute(sql);
    }

    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            initDatabase();
        }
        return connection;
    }

    public void executeAsync(String query, Object... params) {
        Bukkit.getScheduler().runTaskAsynchronously(ClanAscend.getInstance(), () -> {
            try (Connection conn = getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
                stmt.executeUpdate();

            } catch (SQLException ignored) {
            }
        });
    }

    @SuppressWarnings("unused")
    public int executeUpdate(String query, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);
            return stmt.executeUpdate();
        }
    }

    @SuppressWarnings("unused")
    public void query(String query, Consumer<ResultSet> consumer, Object... params) throws SQLException {
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            for (int i = 0; i < params.length; i++) stmt.setObject(i + 1, params[i]);

            try (ResultSet rs = stmt.executeQuery()) {
                consumer.accept(rs);
            }
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {
        }
    }

    @SuppressWarnings("unused")
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    @SuppressWarnings("unused")
    public void reconnect() {
        try {
            close();
            initDatabase();
        } catch (Exception e) {
            ClanAscend.getInstance().getLogger().severe("Error al reconectar: " + e.getMessage());
        }
    }
}