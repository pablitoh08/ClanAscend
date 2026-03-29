package org.pablito.clanAscend.api.impl;

import org.bukkit.Bukkit;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.api.ClanAscendAPI;
import org.pablito.clanAscend.api.event.ClanPointsAddEvent;
import org.pablito.clanAscend.api.event.ClanPowerAddEvent;
import org.pablito.clanAscend.api.model.ClanRewardResult;
import org.pablito.clanAscend.objects.Clan;

import java.util.UUID;

public class ClanAscendAPIImpl implements ClanAscendAPI {

    private final ClanAscend plugin;

    public ClanAscendAPIImpl(ClanAscend plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean hasClan(UUID playerId) {
        return playerId != null && plugin.getClanManager().getPlayerClan(playerId) != null;
    }

    @Override
    public String getClanId(UUID playerId) {
        if (playerId == null) return null;
        Clan clan = plugin.getClanManager().getPlayerClan(playerId);
        return clan == null ? null : clan.getId();
    }

    @Override
    public String getClanName(UUID playerId) {
        if (playerId == null) return null;
        Clan clan = plugin.getClanManager().getPlayerClan(playerId);
        return clan == null ? null : clan.getName();
    }

    @Override
    public String getClanNameById(String clanId) {
        if (clanId == null || clanId.trim().isEmpty()) return null;
        Clan clan = plugin.getClanManager().getClan(clanId);
        return clan == null ? null : clan.getName();
    }

    @Override
    public ClanRewardResult rewardPlayerClanPoints(UUID playerId, int amount, String reason) {
        String clanId = getClanId(playerId);
        if (clanId == null) return ClanRewardResult.failure("Player is not in a clan.");
        return rewardClanPoints(clanId, amount, reason);
    }

    @Override
    public ClanRewardResult rewardPlayerClanPower(UUID playerId, int amount, String reason) {
        String clanId = getClanId(playerId);
        if (clanId == null) return ClanRewardResult.failure("Player is not in a clan.");
        return rewardClanPower(clanId, amount, reason);
    }

    @Override
    public ClanRewardResult rewardPlayerClanKoth(UUID playerId, int points, int power, String kothName) {
        if (playerId == null) {
            return ClanRewardResult.failure("Invalid player id.");
        }

        String clanId = getClanId(playerId);
        String clanName = getClanName(playerId);

        if (clanId == null || clanName == null) {
            return ClanRewardResult.failure("Player is not in a clan.");
        }

        if (points <= 0 && power <= 0) {
            return ClanRewardResult.failure("Points or power must be greater than 0.");
        }

        Clan clan = plugin.getClanManager().getClan(clanId);
        if (clan == null) {
            return ClanRewardResult.failure("Clan not found.");
        }

        if (points > 0) {
            clan.setExperience(clan.getExperience() + points);
            Bukkit.getPluginManager().callEvent(
                    new ClanPointsAddEvent(clanId, clanName, points, "koth:" + safeReason(kothName))
            );
        }

        if (power > 0) {
            clan.addPower(power);
            Bukkit.getPluginManager().callEvent(
                    new ClanPowerAddEvent(clanId, clanName, power, "koth:" + safeReason(kothName))
            );
        }

        plugin.getClanManager().saveClan(clan);

        return ClanRewardResult.success(
                clanId,
                clanName,
                "KOTH reward applied to clan " + clanName + "."
        );
    }

    @Override
    public ClanRewardResult rewardClanPoints(String clanId, int amount, String reason) {
        if (clanId == null || clanId.trim().isEmpty()) {
            return ClanRewardResult.failure("Invalid clan id.");
        }

        if (amount <= 0) {
            return ClanRewardResult.failure("Amount must be greater than 0.");
        }

        Clan clan = plugin.getClanManager().getClan(clanId);
        if (clan == null) {
            return ClanRewardResult.failure("Clan not found.");
        }

        clan.setExperience(clan.getExperience() + amount);
        plugin.getClanManager().saveClan(clan);

        String clanName = clan.getName();

        Bukkit.getPluginManager().callEvent(
                new ClanPointsAddEvent(clanId, clanName, amount, reason == null ? "unknown" : reason)
        );

        return ClanRewardResult.success(
                clanId,
                clanName,
                "Added " + amount + " points to clan " + clanName + "."
        );
    }

    @Override
    public ClanRewardResult rewardClanPower(String clanId, int amount, String reason) {
        if (clanId == null || clanId.trim().isEmpty()) {
            return ClanRewardResult.failure("Invalid clan id.");
        }

        if (amount <= 0) {
            return ClanRewardResult.failure("Amount must be greater than 0.");
        }

        Clan clan = plugin.getClanManager().getClan(clanId);
        if (clan == null) {
            return ClanRewardResult.failure("Clan not found.");
        }

        clan.addPower(amount);
        plugin.getClanManager().saveClan(clan);

        String clanName = clan.getName();

        Bukkit.getPluginManager().callEvent(
                new ClanPowerAddEvent(clanId, clanName, amount, reason == null ? "unknown" : reason)
        );

        return ClanRewardResult.success(
                clanId,
                clanName,
                "Added " + amount + " power to clan " + clanName + "."
        );
    }

    private String safeReason(String input) {
        return input == null || input.trim().isEmpty() ? "unknown" : input.trim();
    }
}