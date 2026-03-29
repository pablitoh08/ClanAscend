package org.pablito.clanAscend.listeners;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import io.papermc.paper.event.player.AsyncChatEvent;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.managers.ClanManager;
import org.pablito.clanAscend.managers.LanguageManager;
import org.pablito.clanAscend.objects.Clan;

import java.util.UUID;

public class ClanSettingsChatListener implements Listener {

    private final ClanAscend plugin;
    private final ClanManager clanManager;
    private final LanguageManager lang;

    public ClanSettingsChatListener(ClanAscend plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.lang = plugin.getLang();  // Cambiado de getLanguageManager() a getLang()
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        PendingChatAction action = plugin.getPendingChatAction(uuid);
        if (action == null) return;

        event.setCancelled(true);

        String msg = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(event.message());
        String raw = msg.trim();

        if (raw.equalsIgnoreCase("cancel") || raw.equalsIgnoreCase("cancelar")) {
            plugin.clearPendingChatAction(uuid);
            Bukkit.getScheduler().runTask(plugin, () -> {
                lang.send(player, "gui.prompts.cancelled");
                plugin.getClanGUI().openSettingsGUI(player);
            });
            return;
        }

        Bukkit.getScheduler().runTask(plugin, () -> {
            Clan clan = clanManager.getPlayerClan(player);
            if (clan == null) {
                plugin.clearPendingChatAction(uuid);
                lang.send(player, "clan.not_in_clan");
                return;
            }

            if (!clan.hasPermission(uuid, "officer")) {
                plugin.clearPendingChatAction(uuid);
                lang.send(player, "clan.no_permission");
                return;
            }

            switch (action) {
                case SET_DESCRIPTION:
                    String desc = net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer.legacyAmpersand().serialize(
                            net.kyori.adventure.text.Component.text(raw)
                    );
                    clan.setDescription(desc);
                    clanManager.saveClan(clan);

                    plugin.clearPendingChatAction(uuid);
                    lang.send(player, "gui.prompts.description_updated");
                    plugin.getClanGUI().openSettingsGUI(player);
                    break;

                case SET_MAX_MEMBERS:
                    int value;
                    try {
                        value = Integer.parseInt(raw);
                    } catch (NumberFormatException e) {
                        lang.send(player, "error.invalid_number");
                        return;
                    }

                    if (value < 5 || value > 50) {
                        lang.send(player, "error.invalid_range");
                        return;
                    }

                    clan.setMaxMembers(value);
                    clanManager.saveClan(clan);

                    plugin.clearPendingChatAction(uuid);
                    lang.send(player, "gui.prompts.maxmembers_updated",
                            lang.placeholders("max", String.valueOf(value)));
                    plugin.getClanGUI().openSettingsGUI(player);
                    break;
            }
        });
    }
}