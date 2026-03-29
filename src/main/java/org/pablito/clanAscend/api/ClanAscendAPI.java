package org.pablito.clanAscend.api;

import org.bukkit.entity.Player;
import org.pablito.clanAscend.api.model.ClanRewardResult;

import java.util.UUID;

public interface ClanAscendAPI {

    boolean hasClan(UUID playerId);

    default boolean hasClan(Player player) {
        return player != null && hasClan(player.getUniqueId());
    }

    String getClanId(UUID playerId);

    String getClanName(UUID playerId);

    String getClanNameById(String clanId);

    ClanRewardResult rewardPlayerClanPoints(UUID playerId, int amount, String reason);

    ClanRewardResult rewardPlayerClanPower(UUID playerId, int amount, String reason);

    ClanRewardResult rewardPlayerClanKoth(UUID playerId, int points, int power, String kothName);

    ClanRewardResult rewardClanPoints(String clanId, int amount, String reason);

    ClanRewardResult rewardClanPower(String clanId, int amount, String reason);
}