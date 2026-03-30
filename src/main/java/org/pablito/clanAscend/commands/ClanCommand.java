package org.pablito.clanAscend.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.managers.ClanManager;
import org.pablito.clanAscend.managers.LanguageManager;
import org.pablito.clanAscend.objects.Clan;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ClanCommand implements CommandExecutor {

    private final ClanAscend plugin;
    private final ClanManager clanManager;
    private final LanguageManager lang;

    public ClanCommand(ClanAscend plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.lang = plugin.getLanguageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        if (!(sender instanceof Player player)) {
            lang.send(sender, "system.command_only_players");
            return true;
        }

        if (args.length == 0) {
            if (plugin.getClanGUI() != null && clanManager.hasClan(player)) {
                plugin.getClanGUI().openClanGUI(player);
            } else {
                showHelp(player);
            }
            return true;
        }

        String sub = args[0].toLowerCase(Locale.ROOT);

        switch (sub) {

            case "help":
                showHelp(player);
                return true;

            case "gui":
                if (!clanManager.hasClan(player)) {
                    lang.send(player, "clan.not_in_clan");
                    return true;
                }
                if (plugin.getClanGUI() != null) {
                    plugin.getClanGUI().openClanGUI(player);
                }
                return true;

            case "create":
                if (args.length < 3) {
                    lang.send(player, "command.clan.create.usage");
                    return true;
                }

                // Validate tag length
                if (args[2].length() < 2 || args[2].length() > 4) {
                    lang.send(player, "command.clan.create.bad_tag");
                    return true;
                }

                clanManager.createClan(player, args[1], args[2]);
                return true;

            case "invite":
                if (args.length < 2) {
                    lang.send(player, "command.clan.invite.usage");
                    return true;
                }

                Player target = plugin.getServer().getPlayer(args[1]);
                if (target == null) {
                    lang.send(player, "error.player_offline");
                    return true;
                }

                clanManager.invitePlayer(player, target);
                return true;

            case "accept":
                if (args.length < 2) {
                    lang.send(player, "command.clan.accept.usage");
                    return true;
                }
                clanManager.acceptInvite(player, args[1]);
                return true;

            case "decline":
            case "deny":
            case "reject":
                if (args.length < 2) {
                    lang.send(player, "command.clan.decline.usage");
                    return true;
                }
                clanManager.declineInvite(player, args[1]);
                return true;

            case "ally":
            case "alliance":

                if (args.length < 2) {
                    lang.send(player, "command.clan.ally.usage");
                    return true;
                }

                if (args[1].equalsIgnoreCase("list")) {
                    clanManager.showAllies(player);
                    return true;
                }

                if (args[1].equalsIgnoreCase("accept")) {
                    if (args.length < 3) {
                        lang.send(player, "command.clan.ally.accept_usage");
                        return true;
                    }
                    clanManager.acceptAllianceRequest(player, args[2]);
                    return true;
                }

                if (args[1].equalsIgnoreCase("deny")
                        || args[1].equalsIgnoreCase("decline")
                        || args[1].equalsIgnoreCase("reject")) {

                    if (args.length < 3) {
                        lang.send(player, "command.clan.ally.deny_usage");
                        return true;
                    }
                    clanManager.denyAllianceRequest(player, args[2]);
                    return true;
                }

                if (args[1].equalsIgnoreCase("remove")
                        || args[1].equalsIgnoreCase("break")) {

                    if (args.length < 3) {
                        lang.send(player, "command.clan.ally.remove_usage");
                        return true;
                    }
                    clanManager.removeAlliance(player, args[2]);
                    return true;
                }

                clanManager.sendAllianceRequest(player, args[1]);
                return true;

            case "allies":
                clanManager.showAllies(player);
                return true;

            case "leave":
                clanManager.leaveClan(player);
                return true;

            case "kick":
                if (args.length < 2) {
                    lang.send(player, "command.clan.kick.usage");
                    return true;
                }
                clanManager.kickPlayer(player, args[1]);
                return true;

            case "claim":
                if (!plugin.getConfig().getBoolean("claims.enabled", true)) {
                    lang.send(player, "claim.disabled");
                    return true;
                }
                clanManager.claimChunk(player);
                return true;

            case "unclaim":
                if (!plugin.getConfig().getBoolean("claims.enabled", true)) {
                    lang.send(player, "claim.disabled");
                    return true;
                }
                clanManager.unclaimChunk(player);
                return true;

            case "setlimit": {
                Clan clan = clanManager.getPlayerClan(player);

                if (clan == null) {
                    lang.send(player, "clan.not_in_clan");
                    return true;
                }

                if (!clan.hasPermission(player.getUniqueId(), "officer")) {
                    lang.send(player, "clan.no_permission");
                    return true;
                }

                if (args.length < 2) {
                    lang.send(player, "command.clan.setlimit.usage");
                    return true;
                }

                int limit;
                try {
                    limit = Integer.parseInt(args[1]);
                } catch (NumberFormatException ex) {
                    lang.send(player, "error.invalid_number");
                    return true;
                }

                int clamped = Math.max(5, Math.min(50, limit));

                if (clamped < clan.getMemberCount()) {
                    lang.send(player, "clan.limit_too_low",
                            lang.placeholders(
                                    "current", String.valueOf(clan.getMemberCount()),
                                    "limit", String.valueOf(clamped)
                            ));
                    return true;
                }

                clan.setMaxMembers(clamped);
                clanManager.saveClan(clan);

                lang.send(player, "clan.limit_set",
                        lang.placeholders("max", String.valueOf(clamped)));
                return true;
            }

            case "setdesc":
            case "desc":
            case "description": {

                Clan clan = clanManager.getPlayerClan(player);

                if (clan == null) {
                    lang.send(player, "clan.not_in_clan");
                    return true;
                }

                if (!clan.hasPermission(player.getUniqueId(), "officer")) {
                    lang.send(player, "clan.no_permission");
                    return true;
                }

                if (args.length < 2) {
                    lang.send(player, "command.clan.setdesc.usage");
                    return true;
                }

                String desc = String.join(" ", Arrays.copyOfRange(args, 1, args.length)).trim();

                if (desc.isEmpty()) {
                    lang.send(player, "command.clan.setdesc.usage");
                    return true;
                }

                int maxLen = plugin.getConfig().getInt("clan.description-max-length", 64);
                if (maxLen < 16) maxLen = 16;

                if (desc.length() > maxLen) {
                    desc = desc.substring(0, maxLen);
                }

                clan.setDescription(desc);
                clanManager.saveClan(clan);

                lang.send(player, "clan.description_set",
                        lang.placeholders("description", desc));
                return true;
            }

            case "info": {
                Clan clan = clanManager.getPlayerClan(player);

                if (clan == null) {
                    lang.send(player, "clan.not_in_clan");
                    return true;
                }

                // Send header
                lang.send(player, "clan.info.header",
                        lang.placeholders("clan", clan.getName()));

                // Send tag line
                lang.send(player, "clan.info.line_tag",
                        lang.placeholders("tag", clan.getTag()));

                // Send leader line
                lang.send(player, "clan.info.line_leader",
                        lang.placeholders("leader", clan.getLeaderName()));

                // Send members line
                lang.send(player, "clan.info.line_members",
                        lang.placeholders(
                                "members", String.valueOf(clan.getMemberCount()),
                                "max_members", String.valueOf(clan.getMaxMembers())
                        ));

                // Send power line
                lang.send(player, "clan.info.line_power",
                        lang.placeholders(
                                "power", String.valueOf(clan.getPower()),
                                "max_power", String.valueOf(clan.getMaxPower())
                        ));

                // Send claims line
                lang.send(player, "clan.info.line_claims",
                        lang.placeholders(
                                "claims", String.valueOf(clan.getClaimCount()),
                                "max_claims", String.valueOf(clan.getMaxClaims())
                        ));

                // Send level line
                lang.send(player, "clan.info.line_level",
                        lang.placeholders(
                                "level", String.valueOf(clan.getLevel()),
                                "experience", String.valueOf(clan.getExperience())
                        ));

                // Send allies line
                lang.send(player, "clan.info.line_allies",
                        lang.placeholders("allies", String.valueOf(clan.getAllianceIds().size())));

                // Send description line
                String desc = clan.getDescription();
                if (desc == null || desc.isEmpty()) {
                    desc = lang.getRaw("clan.no_description");
                }

                lang.send(player, "clan.info.line_description",
                        lang.placeholders("description", desc));

                return true;
            }

            case "top":
            case "list":
                showTopClans(player);
                return true;

            case "chat":
                if (plugin.getClanChatListener() != null) {
                    boolean enabled = plugin.getClanChatListener().toggleClanChat(player);
                    lang.send(player, enabled ? "chat.toggled_on" : "chat.toggled_off");
                } else {
                    lang.send(player, "error.chat_not_available");
                }
                return true;

            default:
                lang.send(player, "error.unknown_command");
                showHelp(player);
                return true;
        }
    }

    private void showHelp(Player player) {
        List<String> helpLines = lang.getList("command.clan.help");
        if (helpLines.isEmpty()) {
            // Default help if language file doesn't have it
            player.sendMessage("§6=== ClanAscend - Commands ===");
            player.sendMessage("§e/clan §7- Open clan menu");
            player.sendMessage("§e/clan help §7- Show this help");
            player.sendMessage("§e/clan create <name> <tag> §7- Create a clan");
            player.sendMessage("§e/clan invite <player> §7- Invite a player");
            player.sendMessage("§e/clan accept <clan> §7- Accept an invite");
            player.sendMessage("§e/clan decline <clan> §7- Decline an invite");
            player.sendMessage("§e/clan kick <player> §7- Kick a member");
            player.sendMessage("§e/clan leave §7- Leave your clan");
            player.sendMessage("§e/clan claim §7- Claim current chunk");
            player.sendMessage("§e/clan unclaim §7- Unclaim current chunk");
            player.sendMessage("§e/clan info §7- Show clan info");
            player.sendMessage("§e/clan list §7- List clans");
            player.sendMessage("§e/clan chat §7- Toggle clan chat");
            player.sendMessage("§e/clan top §7- Show top clans");
            player.sendMessage("§e/clan ally <clan> §7- Send alliance request");
            player.sendMessage("§e/clan allies §7- Show your alliances");
        } else {
            for (String line : helpLines) {
                lang.send(player, line);
            }
        }
    }

    private void showTopClans(Player player) {
        // This would need to be implemented with your leaderboard manager
        lang.send(player, "top.header");

        List<Clan> topClans = plugin.getLeaderboardManager() != null
                ? plugin.getLeaderboardManager().getTopClans(10)
                : new java.util.ArrayList<>();

        if (topClans.isEmpty()) {
            lang.send(player, "top.empty");
        } else {
            int rank = 1;
            for (Clan clan : topClans) {
                lang.send(player, "top.line",
                        lang.placeholders(
                                "rank", String.valueOf(rank++),
                                "name", clan.getName(),
                                "tag", clan.getTag(),
                                "power", String.valueOf(clan.getPower()),
                                "members", String.valueOf(clan.getMemberCount())
                        ));
            }
        }

        lang.send(player, "top.footer");
    }
}