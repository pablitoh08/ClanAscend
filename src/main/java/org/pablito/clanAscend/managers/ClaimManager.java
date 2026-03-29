package org.pablito.clanAscend.managers;

import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.objects.Clan;
import org.pablito.clanAscend.objects.ClanClaim;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ClaimManager {

    private final ClanAscend plugin;
    private final Map<String, ClanClaim> claimsCache = new HashMap<>();

    public ClaimManager(ClanAscend plugin) {
        this.plugin = plugin;
    }

    public boolean claimChunk(Player player, Chunk chunk, Clan clan) {
        LanguageManager lang = plugin.getLang();

        if (!plugin.getConfig().getBoolean("claims.enabled", true)) {
            lang.send(player, "claim.disabled");
            return false;
        }

        if (clan == null) return false;

        if (isChunkClaimed(chunk)) {
            lang.send(player, "claim.already_claimed");
            return false;
        }

        int claimCost = plugin.getConfig().getInt("claims.claim-cost", 10);
        if (clan.getPower() < claimCost) {
            lang.send(player, "power.low_power");
            return false;
        }

        if (getClanClaimsCount(clan.getId()) >= clan.getMaxClaims()) {
            lang.send(player, "claim.max_claims");
            return false;
        }

        ClanClaim claim = new ClanClaim(
                UUID.randomUUID().toString(),
                clan.getId(),
                chunk.getWorld().getName(),
                chunk.getX(),
                chunk.getZ()
        );

        saveClaimToDatabase(claim);

        String chunkKey = getChunkKey(chunk);
        claimsCache.put(chunkKey, claim);

        plugin.getPowerManager().removePower(clan.getId(), claimCost);

        lang.send(player, "claim.claimed");

        if (plugin.getEffectsManager() != null) {
            plugin.getEffectsManager().playClaimSound(player);
            plugin.getEffectsManager().showClaimEffect(player.getLocation(), clan);
        }

        return true;
    }

    public boolean unclaimChunk(Player player, Chunk chunk) {
        LanguageManager lang = plugin.getLang();

        if (!plugin.getConfig().getBoolean("claims.enabled", true)) {
            lang.send(player, "claim.disabled");
            return false;
        }

        String chunkKey = getChunkKey(chunk);

        ClanClaim claim = claimsCache.get(chunkKey);
        if (claim == null) {
            claim = getClaimFromDatabase(chunk);
            if (claim == null) {
                lang.send(player, "claim.not_claimed");
                return false;
            }
        }

        Clan clan = plugin.getClanManager().getClan(claim.getClanId());
        if (clan == null) {
            lang.send(player, "claim.clan_missing_for_claim");
            return false;
        }

        boolean isAdmin = player.hasPermission("clanascend.admin");
        boolean canManage = isAdmin || clan.isLeader(player.getUniqueId()) || clan.hasPermission(player.getUniqueId(), "officer");

        if (!canManage) {
            lang.send(player, "claim.not_your_claim");
            return false;
        }

        removeClaimFromDatabase(claim.getId());
        claimsCache.remove(chunkKey);

        int refund = plugin.getConfig().getInt("claims.claim-refund", 5);
        if (refund > 0) {
            plugin.getPowerManager().addPower(clan.getId(), refund);
        }

        lang.send(player, "claim.unclaimed");
        return true;
    }

    public boolean isChunkClaimed(Chunk chunk) {
        String chunkKey = getChunkKey(chunk);
        if (claimsCache.containsKey(chunkKey)) return true;

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM clan_claims WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {

            stmt.setString(1, chunk.getWorld().getName());
            stmt.setInt(2, chunk.getX());
            stmt.setInt(3, chunk.getZ());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1) > 0;

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error in isChunkClaimed", e);
        }

        return false;
    }

    public ClanClaim getChunkClaim(Chunk chunk) {
        String chunkKey = getChunkKey(chunk);
        if (claimsCache.containsKey(chunkKey)) return claimsCache.get(chunkKey);
        return getClaimFromDatabase(chunk);
    }

    public boolean canBuild(Player player, Location location) {
        if (player.hasPermission("clanascend.admin")) return true;

        Chunk chunk = location.getChunk();
        ClanClaim claim = getChunkClaim(chunk);
        if (claim == null) return true;

        Clan playerClan = plugin.getClanManager().getPlayerClan(player);
        return playerClan != null && playerClan.getId().equals(claim.getClanId());
    }

    private String getChunkKey(Chunk chunk) {
        return chunk.getWorld().getName() + ":" + chunk.getX() + ":" + chunk.getZ();
    }

    private ClanClaim getClaimFromDatabase(Chunk chunk) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM clan_claims WHERE world = ? AND chunk_x = ? AND chunk_z = ?")) {

            stmt.setString(1, chunk.getWorld().getName());
            stmt.setInt(2, chunk.getX());
            stmt.setInt(3, chunk.getZ());

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                ClanClaim claim = new ClanClaim(
                        rs.getString("id"),
                        rs.getString("clan_id"),
                        rs.getString("world"),
                        rs.getInt("chunk_x"),
                        rs.getInt("chunk_z")
                );
                Timestamp ts = rs.getTimestamp("claimed_at");
                if (ts != null) claim.setClaimedAt(ts);

                claimsCache.put(getChunkKey(chunk), claim);
                return claim;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error in getClaimFromDatabase", e);
        }

        return null;
    }

    private void saveClaimToDatabase(ClanClaim claim) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "INSERT INTO clan_claims (id, clan_id, world, chunk_x, chunk_z) VALUES (?, ?, ?, ?, ?)")) {

            stmt.setString(1, claim.getId());
            stmt.setString(2, claim.getClanId());
            stmt.setString(3, claim.getWorld());
            stmt.setInt(4, claim.getChunkX());
            stmt.setInt(5, claim.getChunkZ());
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error in saveClaimToDatabase", e);
        }
    }

    private void removeClaimFromDatabase(String claimId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM clan_claims WHERE id = ?")) {

            stmt.setString(1, claimId);
            stmt.executeUpdate();

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error in removeClaimFromDatabase", e);
        }
    }

    private int getClanClaimsCount(String clanId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT COUNT(*) FROM clan_claims WHERE clan_id = ?")) {

            stmt.setString(1, clanId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return rs.getInt(1);

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error in getClanClaimsCount", e);
        }

        return 0;
    }

    public void loadAllClaims() {
        claimsCache.clear();

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM clan_claims")) {

            while (rs.next()) {
                ClanClaim claim = new ClanClaim(
                        rs.getString("id"),
                        rs.getString("clan_id"),
                        rs.getString("world"),
                        rs.getInt("chunk_x"),
                        rs.getInt("chunk_z")
                );

                Timestamp ts = rs.getTimestamp("claimed_at");
                if (ts != null) claim.setClaimedAt(ts);

                String key = claim.getWorld() + ":" + claim.getChunkX() + ":" + claim.getChunkZ();
                claimsCache.put(key, claim);
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Database error in loadAllClaims", e);
        }
    }
}