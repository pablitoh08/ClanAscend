package org.pablito.clanAscend.commands;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.managers.ClanManager;
import org.pablito.clanAscend.managers.LanguageManager;
import org.pablito.clanAscend.objects.Clan;
import org.pablito.clanAscend.objects.ChunkLocation;

import java.lang.reflect.Field;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class ClanAdminCommand implements CommandExecutor {

    private final ClanAscend plugin;
    private final ClanManager clanManager;
    private final LanguageManager lang;

    public ClanAdminCommand(ClanAscend plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.lang = plugin.getLang();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            showAdminHelp(sender);
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {
            case "help":
                showAdminHelp(sender);
                return true;

            case "reload":
                handleReload(sender);
                return true;

            case "setpower":
                handleSetPower(sender, args);
                return true;

            case "setlevel":
                handleSetLevel(sender, args);
                return true;

            case "disband":
                handleDisband(sender, args);
                return true;

            case "addmember":
                handleAddMember(sender, args);
                return true;

            case "info":
                handleAdminInfo(sender, args);
                return true;

            case "forceunclaim":
                handleForceUnclaim(sender);
                return true;

            default:
                lang.send(sender, "error.unknown_command");
                showAdminHelp(sender);
                return true;
        }
    }

    private void handleReload(CommandSender sender) {
        if (!sender.hasPermission("clanascend.reload")) {
            lang.send(sender, "error.no_permission");
            return;
        }

        plugin.reloadConfig();
        lang.reload();

        lang.send(sender, "admin.reload.success");
    }

    private void handleSetPower(CommandSender sender, String[] args) {
        if (!sender.hasPermission("clanascend.admin")) {
            lang.send(sender, "error.no_permission");
            return;
        }

        if (args.length < 3) {
            lang.send(sender, "admin.setpower.usage");
            return;
        }

        Clan clan = clanManager.getClan(args[1]);
        if (clan == null) {
            lang.send(sender, "error.clan_not_found");
            return;
        }

        int power;
        try {
            power = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            lang.send(sender, "admin.must_be_number");
            return;
        }

        if (power < 0) {
            lang.send(sender, "admin.power_cannot_be_negative");
            return;
        }

        clan.setPower(power);
        clanManager.saveAllClans();

        lang.send(sender, "admin.setpower.done",
                lang.placeholders(
                        "clan", clan.getName(),
                        "power", String.valueOf(power)
                ));
    }

    private void handleSetLevel(CommandSender sender, String[] args) {
        if (!sender.hasPermission("clanascend.admin")) {
            lang.send(sender, "error.no_permission");
            return;
        }

        if (args.length < 3) {
            lang.send(sender, "admin.setlevel.usage");
            return;
        }

        Clan clan = clanManager.getClan(args[1]);
        if (clan == null) {
            lang.send(sender, "error.clan_not_found");
            return;
        }

        int level;
        try {
            level = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            lang.send(sender, "admin.must_be_number");
            return;
        }

        if (level < 1 || level > 100) {
            lang.send(sender, "admin.level_range");
            return;
        }

        clan.setLevel(level);
        clanManager.saveAllClans();

        lang.send(sender, "admin.setlevel.done",
                lang.placeholders(
                        "clan", clan.getName(),
                        "level", String.valueOf(level)
                ));
    }

    private void handleDisband(CommandSender sender, String[] args) {
        if (!sender.hasPermission("clanascend.disband.others")) {
            lang.send(sender, "error.no_permission");
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "admin.disband.usage");
            return;
        }

        Clan clan = clanManager.getClan(args[1]);
        if (clan == null) {
            lang.send(sender, "error.clan_not_found");
            return;
        }

        clanManager.disbandClan(clan, sender.getName());
        lang.send(sender, "admin.disband.done",
                lang.placeholders("clan", clan.getName()));
    }

    private void handleAddMember(CommandSender sender, String[] args) {
        if (!sender.hasPermission("clanascend.admin")) {
            lang.send(sender, "error.no_permission");
            return;
        }

        if (args.length < 3) {
            lang.send(sender, "admin.addmember.usage");
            return;
        }

        Clan clan = clanManager.getClan(args[1]);
        if (clan == null) {
            lang.send(sender, "error.clan_not_found");
            return;
        }

        Player target = Bukkit.getPlayer(args[2]);
        if (target == null) {
            lang.send(sender, "error.player_offline");
            return;
        }

        if (clanManager.getPlayerClan(target) != null) {
            lang.send(sender, "admin.addmember.target_already_in_clan");
            return;
        }

        if (clan.getMemberCount() >= clan.getMaxMembers()) {
            lang.send(sender, "admin.addmember.clan_full");
            return;
        }

        if (clan.addMember(target.getUniqueId())) {
            Map<UUID, String> playerClans = getPlayerClansMap();
            if (playerClans != null) {
                playerClans.put(target.getUniqueId(), clan.getId());
            }

            clanManager.saveAllClans();

            lang.send(sender, "admin.addmember.done",
                    lang.placeholders(
                            "player", target.getName(),
                            "clan", clan.getName()
                    ));

            lang.send(target, "admin.addmember.notify_target",
                    lang.placeholders("clan", clan.getName()));
        } else {
            lang.send(sender, "admin.addmember.failed");
        }
    }

    private void handleAdminInfo(CommandSender sender, String[] args) {
        if (!sender.hasPermission("clanascend.admin")) {
            lang.send(sender, "error.no_permission");
            return;
        }

        if (args.length < 2) {
            lang.send(sender, "admin.info.usage");
            return;
        }

        Clan clan = clanManager.getClan(args[1]);
        if (clan == null) {
            lang.send(sender, "error.clan_not_found");
            return;
        }

        lang.send(sender, "admin.info.header");

        lang.send(sender, "admin.info.name",
                lang.placeholders("name", clan.getName()));
        lang.send(sender, "admin.info.tag",
                lang.placeholders("tag", clan.getTag()));
        lang.send(sender, "admin.info.leader",
                lang.placeholders("leader", clan.getLeaderName()));
        lang.send(sender, "admin.info.power",
                lang.placeholders(
                        "power", String.valueOf(clan.getPower()),
                        "max", String.valueOf(clan.getMaxPower())
                ));
        lang.send(sender, "admin.info.level",
                lang.placeholders(
                        "level", String.valueOf(clan.getLevel()),
                        "exp", String.valueOf(clan.getExperience())
                ));
        lang.send(sender, "admin.info.members",
                lang.placeholders(
                        "members", String.valueOf(clan.getMemberCount()),
                        "max", String.valueOf(clan.getMaxMembers())
                ));
        lang.send(sender, "admin.info.claims",
                lang.placeholders(
                        "claims", String.valueOf(clan.getClaimCount()),
                        "max", String.valueOf(clan.getMaxClaims())
                ));
        lang.send(sender, "admin.info.desc",
                lang.placeholders("desc", clan.getDescription()));
        lang.send(sender, "admin.info.created",
                lang.placeholders("created", String.valueOf(clan.getCreationDate())));
        lang.send(sender, "admin.info.open",
                lang.placeholders("open", clan.isOpen() ? lang.get("admin.yes") : lang.get("admin.no")));

        Object ffObj = clan.getSettings().get("friendlyFire");
        boolean friendlyFire = false;
        if (ffObj instanceof Boolean) {
            friendlyFire = ((Boolean) ffObj).booleanValue();
        } else if (ffObj != null) {
            friendlyFire = Boolean.parseBoolean(ffObj.toString());
        }

        lang.send(sender, "admin.info.friendlyfire",
                lang.placeholders("ff", friendlyFire ? lang.get("admin.enabled") : lang.get("admin.disabled")));

        lang.send(sender, "admin.info.members_header",
                lang.placeholders("count", String.valueOf(clan.getMemberCount())));

        int onlineCount = 0;
        int offlineCount = 0;

        for (UUID memberId : clan.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            boolean online = member != null && member.isOnline();

            String status = online ? lang.get("admin.online") : lang.get("admin.offline");
            String role = clan.isLeader(memberId) ? lang.get("admin.role_leader")
                    : clan.isOfficer(memberId) ? lang.get("admin.role_officer")
                    : lang.get("admin.role_member");

            if (online) {
                onlineCount++;
            } else {
                offlineCount++;
            }

            lang.send(sender, "admin.info.member_line",
                    lang.placeholders(
                            "role", role,
                            "player", getPlayerName(memberId),
                            "status", status
                    ));
        }

        lang.send(sender, "admin.info.online_offline",
                lang.placeholders(
                        "online", String.valueOf(onlineCount),
                        "offline", String.valueOf(offlineCount)
                ));
    }

    private void handleForceUnclaim(CommandSender sender) {
        if (!sender.hasPermission("clanascend.admin")) {
            lang.send(sender, "error.no_permission");
            return;
        }

        if (!(sender instanceof Player)) {
            lang.send(sender, "system.command_only_players");
            return;
        }

        Player player = (Player) sender;

        Clan clanAtLocation = clanManager.getClanAtLocation(player.getLocation());
        if (clanAtLocation == null) {
            lang.send(sender, "admin.forceunclaim.none_here");
            return;
        }

        ChunkLocation chunk = new ChunkLocation(player.getLocation().getChunk());

        clanAtLocation.removeClaim(chunk);

        Map<ChunkLocation, String> claimedChunks = getClaimedChunksMap();
        if (claimedChunks != null) {
            claimedChunks.remove(chunk);
        }

        int refund = plugin.getConfig().getInt("claims.claim-refund", 5);
        clanAtLocation.addPower(refund);

        clanManager.saveAllClans();

        lang.send(sender, "admin.forceunclaim.done",
                lang.placeholders("clan", clanAtLocation.getName()));
        lang.send(sender, "admin.forceunclaim.refund",
                lang.placeholders("refund", String.valueOf(refund)));
    }

    private String getPlayerName(UUID uuid) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
        String name = off != null ? off.getName() : null;
        return name != null ? name : lang.get("system.offline_name");
    }

    private void showAdminHelp(CommandSender sender) {
        if (!sender.hasPermission("clanascend.admin")) {
            lang.send(sender, "error.no_permission");
            return;
        }

        for (String line : lang.getList("admin.help")) {
            lang.send(sender, line);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<UUID, String> getPlayerClansMap() {
        try {
            Field field = ClanManager.class.getDeclaredField("playerClans");
            field.setAccessible(true);
            return (Map<UUID, String>) field.get(clanManager);
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Map<ChunkLocation, String> getClaimedChunksMap() {
        try {
            Field field = ClanManager.class.getDeclaredField("claimedChunks");
            field.setAccessible(true);
            return (Map<ChunkLocation, String>) field.get(clanManager);
        } catch (Exception e) {
            return null;
        }
    }
}