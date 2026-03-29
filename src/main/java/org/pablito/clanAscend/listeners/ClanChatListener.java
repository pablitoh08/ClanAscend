package org.pablito.clanAscend.listeners;

import io.papermc.paper.event.player.AsyncChatEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.managers.ClanManager;
import org.pablito.clanAscend.managers.LanguageManager;
import org.pablito.clanAscend.objects.Clan;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ClanChatListener implements Listener {

    private final ClanAscend plugin;
    private final ClanManager clanManager;
    private final LanguageManager lang;
    private final Set<UUID> clanChatUsers = new HashSet<>();

    public ClanChatListener(ClanAscend plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.lang = plugin.getLang();  // Cambiado de getLanguageManager() a getLang()
    }

    @EventHandler
    public void onPlayerChat(AsyncChatEvent event) {
        Player player = event.getPlayer();

        if (!clanChatUsers.contains(player.getUniqueId())) return;

        event.setCancelled(true);

        Clan clan = clanManager.getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            clanChatUsers.remove(player.getUniqueId());
            return;
        }

        String message = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                .plainText()
                .serialize(event.message());

        // Obtener el formato del mensaje usando el sistema de LanguageManager
        String format = lang.get("chat.format");
        format = lang.format(format, lang.placeholders(
                "player", player.getName(),
                "message", message
        ));

        for (UUID memberId : clan.getMembers()) {
            Player member = plugin.getServer().getPlayer(memberId);
            if (member != null && member.isOnline()) {
                member.sendMessage(format);
            }
        }
    }

    public boolean toggleClanChat(Player player) {
        if (player == null) return false;

        if (clanChatUsers.contains(player.getUniqueId())) {
            clanChatUsers.remove(player.getUniqueId());
            return false;
        }

        if (clanManager.hasClan(player)) {
            clanChatUsers.add(player.getUniqueId());
            return true;
        }

        return false;
    }

    public boolean isClanChatEnabled(Player player) {
        if (player == null) return false;
        return clanChatUsers.contains(player.getUniqueId());
    }

    public void disableClanChat(Player player) {
        if (player == null) return;
        clanChatUsers.remove(player.getUniqueId());
    }
}