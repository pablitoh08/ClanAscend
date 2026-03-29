package org.pablito.clanAscend.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.managers.ClanManager;
import org.pablito.clanAscend.managers.LanguageManager;
import org.pablito.clanAscend.objects.Clan;

public class ClanEventListener implements Listener {

    private final ClanAscend plugin;

    public ClanEventListener(ClanAscend plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (plugin.getPowerManager() == null) return;

        if (killer != null && killer != victim) {
            plugin.getPowerManager().processPlayerKill(killer, victim);
        } else {
            plugin.getPowerManager().processPlayerDeath(victim);
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (!plugin.getConfig().getBoolean("messages.welcome_clan_on_join", false)) {
            return;
        }

        Player player = event.getPlayer();

        ClanManager clanManager = plugin.getClanManager();
        LanguageManager lang = plugin.getLang();

        if (clanManager == null || lang == null) return;

        Clan clan = clanManager.getPlayerClan(player);
        if (clan == null) return;

        lang.send(player, "system.welcome_back_clan",
                lang.placeholders("clan", clan.getName()));
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player == null) return;

        if (plugin.getClanChatListener() != null) {
            plugin.getClanChatListener().disableClanChat(player);
        }

        plugin.clearPendingChatAction(player.getUniqueId());
    }
}