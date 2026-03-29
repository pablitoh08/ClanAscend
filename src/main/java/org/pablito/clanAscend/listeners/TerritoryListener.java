package org.pablito.clanAscend.listeners;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.managers.LanguageManager;
import org.pablito.clanAscend.objects.Clan;
import org.pablito.clanAscend.objects.ClanClaim;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TerritoryListener implements Listener {

    private final ClanAscend plugin;
    private final Map<UUID, String> lastTerritory = new HashMap<>();

    public TerritoryListener(ClanAscend plugin) {
        this.plugin = plugin;
    }

    @EventHandler(ignoreCancelled = true)
    public void onMove(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (to == null) return;

        if (from.getWorld() != null && to.getWorld() != null) {
            if (from.getWorld().equals(to.getWorld())
                    && from.getChunk().getX() == to.getChunk().getX()
                    && from.getChunk().getZ() == to.getChunk().getZ()) {
                return;
            }
        }

        Player p = e.getPlayer();

        TerritoryInfo newTerritory = resolveTerritory(to);
        String newClanId = newTerritory.clanId;

        String oldClanId = lastTerritory.get(p.getUniqueId());
        if (oldClanId == null && !lastTerritory.containsKey(p.getUniqueId())) {
            TerritoryInfo oldTerritory = resolveTerritory(from);
            oldClanId = oldTerritory.clanId;
            lastTerritory.put(p.getUniqueId(), oldClanId);
        }

        if (equalsNullSafe(oldClanId, newClanId)) return;

        if (oldClanId != null && newClanId == null) {
            sendExit(p);
        } else if (oldClanId == null) {
            sendEnter(p, newTerritory);
        } else {
            sendExit(p);
            sendEnter(p, newTerritory);
        }

        lastTerritory.put(p.getUniqueId(), newClanId);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        lastTerritory.remove(e.getPlayer().getUniqueId());
    }

    @EventHandler
    public void onKick(PlayerKickEvent e) {
        lastTerritory.remove(e.getPlayer().getUniqueId());
    }

    private void sendEnter(Player player, TerritoryInfo info) {
        LanguageManager lang = plugin.getLang();

        String clanName = (info.clanName != null ? info.clanName : lang.get("system.unknown"));
        String clanTag = (info.clanTag != null ? info.clanTag : "");

        lang.send(player, "claim.enter",
                lang.placeholders(
                        "clan", clanName,
                        "tag", clanTag
                ));
    }

    private void sendExit(Player player) {
        LanguageManager lang = plugin.getLang();
        lang.send(player, "claim.exit");
    }

    private TerritoryInfo resolveTerritory(Location loc) {
        try {
            ClanClaim claim = plugin.getClaimManager().getChunkClaim(loc.getChunk());
            if (claim != null) {
                Clan clan = plugin.getClanManager().getClan(claim.getClanId());
                if (clan != null) {
                    return new TerritoryInfo(clan.getId(), clan.getName(), clan.getTag());
                }
                LanguageManager lang = plugin.getLang();
                return new TerritoryInfo(claim.getClanId(), lang.get("system.unknown_clan"), "");
            }
        } catch (Exception ignored) {
        }

        try {
            Clan clan = plugin.getClanManager().getClanAtLocation(loc);
            if (clan != null) return new TerritoryInfo(clan.getId(), clan.getName(), clan.getTag());
        } catch (Exception ignored) {}

        return new TerritoryInfo(null, null, null);
    }

    private boolean equalsNullSafe(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null) return false;
        return a.equals(b);
    }

    private static class TerritoryInfo {
        final String clanId;
        final String clanName;
        final String clanTag;

        TerritoryInfo(String clanId, String clanName, String clanTag) {
            this.clanId = clanId;
            this.clanName = clanName;
            this.clanTag = clanTag;
        }
    }
}