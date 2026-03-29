package org.pablito.clanAscend.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.managers.LanguageManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

public class UpdateChecker {

    private final ClanAscend plugin;
    private final String projectSlug;
    private final String loader;
    private final boolean notifyOps;
    private final LanguageManager lang;

    public UpdateChecker(ClanAscend plugin, String projectSlug, String loader, boolean notifyOps) {
        this.plugin = plugin;
        this.projectSlug = projectSlug;
        this.loader = loader;
        this.notifyOps = notifyOps;
        this.lang = plugin.getLang();  // Cambiado de getLanguageManager() a getLang()
    }

    public void checkNowAsync() {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                String current = plugin.getPluginMeta().getVersion();
                String latest = fetchLatestVersion(projectSlug, loader);

                if (latest == null || latest.isEmpty()) return;
                if (equalsVersion(current, latest)) return;

                // Mensaje en consola usando el archivo de idioma
                String consoleMsg = lang.get("update.console")
                        .replace("{latest}", latest)
                        .replace("{current}", current);
                plugin.getLogger().warning(consoleMsg);

                // Mensaje para jugadores con permisos
                if (notifyOps) {
                    Bukkit.getScheduler().runTask(plugin, () -> {
                        for (Player p : Bukkit.getOnlinePlayers()) {
                            if (p.isOp() || p.hasPermission("clanascend.admin")) {
                                lang.send(p, "update.player",
                                        lang.placeholders(
                                                "latest", latest,
                                                "current", current
                                        ));
                            }
                        }
                    });
                }

            } catch (Exception ignored) {
                // Error silencioso
            }
        });
    }

    private boolean equalsVersion(String a, String b) {
        if (a == null || b == null) return false;
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private String fetchLatestVersion(String slug, String loader) {
        BufferedReader reader = null;
        try {
            String urlStr = "https://api.modrinth.com/v2/project/" + slug + "/version";
            URL url = new URL(urlStr);

            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("User-Agent", "ClanAscend-UpdateChecker");
            con.setConnectTimeout(5000);
            con.setReadTimeout(5000);

            reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            String json = sb.toString();

            String best = findFirstVersionMatchingLoader(json, loader);
            if (best != null) return best;

            return findFirstVersionNumber(json);

        } catch (Exception e) {
            return null;
        } finally {
            try { if (reader != null) reader.close(); } catch (Exception ignored) {}
        }
    }

    private String findFirstVersionMatchingLoader(String json, String loader) {
        if (json == null) return null;
        if (loader == null || loader.trim().isEmpty()) return null;

        String l = loader.toLowerCase(Locale.ROOT);

        int idx = 0;
        while (true) {
            int vpos = json.indexOf("\"version_number\"", idx);
            if (vpos == -1) return null;

            int objStart = json.lastIndexOf("{", vpos);
            int objEnd = json.indexOf("}", vpos);
            if (objStart == -1 || objEnd == -1 || objEnd <= objStart) return null;

            String obj = json.substring(objStart, Math.min(objEnd + 1, json.length())).toLowerCase(Locale.ROOT);
            if (obj.contains("\"loaders\"") && obj.contains("\"" + l + "\"")) {
                return extractVersionNumberAt(json, vpos);
            }
            idx = vpos + 1;
        }
    }

    private String findFirstVersionNumber(String json) {
        if (json == null) return null;
        int vpos = json.indexOf("\"version_number\"");
        if (vpos == -1) return null;
        return extractVersionNumberAt(json, vpos);
    }

    private String extractVersionNumberAt(String json, int vpos) {
        int colon = json.indexOf(":", vpos);
        if (colon == -1) return null;
        int q1 = json.indexOf("\"", colon);
        if (q1 == -1) return null;
        int q2 = json.indexOf("\"", q1 + 1);
        if (q2 == -1) return null;
        return json.substring(q1 + 1, q2).trim();
    }
}