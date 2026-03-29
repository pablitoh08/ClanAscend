package org.pablito.clanAscend.listeners;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.InventoryHolder;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.objects.Clan;
import org.pablito.clanAscend.objects.ClanClaim;

public class ClaimProtectionListener implements Listener {

    @EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent e) {
        if (!isProtectionEnabled("blocks")) return;

        Player player = e.getPlayer();
        if (canBypass(player)) return;

        if (!canBuild(player, e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (!isProtectionEnabled("blocks")) return;

        Player player = e.getPlayer();
        if (canBypass(player)) return;

        if (!canBuild(player, e.getBlock().getLocation())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent e) {
        if (e.getClickedBlock() == null) return;

        Block block = e.getClickedBlock();
        Player player = e.getPlayer();

        if (canBypass(player)) return;

        if (block.getState() instanceof InventoryHolder) {
            if (!isProtectionEnabled("chests")) return;
            if (!canBuild(player, block.getLocation())) {
                e.setCancelled(true);
            }
            return;
        }

        if (!isProtectionEnabled("doors") && !isProtectionEnabled("redstone")) return;

        if (!canBuild(player, block.getLocation())) {
            e.setCancelled(true);
        }
    }

    private boolean canBuild(Player player, Location loc) {
        Clan playerClan = ClanAscend.getInstance().getClanManager().getPlayerClan(player);
        boolean allowBuildInAllyClaims = ClanAscend.getInstance().getConfig().getBoolean("alliances.allow-build-in-ally-claims", false);

        try {
            ClanClaim claim = ClanAscend.getInstance().getClaimManager().getChunkClaim(loc.getChunk());
            if (claim != null) {
                Clan clan = ClanAscend.getInstance().getClanManager().getClan(claim.getClanId());
                if (clan == null) return false;
                if (clan.isMember(player.getUniqueId())) return true;
                return allowBuildInAllyClaims && playerClan != null && ClanAscend.getInstance().getClanManager().isAllied(playerClan, clan);
            }
        } catch (Exception ignored) {}

        try {
            Clan clan = ClanAscend.getInstance().getClanManager().getClanAtLocation(loc);
            if (clan != null) {
                if (clan.isMember(player.getUniqueId())) return true;
                return allowBuildInAllyClaims && playerClan != null && ClanAscend.getInstance().getClanManager().isAllied(playerClan, clan);
            }
        } catch (Exception ignored) {}

        return true;
    }

    private boolean canBypass(Player player) {
        return player.hasPermission("clanascend.admin");
    }

    private boolean isProtectionEnabled(String type) {
        return ClanAscend.getInstance().getConfig().getBoolean("claims.protection." + type, true);
    }
}