package org.pablito.clanAscend.objects;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.text.SimpleDateFormat;
import java.util.*;

public class Clan {
    private final String id;

    private String name;
    private String tag;
    private UUID leader;

    private final Set<UUID> members;
    private final Set<UUID> officers;
    private final Set<ChunkLocation> claimedChunks;

    private int power;
    private int maxPower;

    private int level;
    private int experience;

    private Date creationDate;
    private String description;
    private final Map<String, Object> settings;

    public Clan(String name, String tag, UUID leader) {
        this(UUID.randomUUID().toString(), name, tag, leader, null, 20);
    }

    @SuppressWarnings("unused")
    public Clan(String id, String name, String tag, String leaderUUID, String leaderName, int maxMembers) {
        this(id, name, tag, UUID.fromString(leaderUUID), leaderName, maxMembers);
    }

    private Clan(String id, String name, String tag, UUID leader, String leaderName, int maxMembers) {
        this.id = id;

        this.name = name;
        this.tag = tag;
        this.leader = leader;

        this.members = new HashSet<>();
        this.officers = new HashSet<>();
        this.claimedChunks = new HashSet<>();

        this.power = 100;
        this.maxPower = 1000;

        this.level = 1;
        this.experience = 0;

        this.creationDate = new Date();
        this.description = "Un clan recién creado";

        this.settings = new HashMap<>();
        this.settings.put("open", false);
        this.settings.put("friendlyFire", false);
        this.settings.put("maxMembers", maxMembers);
        this.settings.put("banned", new ArrayList<String>());
        this.settings.put("allies", new ArrayList<String>());
        this.settings.put("allyRequestsIncoming", new ArrayList<String>());
        this.settings.put("allyRequestsOutgoing", new ArrayList<String>());

        this.members.add(leader);
    }

