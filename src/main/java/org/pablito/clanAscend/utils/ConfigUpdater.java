package org.pablito.clanAscend.utils;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class ConfigUpdater {

    private ConfigUpdater() {
    }

    public static void updateMainConfig(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "config.yml");
        updateFile(plugin, "config.yml", file);
    }

    public static void updateLanguageFiles(JavaPlugin plugin) {
        String[] languageFiles = new String[] {
                "lang/messages_de.yml",
                "lang/messages_en.yml",
                "lang/messages_es.yml",
                "lang/messages_fr.yml",
                "lang/messages_it.yml",
                "lang/messages_pl.yml",
                "lang/messages_pt_BR.yml",
                "lang/messages_ru.yml",
                "lang/messages_tr.yml",
                "lang/messages_uk.yml"
        };

        for (String path : languageFiles) {
            File file = new File(plugin.getDataFolder(), path);
            updateFile(plugin, path, file);
        }
    }

    public static boolean updateFile(JavaPlugin plugin, String resourcePath, File targetFile) {
        try {
            if (!targetFile.exists()) {
                ensureParentExists(targetFile);
                plugin.saveResource(resourcePath, false);
                plugin.getLogger().info("Creado archivo faltante: " + resourcePath);
                return true;
            }

            String defaultText = readResourceText(plugin, resourcePath);
            if (defaultText == null || defaultText.trim().isEmpty()) {
                plugin.getLogger().warning("No se pudo leer el recurso interno: " + resourcePath);
                return false;
            }

            String currentText = readFileText(targetFile);

            YamlConfiguration defaultYaml = YamlConfiguration.loadConfiguration(new InputStreamReader(
                    new java.io.ByteArrayInputStream(defaultText.getBytes(StandardCharsets.UTF_8)),
                    StandardCharsets.UTF_8
            ));

            YamlConfiguration currentYaml = YamlConfiguration.loadConfiguration(targetFile);

            Set<String> missingKeys = findMissingKeys(defaultYaml, currentYaml);
            if (missingKeys.isEmpty()) {
                return false;
            }

            List<String> missingBlocks = extractMissingTopLevelBlocks(defaultText, missingKeys);

            if (missingBlocks.isEmpty()) {
                plugin.getLogger().warning("No se pudieron extraer bloques faltantes para " + resourcePath);
                return false;
            }

            createBackup(targetFile);

            StringBuilder merged = new StringBuilder(currentText == null ? "" : currentText.trim());

            if (merged.length() > 0 && merged.charAt(merged.length() - 1) != '\n') {
                merged.append('\n');
            }

            merged.append('\n');
            merged.append("# ------------------------------\n");
            merged.append("# Added automatically by ClanAscend updater\n");
            merged.append("# Missing keys from: ").append(resourcePath).append("\n");
            merged.append("# ------------------------------\n");

            for (String block : missingBlocks) {
                if (block == null || block.trim().isEmpty()) continue;
                merged.append('\n');
                merged.append(block.trim());
                merged.append('\n');
            }

            writeFileText(targetFile, merged.toString());

            plugin.getLogger().info("Actualizado " + resourcePath + " con " + missingKeys.size() + " claves faltantes.");
            return true;

        } catch (Exception e) {
            plugin.getLogger().severe("Error actualizando " + resourcePath + ": " + e.getMessage());
            return false;
        }
    }

    private static Set<String> findMissingKeys(YamlConfiguration defaults, YamlConfiguration current) {
        Set<String> missing = new LinkedHashSet<String>();

        for (String key : defaults.getKeys(true)) {
            if (isRootMetadataKey(key)) continue;
            if (!current.contains(key)) {
                missing.add(key);
            }
        }

        return missing;
    }

    private static boolean isRootMetadataKey(String key) {
        return "config-version".equalsIgnoreCase(key) || "lang-version".equalsIgnoreCase(key);
    }

    private static List<String> extractMissingTopLevelBlocks(String defaultText, Set<String> missingKeys) {
        List<String> result = new ArrayList<String>();
        if (defaultText == null || defaultText.isEmpty() || missingKeys.isEmpty()) {
            return result;
        }

        Set<String> missingTopLevelKeys = new LinkedHashSet<String>();
        for (String key : missingKeys) {
            String top = key;
            int dot = key.indexOf('.');
            if (dot != -1) {
                top = key.substring(0, dot);
            }
            missingTopLevelKeys.add(top);
        }

        List<String> lines = splitLines(defaultText);
        String currentTopLevel = null;
        StringBuilder block = null;

        for (String line : lines) {
            if (isTopLevelKeyLine(line)) {
                if (currentTopLevel != null && block != null && missingTopLevelKeys.contains(currentTopLevel)) {
                    result.add(block.toString());
                }

                currentTopLevel = getTopLevelKey(line);
                block = new StringBuilder();
                block.append(line).append('\n');
            } else {
                if (block != null) {
                    block.append(line).append('\n');
                }
            }
        }

        if (currentTopLevel != null && block != null && missingTopLevelKeys.contains(currentTopLevel)) {
            result.add(block.toString());
        }

        return removeDuplicateBlocks(result);
    }

    private static List<String> removeDuplicateBlocks(List<String> blocks) {
        Set<String> unique = new LinkedHashSet<String>();
        List<String> out = new ArrayList<String>();

        for (String block : blocks) {
            String normalized = block == null ? "" : block.trim();
            if (normalized.isEmpty()) continue;
            if (unique.add(normalized)) {
                out.add(normalized);
            }
        }

        return out;
    }

    private static boolean isTopLevelKeyLine(String line) {
        if (line == null) return false;
        if (line.trim().isEmpty()) return false;
        if (line.startsWith(" ") || line.startsWith("\t")) return false;
        String trimmed = line.trim();
        if (trimmed.startsWith("#")) return false;
        return trimmed.endsWith(":") || trimmed.contains(": ");
    }

    private static String getTopLevelKey(String line) {
        String trimmed = line.trim();
        int idx = trimmed.indexOf(':');
        if (idx == -1) return trimmed;
        return trimmed.substring(0, idx).trim();
    }

    private static List<String> splitLines(String text) {
        List<String> lines = new ArrayList<String>();
        if (text == null || text.isEmpty()) return lines;

        BufferedReader reader = new BufferedReader(new InputStreamReader(
                new java.io.ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8)),
                StandardCharsets.UTF_8
        ));

        try {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException ignored) {
        }

        return lines;
    }

    private static String readResourceText(JavaPlugin plugin, String resourcePath) throws IOException {
        InputStream in = plugin.getResource(resourcePath);
        if (in == null) return null;

        BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder();

        String line;
        while ((line = reader.readLine()) != null) {
            out.append(line).append('\n');
        }

        reader.close();
        return out.toString();
    }

    private static String readFileText(File file) throws IOException {
        if (!file.exists()) return "";
        byte[] bytes = Files.readAllBytes(file.toPath());
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static void writeFileText(File file, String text) throws IOException {
        ensureParentExists(file);

        OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
        writer.write(text);
        writer.flush();
        writer.close();
    }

    private static void ensureParentExists(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    private static void createBackup(File file) throws IOException {
        if (!file.exists()) return;

        File backup = new File(file.getParentFile(), file.getName() + ".bak");
        Files.copy(file.toPath(), backup.toPath(), StandardCopyOption.REPLACE_EXISTING);
    }

    @SuppressWarnings("unused")
    private static Object getValueAtPath(ConfigurationSection section, String path) {
        if (section == null || path == null || path.trim().isEmpty()) return null;
        return section.get(path);
    }
}