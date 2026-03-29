package org.pablito.clanAscend.managers;

import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.objects.Clan;
import org.pablito.clanAscend.objects.ChunkLocation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class DataManager {
    private final ClanAscend plugin;
    private final File clansFolder;

    public DataManager(ClanAscend plugin) {
        this.plugin = plugin;
        this.clansFolder = new File(plugin.getDataFolder(), "clans");
        if (!clansFolder.exists() && !clansFolder.mkdirs()) {
            plugin.getLogger().warning("No se pudo crear la carpeta de clanes: " + clansFolder.getPath());
        }
    }

    public void saveClan(Clan clan) {
        File file = new File(clansFolder, clan.getName().toLowerCase() + ".yml");
        YamlConfiguration config = new YamlConfiguration();

        config.set("id", clan.getId());
        config.set("name", clan.getName());
        config.set("tag", clan.getTag());
        config.set("leader", clan.getLeader().toString());

        List<String> members = new ArrayList<>();
        for (UUID member : clan.getMembers()) members.add(member.toString());
        config.set("members", members);

        List<String> officers = new ArrayList<>();
        for (UUID officer : clan.getOfficers()) officers.add(officer.toString());
        config.set("officers", officers);

        List<String> claims = new ArrayList<>();
        for (ChunkLocation chunk : clan.getClaimedChunks()) claims.add(chunk.toString());
        config.set("claims", claims);

        config.set("power", clan.getPower());
        config.set("max-power", clan.getMaxPower());
        config.set("level", clan.getLevel());
        config.set("experience", clan.getExperience());
        config.set("creation-date", clan.getCreationDate().getTime());

        config.set("description", clan.getDescription());

        config.set("settings", clan.getSettings());

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Error al guardar clan " + clan.getName(), e);
        }
    }

    public Clan loadClan(String name) {
        File file = new File(clansFolder, name.toLowerCase() + ".yml");
        if (!file.exists()) return null;

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);

        String id = config.getString("id", UUID.randomUUID().toString());
        String clanName = config.getString("name", name);
        String tag = config.getString("tag", "CL");
        String leaderStr = config.getString("leader");
        if (leaderStr == null) {
            return null;
        }
        UUID leader = UUID.fromString(leaderStr);

        Clan clan = new Clan(id, clanName, tag, leader.toString(), null, config.getInt("settings.maxMembers", 20));

        for (String memberStr : config.getStringList("members")) {
            try {
                UUID memberId = UUID.fromString(memberStr);
                if (!memberId.equals(leader)) clan.addMember(memberId);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("UUID inválido en miembros del clan: " + memberStr);
            }
        }

        for (String officerStr : config.getStringList("officers")) {
            try {
                UUID officerId = UUID.fromString(officerStr);
                clan.getOfficers().add(officerId);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("UUID inválido en officers del clan: " + officerStr);
            }
        }

        for (String claimStr : config.getStringList("claims")) {
            ChunkLocation chunk = ChunkLocation.fromString(claimStr);
            if (chunk != null) clan.addClaim(chunk);
        }

        clan.setPower(config.getInt("power", 100));
        clan.setMaxPower(config.getInt("max-power", 1000));
        clan.setLevel(config.getInt("level", 1));
        clan.setExperience(config.getInt("experience", 0));

        long creationTime = config.getLong("creation-date", System.currentTimeMillis());
        clan.setCreationDate(new Date(creationTime));

        clan.setDescription(config.getString("description", "Un clan"));

        ConfigurationSection sec = config.getConfigurationSection("settings");
        if (sec != null) {
            clan.getSettings().putAll(sec.getValues(false));
        }

        if (!config.contains("id")) {
            saveClan(clan);
        }

        return clan;
    }

    public List<Clan> loadAllClans() {
        List<Clan> clans = new ArrayList<>();
        File[] files = clansFolder.listFiles((dir, n) -> n.endsWith(".yml"));

        if (files != null) {
            for (File file : files) {
                String name = file.getName().replace(".yml", "");
                Clan clan = loadClan(name);
                if (clan != null) clans.add(clan);
            }
        }

        return clans;
    }

    public void deleteClan(String name) {
        File file = new File(clansFolder, name.toLowerCase() + ".yml");
        if (file.exists() && !file.delete()) {
            plugin.getLogger().warning("Could not delete clan file: " + file.getPath());
        }
    }
}
