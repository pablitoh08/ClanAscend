package org.pablito.clanAscend.managers;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.pablito.clanAscend.ClanAscend;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class LanguageManager {

    private final ClanAscend plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();

    private final Map<String, YamlConfiguration> languages = new HashMap<>();

    private String currentLang;
    private String fallbackLang;

    public LanguageManager(ClanAscend plugin) {
        this.plugin = plugin;
        load();
    }

    public void load() {
        plugin.reloadConfig();

        currentLang = plugin.getConfig().getString("language", "es");
        fallbackLang = plugin.getConfig().getString("fallback-language", "en");

        loadLanguages();
    }

    private void loadLanguages() {
        File langFolder = new File(plugin.getDataFolder(), "lang");

        if (!langFolder.exists()) {
            langFolder.mkdirs();
        }

        saveDefaultLangs();

        File[] files = langFolder.listFiles();
        if (files == null) return;

        for (File file : files) {
            String name = file.getName();

            if (!name.startsWith("messages_") || !name.endsWith(".yml")) continue;

            String langCode = name.replace("messages_", "").replace(".yml", "");

            languages.put(langCode, YamlConfiguration.loadConfiguration(file));
        }
    }

    private void saveDefaultLangs() {
        String[] langs = {
                "messages_es.yml",
                "messages_en.yml",
                "messages_fr.yml",
                "messages_de.yml",
                "messages_it.yml",
                "messages_pl.yml",
                "messages_pt_BR.yml",
                "messages_ru.yml",
                "messages_tr.yml",
                "messages_uk.yml"
        };

        for (String lang : langs) {
            File file = new File(plugin.getDataFolder() + "/lang/", lang);
            if (!file.exists()) {
                plugin.saveResource("lang/" + lang, false);
            }
        }
    }

    public String get(String path) {
        String msg = null;

        if (languages.containsKey(currentLang)) {
            msg = languages.get(currentLang).getString(path);
        }

        if (msg == null && languages.containsKey(fallbackLang)) {
            msg = languages.get(fallbackLang).getString(path);
        }

        if (msg == null) {
            msg = "<red>Missing message: " + path;
        }

        return msg;
    }

    public String format(String message, Map<String, String> placeholders) {
        if (placeholders == null) return message;

        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("<" + entry.getKey() + ">", entry.getValue());
        }

        return message;
    }

    private String colorize(String message) {
        // HEX support: &#FFFFFF → <#FFFFFF>
        message = message.replaceAll("(?i)&#([A-F0-9]{6})", "<#$1>");

        // Legacy & → §
        message = message.replace("&", "§");

        return message;
    }

    public Component getComponent(String path, Map<String, String> placeholders) {
        String message = get(path);
        message = format(message, placeholders);
        message = colorize(message);

        return miniMessage.deserialize(message);
    }

    public void send(CommandSender sender, String path) {
        send(sender, path, null);
    }

    public void send(CommandSender sender, String path, Map<String, String> placeholders) {
        Component component = getComponent(path, placeholders);

        if (sender instanceof Player player) {
            plugin.getAdventure().player(player).sendMessage(component);
        } else {
            sender.sendMessage(miniMessage.serialize(component));
        }
    }

    public Map<String, String> placeholders(String... args) {
        Map<String, String> map = new HashMap<>();

        for (int i = 0; i < args.length; i += 2) {
            map.put(args[i], args[i + 1]);
        }

        return map;
    }

    public void reload() {
        languages.clear();
        load();
    }

    public java.util.List<String> getList(String path) {
        java.util.List<String> list = null;

        if (languages.containsKey(currentLang)) {
            list = languages.get(currentLang).getStringList(path);
        }

        if ((list == null || list.isEmpty()) && languages.containsKey(fallbackLang)) {
            list = languages.get(fallbackLang).getStringList(path);
        }

        if (list == null || list.isEmpty()) {
            return java.util.Collections.singletonList("<red>Missing list: " + path);
        }

        return list;
    }
}