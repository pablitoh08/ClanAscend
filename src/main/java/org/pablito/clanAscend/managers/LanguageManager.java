package org.pablito.clanAscend.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.pablito.clanAscend.ClanAscend;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LanguageManager {

    private final ClanAscend plugin;
    private final MiniMessage miniMessage;
    private final LegacyComponentSerializer sectionSerializer;
    private final LegacyComponentSerializer ampersandSerializer;

    public LanguageManager(ClanAscend plugin) {
        this.plugin = plugin;
        this.miniMessage = MiniMessage.miniMessage();
        this.sectionSerializer = LegacyComponentSerializer.legacySection();
        this.ampersandSerializer = LegacyComponentSerializer.legacyAmpersand();
    }

    /**
     * Get raw string from language file
     */
    public String getRaw(String path) {
        FileConfiguration lang = plugin.getLangConfig();
        String message = lang.getString(path);

        if (message == null) {
            return "<red>Missing message: " + path;
        }

        return message;
    }

    /**
     * Get a list of strings from language file (supports multi-line strings split by \n)
     */
    public List<String> getList(String path) {
        FileConfiguration lang = plugin.getLangConfig();
        List<String> list = lang.getStringList(path);

        if (list != null && !list.isEmpty()) {
            return list;
        }

        // If not a list, try to get as string and split by newline
        String single = lang.getString(path);
        if (single != null && !single.isEmpty()) {
            String[] lines = single.split("\\n");
            List<String> result = new ArrayList<>();
            for (String line : lines) {
                result.add(line);
            }
            return result;
        }

        return new ArrayList<>();
    }

    /**
     * Creates a map of placeholders from key-value pairs
     * Usage: placeholders("player", "John", "clan", "Warriors")
     */
    public Map<String, String> placeholders(String... pairs) {
        Map<String, String> placeholders = new HashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            if (i + 1 < pairs.length) {
                placeholders.put(pairs[i], pairs[i + 1]);
            }
        }
        return placeholders;
    }

    /**
     * Replace placeholders in a string
     */
    public String replacePlaceholders(String text, Map<String, String> placeholders) {
        if (text == null || placeholders == null || placeholders.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            String value = entry.getValue() == null ? "" : entry.getValue();
            result = result.replace("{" + entry.getKey() + "}", value);
        }
        return result;
    }

    /**
     * Get formatted component with placeholders
     */
    public Component getComponent(String path) {
        return getComponent(path, null);
    }

    public Component getComponent(String path, Map<String, String> placeholders) {
        String message = getRaw(path);
        if (placeholders != null && !placeholders.isEmpty()) {
            message = replacePlaceholders(message, placeholders);
        }

        // Add prefix if the message doesn't already have it and it's not a raw message
        if (!path.equals("prefix") && !path.startsWith("gui.") && !message.startsWith("§") && !message.startsWith("<")) {
            String prefix = getRaw("prefix");
            if (prefix != null && !prefix.isEmpty()) {
                message = prefix + message;
            }
        }

        try {
            if (message.contains("§")) {
                return sectionSerializer.deserialize(message);
            }

            if (message.contains("&")) {
                return ampersandSerializer.deserialize(message);
            }

            return miniMessage.deserialize(message);
        } catch (Exception ex) {
            plugin.getLogger().warning("Could not parse lang key '" + path + "': " + ex.getMessage());
            return Component.text("Invalid message: " + path);
        }
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, null);
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        if (sender == null) return;
        sender.sendMessage(getComponent(path, placeholders));
    }
}