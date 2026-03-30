package org.pablito.clanAscend.managers;

import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.objects.Clan;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public class PowerManager {

    private final ClanAscend plugin;
    private final Map<String, Integer> powerCache = new ConcurrentHashMap<>();

    public PowerManager(ClanAscend plugin) {
        this.plugin = plugin;
    }

    public void addPower(String clanId, int amount) {
        int currentPower = getPower(clanId);
        int newPower = Math.min(currentPower + amount, getMaxPower(clanId));

        powerCache.put(clanId, newPower);

        plugin.getDatabaseManager().executeAsync(
                "UPDATE clans SET power = ? WHERE id = ?",
                String.valueOf(newPower), clanId
        );

        notifyClanMembers(clanId, "power.increased", amount, newPower);
    }

    public void removePower(String clanId, int amount) {
        int currentPower = getPower(clanId);
        int newPower = Math.max(0, currentPower - amount);

        powerCache.put(clanId, newPower);

        plugin.getDatabaseManager().executeAsync(
                "UPDATE clans SET power = ? WHERE id = ?",
                String.valueOf(newPower), clanId
        );

        notifyClanMembers(clanId, "power.decreased", amount, newPower);
    }

    public int getPower(String clanId) {
        if (powerCache.containsKey(clanId)) return powerCache.get(clanId);

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT power FROM clans WHERE id = ?")) {

            stmt.setString(1, clanId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int power = rs.getInt("power");
                powerCache.put(clanId, power);
                return power;
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting power for clan: " + clanId, e);
        }

        return plugin.getConfig().getInt("clan.default-power", 100);
    }

    public int getMaxPower(String clanId) {
        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT max_power FROM clans WHERE id = ?")) {

            stmt.setString(1, clanId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) return rs.getInt("max_power");

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error getting max power for clan: " + clanId, e);
        }

        return plugin.getConfig().getInt("clan.max-power", 1000);
    }

    public void processPlayerKill(Player killer, Player victim) {
        Clan killerClan = plugin.getClanManager().getPlayerClan(killer);
        Clan victimClan = plugin.getClanManager().getPlayerClan(victim);

        if (killerClan != null && victimClan != null && !killerClan.getId().equals(victimClan.getId())) {
            int powerGain = plugin.getConfig().getInt("power.per-kill",
                    plugin.getConfig().getInt("power.gain-per-kill", 5));

            if (powerGain > 0) addPower(killerClan.getId(), powerGain);
            updatePlayerStats(killer.getUniqueId().toString(), true);

            if (plugin.getEffectsManager() != null) {
                plugin.getEffectsManager().showPowerGainEffect(killer, powerGain);
            }
        }

        processPlayerDeath(victim);
    }

    public void processPlayerDeath(Player victim) {
        Clan victimClan = plugin.getClanManager().getPlayerClan(victim);
        if (victimClan == null) return;

        int perDeath = plugin.getConfig().getInt("power.per-death", 2);
        int loss = Math.abs(perDeath);

        if (loss > 0) {
            removePower(victimClan.getId(), loss);

            if (plugin.getEffectsManager() != null) {
                plugin.getEffectsManager().showPowerLossEffect(victim, loss);
            }
        }

        updatePlayerStats(victim.getUniqueId().toString(), false);
    }

    @SuppressWarnings("unused")
    public void processDailyDecay() {
        int dailyDecay = plugin.getConfig().getInt("power.daily-decay", 0);
        if (dailyDecay <= 0) return;

        try (Connection conn = plugin.getDatabaseManager().getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT id, power FROM clans WHERE power > 0")) {

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                String clanId = rs.getString("id");
                int currentPower = rs.getInt("power");
                int newPower = Math.max(0, currentPower - dailyDecay);

                if (newPower != currentPower) {
                    powerCache.put(clanId, newPower);
                    plugin.getDatabaseManager().executeAsync(
                            "UPDATE clans SET power = ? WHERE id = ?",
                            String.valueOf(newPower), clanId
                    );
                }
            }

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Error processing daily decay", e);
        }
    }

    private void updatePlayerStats(String playerUUID, boolean isKill) {
        String column = isKill ? "kills" : "deaths";

        plugin.getDatabaseManager().executeAsync(
                "UPDATE clan_members SET " + column + " = " + column + " + 1 WHERE player_uuid = ?",
                playerUUID
        );
    }

    private void notifyClanMembers(String clanId, String messageKey, int amount, int total) {
        Clan clan = plugin.getClanManager().getClan(clanId);
        if (clan == null) return;

        LanguageManager lang = plugin.getLanguageManager();

        for (Player p : Bukkit.getOnlinePlayers()) {
            Clan pc = plugin.getClanManager().getPlayerClan(p);
            if (pc != null && pc.getId().equals(clanId)) {
                lang.send(p, messageKey, lang.placeholders(
                        "amount", String.valueOf(amount),
                        "total", String.valueOf(total)
                ));
            }
        }
    }

    @SuppressWarnings("unused")
    public void updateMaxPower(String clanId, int level) {
        int baseMaxPower = plugin.getConfig().getInt("clan.max-power", 1000);
        int powerPerLevel = 50;

        int newMaxPower = baseMaxPower + (level * powerPerLevel);

        plugin.getDatabaseManager().executeAsync(
                "UPDATE clans SET max_power = ? WHERE id = ?",
                String.valueOf(newMaxPower), clanId
        );
    }
}