    public String getId() { return id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTag() { return tag; }
    public void setTag(String tag) { this.tag = tag; }

    public UUID getLeader() { return leader; }
    public void setLeader(UUID leader) { this.leader = leader; }

    public String getLeaderUUID() { return leader.toString(); }

    public Set<UUID> getMembers() { return members; }
    public Set<UUID> getOfficers() { return officers; }
    public Set<ChunkLocation> getClaimedChunks() { return claimedChunks; }

    public int getPower() { return power; }
    public void setPower(int power) { this.power = power; }
    public void addPower(int amount) { this.power = Math.min(this.power + amount, maxPower); }
    public void removePower(int amount) { this.power = Math.max(this.power - amount, 0); }

    public int getMaxPower() { return maxPower; }
    public void setMaxPower(int maxPower) { this.maxPower = maxPower; }

    public int getLevel() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, level); }

    public int getExperience() { return experience; }
    public void setExperience(int experience) { this.experience = Math.max(0, experience); }

    public Date getCreationDate() { return creationDate; }
    public void setCreationDate(Date creationDate) {
        if (creationDate != null) this.creationDate = creationDate;
    }

    public String getFormattedCreationDate() {
        try {
            return new SimpleDateFormat("dd/MM/yyyy").format(creationDate);
        } catch (Exception e) {
            return "";
        }
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Map<String, Object> getSettings() { return settings; }

    public boolean isOpen() {
        Object val = settings.get("open");
        if (val instanceof Boolean) return (Boolean) val;
        if (val != null) return Boolean.parseBoolean(val.toString());
        return false;
    }

    public void setOpen(boolean open) {
        settings.put("open", open);
    }

    public boolean addMember(UUID playerId) {
        if (isBanned(playerId)) return false;
        if (members.size() >= getMaxMembers()) return false;
        return members.add(playerId);
    }

    public boolean removeMember(UUID playerId) {
        officers.remove(playerId);
        return members.remove(playerId);
    }

    // NUEVOS MÉTODOS PARA OFICIALES
    public boolean addOfficer(UUID playerId) {
        if (!members.contains(playerId)) return false;
        return officers.add(playerId);
    }

    public boolean removeOfficer(UUID playerId) {
        return officers.remove(playerId);
    }

    public boolean isMember(UUID playerId) { return members.contains(playerId); }
    public boolean isOfficer(UUID playerId) { return officers.contains(playerId); }
    public boolean isLeader(UUID playerId) { return leader.equals(playerId); }

    public boolean hasPermission(UUID playerId, String permission) {
        if (isLeader(playerId)) return true;
        if (isOfficer(playerId) && permission.equalsIgnoreCase("officer")) return true;
        return permission.equalsIgnoreCase("member");
    }

    public int getMemberCount() { return members.size(); }

    public int getMaxMembers() {
        Object v = settings.get("maxMembers");
        if (v == null) return 20;

        if (v instanceof Number) return ((Number) v).intValue();
        try { return Integer.parseInt(String.valueOf(v)); } catch (Exception ignored) {}
        return 20;
    }

    @SuppressWarnings("unused")
    public void setMaxMembers(int maxMembers) {
        settings.put("maxMembers", Math.max(5, Math.min(50, maxMembers)));
    }

    @SuppressWarnings("unused")
    public boolean addClaim(ChunkLocation chunk) {
        if (claimedChunks.size() >= getMaxClaims()) return false;
        return claimedChunks.add(chunk);
    }

    public boolean removeClaim(ChunkLocation chunk) { return claimedChunks.remove(chunk); }
    public boolean hasClaim(ChunkLocation chunk) { return claimedChunks.contains(chunk); }

    public int getMaxClaims() { return Math.max(0, power / 10); }
    public int getClaimCount() { return claimedChunks.size(); }

    public String getColor() {
        if (power >= maxPower * 0.8) return "§6";
        if (power >= maxPower * 0.6) return "§e";
        if (power >= maxPower * 0.4) return "§a";
        if (power >= maxPower * 0.2) return "§7";
        return "§8";
    }

    public String getLeaderName() {
        OfflinePlayer off = Bukkit.getOfflinePlayer(leader);
        String name = off.getName();
        return name != null ? name : "Offline";
    }

    @SuppressWarnings("unchecked")
    public Set<UUID> getBannedMembers() {
        Object v = settings.get("banned");
        Set<UUID> out = new HashSet<>();
        if (v instanceof Iterable) {
            for (Object o : (Iterable<Object>) v) {
                if (o == null) continue;
                try { out.add(UUID.fromString(String.valueOf(o))); } catch (Exception ignored) {}
            }
        }
        return out;
    }

    public boolean isBanned(UUID uuid) {
        if (uuid == null) return false;
        return getBannedMembers().contains(uuid);
    }

    public void ban(UUID uuid) {
        if (uuid == null) return;
        Set<UUID> banned = getBannedMembers();
        banned.add(uuid);
        List<String> store = new ArrayList<>();
        for (UUID u : banned) store.add(u.toString());
        settings.put("banned", store);
    }

    @SuppressWarnings("unused")
    public void unban(UUID uuid) {
        if (uuid == null) return;
        Set<UUID> banned = getBannedMembers();
        banned.remove(uuid);
        List<String> store = new ArrayList<>();
        for (UUID u : banned) store.add(u.toString());
        settings.put("banned", store);
    }

    public Set<String> getAllianceIds() {
        return getStringSetSetting("allies");
    }

    public Set<String> getIncomingAllianceRequests() {
        return getStringSetSetting("allyRequestsIncoming");
    }

    public Set<String> getOutgoingAllianceRequests() {
        return getStringSetSetting("allyRequestsOutgoing");
    }

    public boolean isAlliedWith(String clanId) {
        return clanId != null && getAllianceIds().contains(clanId);
    }

    public boolean hasIncomingAllianceRequestFrom(String clanId) {
        return clanId != null && getIncomingAllianceRequests().contains(clanId);
    }

    public boolean hasOutgoingAllianceRequestTo(String clanId) {
        return clanId != null && getOutgoingAllianceRequests().contains(clanId);
    }

    public void addAlliance(String clanId) {
        Set<String> allies = getAllianceIds();
        allies.add(clanId);
        saveStringSetSetting("allies", allies);
    }

    public void removeAlliance(String clanId) {
        Set<String> allies = getAllianceIds();
        allies.remove(clanId);
        saveStringSetSetting("allies", allies);
    }

    public void addIncomingAllianceRequest(String clanId) {
        Set<String> ids = getIncomingAllianceRequests();
        ids.add(clanId);
        saveStringSetSetting("allyRequestsIncoming", ids);
    }

    public void removeIncomingAllianceRequest(String clanId) {
        Set<String> ids = getIncomingAllianceRequests();
        ids.remove(clanId);
        saveStringSetSetting("allyRequestsIncoming", ids);
    }

    public void addOutgoingAllianceRequest(String clanId) {
        Set<String> ids = getOutgoingAllianceRequests();
        ids.add(clanId);
        saveStringSetSetting("allyRequestsOutgoing", ids);
    }

    public void removeOutgoingAllianceRequest(String clanId) {
        Set<String> ids = getOutgoingAllianceRequests();
        ids.remove(clanId);
        saveStringSetSetting("allyRequestsOutgoing", ids);
    }

    private Set<String> getStringSetSetting(String key) {
        Object v = settings.get(key);
        Set<String> out = new HashSet<>();
        if (v instanceof Iterable) {
            for (Object o : (Iterable<Object>) v) {
                if (o == null) continue;
                String value = String.valueOf(o).trim();
                if (!value.isEmpty()) out.add(value);
            }
        }
        return out;
    }

    private void saveStringSetSetting(String key, Set<String> values) {
        settings.put(key, new ArrayList<>(values));
    }
}