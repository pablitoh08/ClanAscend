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
import org.pablito.clanAscend.objects.ChunkLocation;

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

        // Check if chunk changed
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

        LanguageManager lang = plugin.getLanguageManager();

        if (oldClanId != null && newClanId == null) {
            // Exit territory
            lang.send(p, "claim.exit");
        } else if (oldClanId == null) {
            // Enter territory
            if (newTerritory.clanName != null) {
                lang.send(p, "claim.enter",
                        lang.placeholders(
                                "clan", newTerritory.clanName,
                                "tag", newTerritory.clanTag
                        ));
            }
        } else {
            // Changed from one territory to another
            lang.send(p, "claim.exit");
            if (newTerritory.clanName != null) {
                lang.send(p, "claim.enter",
                        lang.placeholders(
                                "clan", newTerritory.clanName,
                                "tag", newTerritory.clanTag
                        ));
            }
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

    private TerritoryInfo resolveTerritory(Location loc) {
        try {
            // Get clan at location using ClanManager
            Clan clan = plugin.getClanManager().getClanAtLocation(loc);
            if (clan != null) {
                return new TerritoryInfo(clan.getId(), clan.getName(), clan.getTag());
            }
        } catch (Exception ignored) {
        }

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