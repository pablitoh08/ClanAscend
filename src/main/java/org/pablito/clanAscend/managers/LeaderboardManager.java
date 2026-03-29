package org.pablito.clanAscend.managers;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.objects.Clan;
import org.pablito.clanAscend.objects.ClanMember;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class LeaderboardManager {

    private final ClanAscend plugin;
    private final Map<String, Integer> clanRankings = new LinkedHashMap<>();
    private final Map<String, Integer> playerRankings = new LinkedHashMap<>();

    private boolean syncedToLocalDb = false;

    public LeaderboardManager(ClanAscend plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unused")
    public void updateLeaderboards() {
        ensureLocalDbHasClans();
        updateClanLeaderboard();
        updatePlayerLeaderboard();
    }

    private void ensureLocalDbHasClans() {
        if (syncedToLocalDb) return;
        if (plugin.getDatabaseManager() == null) return;
        if (plugin.getClanManager() == null) return;

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            if (conn == null) return;

            int dbCount = 0;
            try (Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT COUNT(*) AS c FROM clans")) {
                if (rs.next()) {
                    dbCount = rs.getInt("c");
                }
            }

            int memoryCount = plugin.getClanManager().getAllClans().size();

            if (memoryCount == 0) {
                syncedToLocalDb = true;
                return;
            }

            if (dbCount >= memoryCount) {
                syncedToLocalDb = true;
                return;
            }

            conn.setAutoCommit(false);

            String upsertClanSql =
                    "INSERT OR REPLACE INTO clans " +
                            "(id, name, tag, leader_uuid, leader_name, description, power, max_power, level, experience, max_members, color, created_at, last_active) " +
                            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

            String upsertMemberSql =
                    "INSERT OR REPLACE INTO clan_members " +
                            "(clan_id, player_uuid, player_name, role, kills, deaths, joined_at, last_active) " +
                            "VALUES (?, ?, ?, ?, COALESCE((SELECT kills FROM clan_members WHERE player_uuid=?), 0), " +
                            "        COALESCE((SELECT deaths FROM clan_members WHERE player_uuid=?), 0), " +
                            "        CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)";

            try (PreparedStatement clanStmt = conn.prepareStatement(upsertClanSql);
                 PreparedStatement memberStmt = conn.prepareStatement(upsertMemberSql)) {

                for (Clan clan : plugin.getClanManager().getAllClans().values()) {
                    String leaderName = safeName(Bukkit.getOfflinePlayer(clan.getLeader()));

                    clanStmt.setString(1, clan.getId());
                    clanStmt.setString(2, clan.getName());
                    clanStmt.setString(3, clan.getTag());
                    clanStmt.setString(4, clan.getLeaderUUID());
                    clanStmt.setString(5, leaderName);
                    clanStmt.setString(6, clan.getDescription() == null ? "" : clan.getDescription());
                    clanStmt.setInt(7, clan.getPower());
                    clanStmt.setInt(8, clan.getMaxPower());
                    clanStmt.setInt(9, clan.getLevel());
                    clanStmt.setInt(10, clan.getExperience());
                    clanStmt.setInt(11, clan.getMaxMembers());
                    clanStmt.setString(12, "&f");
                    clanStmt.addBatch();

                    for (UUID memberId : clan.getMembers()) {
                        String role = "MEMBER";
                        if (clan.isLeader(memberId)) {
                            role = "LEADER";
                        } else if (clan.isOfficer(memberId)) {
                            role = "OFFICER";
                        }

                        OfflinePlayer off = Bukkit.getOfflinePlayer(memberId);
                        String memberName = safeName(off);

                        memberStmt.setString(1, clan.getId());
                        memberStmt.setString(2, memberId.toString());
                        memberStmt.setString(3, memberName);
                        memberStmt.setString(4, role);
                        memberStmt.setString(5, memberId.toString());
                        memberStmt.setString(6, memberId.toString());
                        memberStmt.addBatch();
                    }
                }

                clanStmt.executeBatch();
                memberStmt.executeBatch();
            }

            conn.commit();
            conn.setAutoCommit(true);

            plugin.getLogger().info("Sincronizados " + memoryCount + " clanes a SQLite local (clans.db).");
            syncedToLocalDb = true;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error synchronizing clans to database", e);
        }
    }

    private String safeName(OfflinePlayer off) {
        if (off == null) return "Unknown";
        String n = off.getName();
        return (n == null || n.trim().isEmpty()) ? "Unknown" : n;
    }

    private void updateClanLeaderboard() {
        clanRankings.clear();

        if (plugin.getDatabaseManager() == null) return;

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT c.id, c.power, c.level, " +
                             "(SELECT COUNT(*) FROM clan_members WHERE clan_id = c.id) AS member_count " +
                             "FROM clans c " +
                             "ORDER BY c.power DESC, c.level DESC, member_count DESC " +
                             "LIMIT 10")) {

            int rank = 1;
            while (rs.next()) {
                String clanId = rs.getString("id");
                clanRankings.put(clanId, rank);
                rank++;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating clan leaderboard", e);
        }
    }

    private void updatePlayerLeaderboard() {
        playerRankings.clear();

        if (plugin.getDatabaseManager() == null) return;

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT player_uuid, kills, deaths, " +
                             "(kills * 100.0 / NULLIF(kills + deaths, 0)) AS kd_ratio " +
                             "FROM clan_members " +
                             "WHERE kills + deaths > 0 " +
                             "ORDER BY kills DESC, kd_ratio DESC " +
                             "LIMIT 10")) {

            int rank = 1;
            while (rs.next()) {
                String playerUUID = rs.getString("player_uuid");
                playerRankings.put(playerUUID, rank);
                rank++;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error updating player leaderboard", e);
        }
    }

    public List<Clan> getTopClans(int limit) {
        ensureLocalDbHasClans();

        List<Clan> topClans = new ArrayList<>();

        if (plugin.getDatabaseManager() == null) return topClans;

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM clans ORDER BY power DESC, level DESC LIMIT ?")) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Clan clan = new Clan(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("tag"),
                        rs.getString("leader_uuid"),
                        rs.getString("leader_name"),
                        rs.getInt("max_members")
                );

                clan.setPower(rs.getInt("power"));
                clan.setMaxPower(rs.getInt("max_power"));
                clan.setLevel(rs.getInt("level"));
                clan.setExperience(rs.getInt("experience"));

                topClans.add(clan);
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting top clans", e);
        }

        return topClans;
    }

    @SuppressWarnings("unused")
    public List<ClanMember> getTopPlayers(int limit) {
        ensureLocalDbHasClans();

        List<ClanMember> topPlayers = new ArrayList<>();

        if (plugin.getDatabaseManager() == null) return topPlayers;

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT cm.*, c.name AS clan_name " +
                             "FROM clan_members cm " +
                             "LEFT JOIN clans c ON cm.clan_id = c.id " +
                             "ORDER BY cm.kills DESC, (cm.kills * 100.0 / NULLIF(cm.kills + cm.deaths, 0)) DESC " +
                             "LIMIT ?")) {

            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                ClanMember member = new ClanMember(
                        rs.getString("player_uuid"),
                        rs.getString("player_name"),
                        rs.getString("clan_id"),
                        rs.getString("role")
                );
                member.setKills(rs.getInt("kills"));
                member.setDeaths(rs.getInt("deaths"));
                topPlayers.add(member);
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting top players", e);
        }

        return topPlayers;
    }

    @SuppressWarnings("unused")
    public int getClanRank(String clanId) {
        if (clanRankings.isEmpty()) {
            updateClanLeaderboard();
        }

        Integer rank = clanRankings.get(clanId);
        return rank != null ? rank : -1;
    }

    public void displayTopClans(CommandSender sender) {
        LanguageManager lang = plugin.getLang();  // Cambiado de getLanguageManager() a getLang()

        List<Clan> topClans = getTopClans(10);

        lang.send(sender, "top.header");
        if (topClans.isEmpty()) {
            lang.send(sender, "top.empty");
            return;
        }

        for (int i = 0; i < topClans.size(); i++) {
            Clan clan = topClans.get(i);
            int rank = i + 1;

            String color = (rank == 1) ? "§6" : (rank == 2) ? "§7" : (rank == 3) ? "§c" : "§f";

            lang.send(sender, "top.line",
                    lang.placeholders(
                            "color", color,
                            "rank", String.valueOf(rank),
                            "name", clan.getName(),
                            "tag", clan.getTag(),
                            "power", String.valueOf(clan.getPower()),
                            "members", String.valueOf(clan.getMemberCount())
                    ));
        }

        lang.send(sender, "top.footer");
    }
}