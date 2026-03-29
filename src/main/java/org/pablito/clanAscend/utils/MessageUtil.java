package org.pablito.clanAscend.utils;

import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.pablito.clanAscend.ClanAscend;

public class MessageUtil {

    private static BukkitAudiences adventure;
    private static final MiniMessage miniMessage = MiniMessage.miniMessage();

    public static void init(ClanAscend plugin) {
        adventure = BukkitAudiences.create(plugin);
    }

    public static void shutdown() {
        if (adventure != null) {
            adventure.close();
        }
    }

    public static void send(CommandSender sender, String message) {
        Component component = parse(message);

        if (sender instanceof Player player) {
            adventure.player(player).sendMessage(component);
        } else {
            Bukkit.getConsoleSender().sendMessage(miniMessage.serialize(component));
        }
    }

    public static Component parse(String message) {
        return miniMessage.deserialize(message);
    }

    public static String legacyToMini(String message) {
        message = message.replace("&", "§");

        message = message.replaceAll("(?i)&#([A-F0-9]{6})", "<#$1>");

        return message;
    }

    public static void sendLegacy(CommandSender sender, String message) {
        send(sender, legacyToMini(message));
    }
}