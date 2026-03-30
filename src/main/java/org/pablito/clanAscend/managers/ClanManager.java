package org.pablito.clanAscend.managers;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.objects.Clan;
import org.pablito.clanAscend.objects.ChunkLocation;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ClanManager {

    private final Map<String, Clan> clansByName = new ConcurrentHashMap<>();
    private final Map<String, Clan> clansById = new ConcurrentHashMap<>();
    private final Map<UUID, String> playerClans = new ConcurrentHashMap<>();
    private final Map<ChunkLocation, String> claimedChunks = new ConcurrentHashMap<>();
    private final Map<UUID, PendingInvite> pendingInvites = new ConcurrentHashMap<>();

    private final ClanAscend plugin;
    private final LanguageManager lang;

    private static final long INVITE_TTL_MS = 5L * 60L * 1000L;

    public ClanManager(ClanAscend plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager(); // CORREGIDO: usar getLanguageManager() en lugar de getLang()
        loadClans();
    }

    public Map<String, Clan> getAllClans() {
        return new HashMap<>(clansById);
    }

    public Clan getClan(String idOrName) {
        if (idOrName == null) return null;

        Clan byId = clansById.get(idOrName);
        if (byId != null) return byId;

        return clansByName.get(idOrName.toLowerCase(Locale.ROOT));
    }

    public Clan getClanByName(String name) {
        return name == null ? null : clansByName.get(name.toLowerCase(Locale.ROOT));
    }

    public Clan getPlayerClan(Player player) {
        return player == null ? null : getPlayerClan(player.getUniqueId());
    }

    public Clan getPlayerClan(UUID uuid) {
        String clanId = playerClans.get(uuid);
        return clanId == null ? null : clansById.get(clanId);
    }

    public boolean hasClan(Player player) {
        return getPlayerClan(player) != null;
    }

    public void createClan(Player player, String name, String tag) {
        if (player == null) return;

        String normalized = name.toLowerCase(Locale.ROOT);
        if (clansByName.containsKey(normalized)) {
            lang.send(player, "clan.already_exists");
            return;
        }

        Clan clan = new Clan(name, tag, player.getUniqueId());

        int defaultPower = plugin.getConfig().getInt("clan.default-power", 100);
        int maxPower = plugin.getConfig().getInt("clan.max-power", 1000);
        int defaultMaxMembers = plugin.getConfig().getInt("clan.default-max-members", 20);

        clan.setPower(defaultPower);
        clan.setMaxPower(maxPower);
        clan.getSettings().put("maxMembers", defaultMaxMembers);

        clansById.put(clan.getId(), clan);
        clansByName.put(normalized, clan);
        playerClans.put(player.getUniqueId(), clan.getId());

        saveClan(clan);

        lang.send(player, "clan.created",
                lang.placeholders(
                        "clan", clan.getName(),
                        "tag", clan.getTag()
                ));

        if (plugin.getConfig().getBoolean("broadcast.clan-creation", true)) {
            String broadcast = plugin.getConfig().getString(
                    "broadcast.message",
                    "&6{player} &7ha creado el clan &6{clan} &7[&6{tag}&7]!"
            );

            if (broadcast != null) {
                broadcast = broadcast
                        .replace("{player}", player.getName())
                        .replace("{clan}", clan.getName())
                        .replace("{tag}", clan.getTag())
                        .replace("&", "§");
                Bukkit.broadcastMessage(broadcast);
            }
        }
    }

    public void disbandClan(Player player) {
        if (player == null) return;

        Clan clan = getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        if (!clan.isLeader(player.getUniqueId()) && !player.hasPermission("clanascend.disband.others")) {
            lang.send(player, "clan.not_leader");
            return;
        }

        disbandClan(clan, player.getName());
    }

    public void disbandClan(Clan clan, String actorName) {
        if (clan == null) return;

        removeAllAllianceRelations(clan);

        for (UUID memberId : new HashSet<>(clan.getMembers())) {
            playerClans.remove(memberId);
            Player online = Bukkit.getPlayer(memberId);
            if (online != null && online.isOnline()) {
                lang.send(online, "clan.disbanded",
                        lang.placeholders("clan", clan.getName()));
            }
        }

        for (ChunkLocation chunk : new HashSet<>(clan.getClaimedChunks())) {
            claimedChunks.remove(chunk);
        }

        clansByName.remove(clan.getName().toLowerCase(Locale.ROOT));
        clansById.remove(clan.getId());

        File clanFile = new File(plugin.getDataFolder(), "clans/" + clan.getId() + ".yml");
        if (clanFile.exists()) {
            clanFile.delete();
        }

        if (plugin.getConfig().getBoolean("broadcast.clan-disband", true)) {
            String broadcast = plugin.getConfig().getString(
                    "broadcast.disband-message",
                    "&cEl clan &6{clan} &cha sido eliminado!"
            );

            if (broadcast != null) {
                broadcast = broadcast
                        .replace("{clan}", clan.getName())
                        .replace("&", "§");
                Bukkit.broadcastMessage(broadcast);
            }
        }
    }

    public void invitePlayer(Player inviter, Player target) {
        if (inviter == null || target == null) return;

        Clan clan = getPlayerClan(inviter);
        if (clan == null) {
            lang.send(inviter, "clan.not_in_clan");
            return;
        }

        if (!clan.hasPermission(inviter.getUniqueId(), "officer")) {
            lang.send(inviter, "clan.no_permission");
            return;
        }

        if (hasClan(target)) {
            lang.send(inviter, "clan.target_already_in_clan");
            return;
        }

        pendingInvites.put(target.getUniqueId(), new PendingInvite(clan.getId(), System.currentTimeMillis()));

        lang.send(inviter, "clan.invite_sent",
                lang.placeholders("player", target.getName()));

        lang.send(target, "clan.invited",
                lang.placeholders(
                        "clan", clan.getName(),
                        "player", inviter.getName()
                ));

        lang.send(target, "clan.click_to_accept_hover",
                lang.placeholders("clan", clan.getName()));
        target.sendMessage("§7(/clan accept " + clan.getName() + ")");
    }

    public void acceptInvite(Player player, String clanName) {
        if (player == null || clanName == null) return;

        PendingInvite invite = pendingInvites.get(player.getUniqueId());
        if (invite == null || invite.isExpired() || !invite.matchesClanName(clanName, this)) {
            lang.send(player, "clan.no_pending_invite");
            return;
        }

        Clan clan = clansById.get(invite.getClanId());
        if (clan == null) {
            pendingInvites.remove(player.getUniqueId());
            lang.send(player, "clan.no_pending_invite");
            return;
        }

        if (hasClan(player)) {
            lang.send(player, "clan.already_in_clan");
            return;
        }

        if (!clan.addMember(player.getUniqueId())) {
            lang.send(player, "clan.cannot_join");
            return;
        }

        playerClans.put(player.getUniqueId(), clan.getId());
        pendingInvites.remove(player.getUniqueId());

        saveClan(clan);

        lang.send(player, "clan.joined",
                lang.placeholders("clan", clan.getName()));

        broadcastToClan(clan, lang.getRaw("clan.member_joined")
                .replace("{player}", player.getName()));
    }

    public void declineInvite(Player player, String clanName) {
        if (player == null || clanName == null) return;

        PendingInvite invite = pendingInvites.get(player.getUniqueId());
        if (invite == null || invite.isExpired() || !invite.matchesClanName(clanName, this)) {
            lang.send(player, "clan.no_pending_invite");
            return;
        }

        Clan clan = clansById.get(invite.getClanId());
        pendingInvites.remove(player.getUniqueId());

        if (clan != null) {
            lang.send(player, "clan.invite_declined",
                    lang.placeholders("clan", clan.getName()));
        } else {
            lang.send(player, "clan.no_pending_invite");
        }
    }

    public void leaveClan(Player player) {
        if (player == null) return;

        Clan clan = getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        if (clan.isLeader(player.getUniqueId())) {
            lang.send(player, "clan.leader_cannot_leave");
            return;
        }

        clan.removeMember(player.getUniqueId());
        playerClans.remove(player.getUniqueId());

        saveClan(clan);

        lang.send(player, "clan.left",
                lang.placeholders("clan", clan.getName()));

        broadcastToClan(clan, lang.getRaw("clan.member_left")
                .replace("{player}", player.getName()));
    }

    public void kickPlayer(Player actor, String targetName) {
        if (actor == null || targetName == null) return;

        Clan clan = getPlayerClan(actor);
        if (clan == null) {
            lang.send(actor, "clan.not_in_clan");
            return;
        }

        if (!clan.hasPermission(actor.getUniqueId(), "officer")) {
            lang.send(actor, "clan.no_permission");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        if (target == null || target.getUniqueId() == null || !clan.isMember(target.getUniqueId())) {
            lang.send(actor, "clan.target_not_in_clan");
            return;
        }

        if (clan.isLeader(target.getUniqueId())) {
            lang.send(actor, "clan.cannot_kick_leader");
            return;
        }

        clan.removeMember(target.getUniqueId());
        playerClans.remove(target.getUniqueId());

        saveClan(clan);

        lang.send(actor, "clan.kicked",
                lang.placeholders("player", target.getName()));

        Player online = Bukkit.getPlayer(target.getUniqueId());
        if (online != null && online.isOnline()) {
            lang.send(online, "clan.you_were_kicked",
                    lang.placeholders("clan", clan.getName()));
        }

        broadcastToClan(clan, lang.getRaw("clan.member_kicked")
                .replace("{player}", target.getName()));
    }

    public void claimChunk(Player player) {
        if (player == null) return;

        Clan clan = getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        if (!clan.hasPermission(player.getUniqueId(), "officer")) {
            lang.send(player, "clan.no_permission");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        ChunkLocation chunkLocation = new ChunkLocation(chunk);

        if (claimedChunks.containsKey(chunkLocation)) {
            lang.send(player, "claim.already_claimed");
            return;
        }

        if (clan.getClaimCount() >= clan.getMaxClaims()) {
            lang.send(player, "claim.limit_reached");
            return;
        }

        clan.getClaimedChunks().add(chunkLocation);
        claimedChunks.put(chunkLocation, clan.getId());

        saveClan(clan);

        lang.send(player, "claim.claimed",
                lang.placeholders(
                        "x", String.valueOf(chunk.getX()),
                        "z", String.valueOf(chunk.getZ())
                ));
    }

    public void unclaimChunk(Player player) {
        if (player == null) return;

        Clan clan = getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        if (!clan.hasPermission(player.getUniqueId(), "officer")) {
            lang.send(player, "clan.no_permission");
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        ChunkLocation chunkLocation = new ChunkLocation(chunk);

        String ownerClanId = claimedChunks.get(chunkLocation);
        if (ownerClanId == null || !ownerClanId.equals(clan.getId())) {
            lang.send(player, "claim.not_your_claim");
            return;
        }

        clan.getClaimedChunks().remove(chunkLocation);
        claimedChunks.remove(chunkLocation);

        saveClan(clan);

        lang.send(player, "claim.unclaimed",
                lang.placeholders(
                        "x", String.valueOf(chunk.getX()),
                        "z", String.valueOf(chunk.getZ())
                ));
    }

    public Clan getClanAtLocation(Location location) {
        if (location == null || location.getWorld() == null) return null;

        ChunkLocation chunkLocation = new ChunkLocation(location.getChunk());
        String clanId = claimedChunks.get(chunkLocation);

        return clanId == null ? null : clansById.get(clanId);
    }

    public boolean isClaimed(Location location) {
        return getClanAtLocation(location) != null;
    }

    public void saveClan(Clan clan) {
        if (clan == null) return;

        File clansFolder = new File(plugin.getDataFolder(), "clans");
        if (!clansFolder.exists()) {
            clansFolder.mkdirs();
        }

        File clanFile = new File(clansFolder, clan.getId() + ".yml");
        FileConfiguration data = YamlConfiguration.loadConfiguration(clanFile);

        data.set("id", clan.getId());
        data.set("name", clan.getName());
        data.set("tag", clan.getTag());
        data.set("leader", clan.getLeader().toString());
        data.set("power", clan.getPower());
        data.set("maxPower", clan.getMaxPower());

        List<String> members = clan.getMembers().stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        data.set("members", members);

        List<String> officers = clan.getOfficers().stream()
                .map(UUID::toString)
                .collect(Collectors.toList());
        data.set("officers", officers);

        List<String> claimedChunksList = clan.getClaimedChunks().stream()
                .map(ChunkLocation::toString)
                .collect(Collectors.toList());
        data.set("claimedChunks", claimedChunksList);

        data.set("allies", new ArrayList<>(clan.getAllianceIds()));
        data.set("allyRequestsIncoming", new ArrayList<>(clan.getIncomingAllianceRequests()));
        data.set("allyRequestsOutgoing", new ArrayList<>(clan.getOutgoingAllianceRequests()));

        try {
            data.save(clanFile);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save clan: " + clan.getName());
            e.printStackTrace();
        }
    }

    public void saveAllClans() {
        for (Clan clan : clansById.values()) {
            saveClan(clan);
        }
    }

    public void loadClans() {
        File clansFolder = new File(plugin.getDataFolder(), "clans");
        if (!clansFolder.exists()) {
            clansFolder.mkdirs();
            return;
        }

        File[] clanFiles = clansFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (clanFiles == null) return;

        for (File file : clanFiles) {
            try {
                FileConfiguration data = YamlConfiguration.loadConfiguration(file);

                String name = data.getString("name");
                String tag = data.getString("tag");
                UUID leader = UUID.fromString(data.getString("leader"));

                Clan clan = new Clan(name, tag, leader);

                clan.setPower(data.getInt("power", 100));
                clan.setMaxPower(data.getInt("maxPower", 1000));

                List<String> members = data.getStringList("members");
                for (String member : members) {
                    clan.addMember(UUID.fromString(member));
                }

                List<String> officers = data.getStringList("officers");
                for (String officer : officers) {
                    clan.addOfficer(UUID.fromString(officer));
                }

                List<String> claimedChunksList = data.getStringList("claimedChunks");
                for (String chunkStr : claimedChunksList) {
                    ChunkLocation chunk = ChunkLocation.fromString(chunkStr);
                    if (chunk != null) {
                        clan.getClaimedChunks().add(chunk);
                        claimedChunks.put(chunk, clan.getId());
                    }
                }

                List<String> allies = data.getStringList("allies");
                for (String ally : allies) {
                    clan.addAlliance(ally);
                }

                List<String> incomingRequests = data.getStringList("allyRequestsIncoming");
                for (String request : incomingRequests) {
                    clan.addIncomingAllianceRequest(request);
                }

                List<String> outgoingRequests = data.getStringList("allyRequestsOutgoing");
                for (String request : outgoingRequests) {
                    clan.addOutgoingAllianceRequest(request);
                }

                clansById.put(clan.getId(), clan);
                clansByName.put(clan.getName().toLowerCase(Locale.ROOT), clan);

                for (UUID member : clan.getMembers()) {
                    playerClans.put(member, clan.getId());
                }

            } catch (Exception e) {
                plugin.getLogger().warning("Failed to load clan from file: " + file.getName());
                e.printStackTrace();
            }
        }

        plugin.getLogger().info("Loaded " + clansById.size() + " clans");
    }

    public void sendAllianceRequest(Player player, String targetClanName) {
        if (player == null || targetClanName == null || targetClanName.trim().isEmpty()) return;

        Clan senderClan = getPlayerClan(player);
        if (senderClan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        if (!senderClan.hasPermission(player.getUniqueId(), "officer")) {
            lang.send(player, "clan.no_permission");
            return;
        }

        Clan targetClan = getClanByName(targetClanName);
        if (targetClan == null) {
            lang.send(player, "clan.not_found");
            return;
        }

        if (senderClan.getId().equals(targetClan.getId())) {
            lang.send(player, "alliance.cannot_ally_self");
            return;
        }

        if (isAllied(senderClan, targetClan)) {
            lang.send(player, "alliance.already_allied",
                    lang.placeholders("clan", targetClan.getName()));
            return;
        }

        int maxAllies = Math.max(1, plugin.getConfig().getInt("alliances.max-allies", 5));

        if (senderClan.getAllianceIds().size() >= maxAllies) {
            lang.send(player, "alliance.limit_reached",
                    lang.placeholders("limit", String.valueOf(maxAllies)));
            return;
        }

        if (targetClan.getAllianceIds().size() >= maxAllies) {
            lang.send(player, "alliance.target_limit_reached",
                    lang.placeholders(
                            "clan", targetClan.getName(),
                            "limit", String.valueOf(maxAllies)
                    ));
            return;
        }

        if (targetClan.hasIncomingAllianceRequestFrom(senderClan.getId())
                || senderClan.hasOutgoingAllianceRequestTo(targetClan.getId())) {
            lang.send(player, "alliance.request_already_sent",
                    lang.placeholders("clan", targetClan.getName()));
            return;
        }

        if (senderClan.hasIncomingAllianceRequestFrom(targetClan.getId())
                || targetClan.hasOutgoingAllianceRequestTo(senderClan.getId())) {

            makeAlliance(senderClan, targetClan);

            lang.send(player, "alliance.accepted_auto",
                    lang.placeholders("clan", targetClan.getName()));

            broadcastToClan(senderClan, lang.getRaw("alliance.established")
                    .replace("{clan}", targetClan.getName()));

            broadcastToClan(targetClan, lang.getRaw("alliance.established")
                    .replace("{clan}", senderClan.getName()));
            return;
        }

        senderClan.addOutgoingAllianceRequest(targetClan.getId());
        targetClan.addIncomingAllianceRequest(senderClan.getId());

        saveClan(senderClan);
        saveClan(targetClan);

        lang.send(player, "alliance.request_sent",
                lang.placeholders("clan", targetClan.getName()));

        notifyClanOfficers(targetClan, lang.getRaw("alliance.request_received")
                .replace("{clan}", senderClan.getName())
                .replace("{player}", player.getName()));

        sendAllianceAcceptHint(targetClan, senderClan);
    }

    public void acceptAllianceRequest(Player player, String fromClanName) {
        if (player == null || fromClanName == null || fromClanName.trim().isEmpty()) return;

        Clan ownClan = getPlayerClan(player);
        if (ownClan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        if (!ownClan.hasPermission(player.getUniqueId(), "officer")) {
            lang.send(player, "clan.no_permission");
            return;
        }

        Clan otherClan = getClanByName(fromClanName);
        if (otherClan == null) {
            lang.send(player, "clan.not_found");
            return;
        }

        if (!ownClan.hasIncomingAllianceRequestFrom(otherClan.getId())) {
            lang.send(player, "alliance.no_request_from",
                    lang.placeholders("clan", otherClan.getName()));
            return;
        }

        int maxAllies = Math.max(1, plugin.getConfig().getInt("alliances.max-allies", 5));

        if (ownClan.getAllianceIds().size() >= maxAllies) {
            lang.send(player, "alliance.limit_reached",
                    lang.placeholders("limit", String.valueOf(maxAllies)));
            return;
        }

        if (otherClan.getAllianceIds().size() >= maxAllies) {
            lang.send(player, "alliance.target_limit_reached",
                    lang.placeholders(
                            "clan", otherClan.getName(),
                            "limit", String.valueOf(maxAllies)
                    ));
            return;
        }

        makeAlliance(ownClan, otherClan);

        lang.send(player, "alliance.request_accepted",
                lang.placeholders("clan", otherClan.getName()));

        broadcastToClan(ownClan, lang.getRaw("alliance.established")
                .replace("{clan}", otherClan.getName()));

        broadcastToClan(otherClan, lang.getRaw("alliance.established")
                .replace("{clan}", ownClan.getName()));
    }

    public void denyAllianceRequest(Player player, String fromClanName) {
        if (player == null || fromClanName == null || fromClanName.trim().isEmpty()) return;

        Clan ownClan = getPlayerClan(player);
        if (ownClan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        if (!ownClan.hasPermission(player.getUniqueId(), "officer")) {
            lang.send(player, "clan.no_permission");
            return;
        }

        Clan otherClan = getClanByName(fromClanName);
        if (otherClan == null) {
            lang.send(player, "clan.not_found");
            return;
        }

        if (!ownClan.hasIncomingAllianceRequestFrom(otherClan.getId())) {
            lang.send(player, "alliance.no_request_from",
                    lang.placeholders("clan", otherClan.getName()));
            return;
        }

        ownClan.removeIncomingAllianceRequest(otherClan.getId());
        otherClan.removeOutgoingAllianceRequest(ownClan.getId());

        saveClan(ownClan);
        saveClan(otherClan);

        lang.send(player, "alliance.request_denied",
                lang.placeholders("clan", otherClan.getName()));

        notifyClanOfficers(otherClan, lang.getRaw("alliance.request_denied_notify")
                .replace("{clan}", ownClan.getName()));
    }

    public void removeAlliance(Player player, String otherClanName) {
        if (player == null || otherClanName == null || otherClanName.trim().isEmpty()) return;

        Clan ownClan = getPlayerClan(player);
        if (ownClan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        if (!ownClan.hasPermission(player.getUniqueId(), "officer")) {
            lang.send(player, "clan.no_permission");
            return;
        }

        Clan otherClan = getClanByName(otherClanName);
        if (otherClan == null) {
            lang.send(player, "clan.not_found");
            return;
        }

        if (!isAllied(ownClan, otherClan)) {
            lang.send(player, "alliance.not_allied",
                    lang.placeholders("clan", otherClan.getName()));
            return;
        }

        ownClan.removeAlliance(otherClan.getId());
        otherClan.removeAlliance(ownClan.getId());

        ownClan.removeIncomingAllianceRequest(otherClan.getId());
        ownClan.removeOutgoingAllianceRequest(otherClan.getId());
        otherClan.removeIncomingAllianceRequest(ownClan.getId());
        otherClan.removeOutgoingAllianceRequest(ownClan.getId());

        saveClan(ownClan);
        saveClan(otherClan);

        lang.send(player, "alliance.removed",
                lang.placeholders("clan", otherClan.getName()));

        broadcastToClan(otherClan, lang.getRaw("alliance.removed_notify")
                .replace("{clan}", ownClan.getName()));
    }

    public void showAllies(Player player) {
        if (player == null) return;

        Clan clan = getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        Set<String> allyIds = clan.getAllianceIds();
        if (allyIds.isEmpty()) {
            lang.send(player, "alliance.none");
            return;
        }

        List<String> names = new ArrayList<>();
        for (String allyId : allyIds) {
            Clan ally = getClan(allyId);
            if (ally != null) {
                names.add(ally.getName());
            }
        }

        Collections.sort(names, String.CASE_INSENSITIVE_ORDER);

        lang.send(player, "alliance.list",
                lang.placeholders("allies", String.join("§7, §a", names)));
    }

    public boolean isAllied(Clan first, Clan second) {
        if (first == null || second == null) return false;
        if (first.getId().equals(second.getId())) return false;
        return first.isAlliedWith(second.getId()) && second.isAlliedWith(first.getId());
    }

    private void makeAlliance(Clan first, Clan second) {
        first.addAlliance(second.getId());
        second.addAlliance(first.getId());

        first.removeIncomingAllianceRequest(second.getId());
        first.removeOutgoingAllianceRequest(second.getId());
        second.removeIncomingAllianceRequest(first.getId());
        second.removeOutgoingAllianceRequest(first.getId());

        saveClan(first);
        saveClan(second);
    }

    private void removeAllAllianceRelations(Clan clan) {
        if (clan == null) return;

        for (String allyId : new HashSet<>(clan.getAllianceIds())) {
            Clan ally = getClan(allyId);
            if (ally != null) {
                ally.removeAlliance(clan.getId());
                ally.removeIncomingAllianceRequest(clan.getId());
                ally.removeOutgoingAllianceRequest(clan.getId());

                saveClan(ally);

                broadcastToClan(ally, lang.getRaw("alliance.removed_notify")
                        .replace("{clan}", clan.getName()));
            }
        }

        for (String requestId : new HashSet<>(clan.getIncomingAllianceRequests())) {
            Clan other = getClan(requestId);
            if (other != null) {
                other.removeOutgoingAllianceRequest(clan.getId());
                saveClan(other);
            }
        }

        for (String requestId : new HashSet<>(clan.getOutgoingAllianceRequests())) {
            Clan other = getClan(requestId);
            if (other != null) {
                other.removeIncomingAllianceRequest(clan.getId());
                saveClan(other);
            }
        }

        clan.getAllianceIds().clear();
        clan.getIncomingAllianceRequests().clear();
        clan.getOutgoingAllianceRequests().clear();

        saveClan(clan);
    }

    private void notifyClanOfficers(Clan clan, String message) {
        if (clan == null) return;

        Set<UUID> recipients = new HashSet<>(clan.getOfficers());
        recipients.add(clan.getLeader());

        for (UUID memberId : recipients) {
            Player online = Bukkit.getPlayer(memberId);
            if (online != null && online.isOnline()) {
                online.sendMessage(lang.getRaw("prefix") + message);
            }
        }
    }

    private void sendAllianceAcceptHint(Clan targetClan, Clan senderClan) {
        if (targetClan == null || senderClan == null) return;

        Set<UUID> recipients = new HashSet<>(targetClan.getOfficers());
        recipients.add(targetClan.getLeader());

        for (UUID memberId : recipients) {
            Player online = Bukkit.getPlayer(memberId);
            if (online != null && online.isOnline()) {
                lang.send(online, "alliance.clickable_prompt",
                        lang.placeholders("clan", senderClan.getName()));
                online.sendMessage("§a/clan ally accept " + senderClan.getName());
                online.sendMessage("§c/clan ally deny " + senderClan.getName());
            }
        }
    }

    private void broadcastToClan(Clan clan, String message) {
        if (clan == null) return;

        for (UUID memberId : clan.getMembers()) {
            Player online = Bukkit.getPlayer(memberId);
            if (online != null && online.isOnline()) {
                online.sendMessage(lang.getRaw("prefix") + message);
            }
        }
    }

    private static final class PendingInvite {
        private final String clanId;
        private final long createdAt;

        private PendingInvite(String clanId, long createdAt) {
            this.clanId = clanId;
            this.createdAt = createdAt;
        }

        private String getClanId() {
            return clanId;
        }

        private boolean isExpired() {
            return System.currentTimeMillis() - createdAt > INVITE_TTL_MS;
        }

        private boolean matchesClanName(String clanName, ClanManager manager) {
            Clan clan = manager.getClan(clanId);
            return clan != null && clan.getName().equalsIgnoreCase(clanName);
        }
    }
}