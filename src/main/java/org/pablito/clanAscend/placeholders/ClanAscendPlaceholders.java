package org.pablito.clanAscend.placeholders;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.objects.Clan;

public class ClanAscendPlaceholders extends PlaceholderExpansion {

    private final ClanAscend plugin;

    public ClanAscendPlaceholders(ClanAscend plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() {
        return "clanascend";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Pablito";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginMeta().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
        if (player == null) return "";

        Clan clan = plugin.getClanManager().getPlayerClan(player.getUniqueId());

        switch (params.toLowerCase()) {

            case "clan":
            case "name":
                return clan != null ? clan.getName() : "";

            case "tag":
                return clan != null ? clan.getTag() : "";

            case "clan_formatted":
                return clan != null ? "[" + clan.getTag() + "] " + clan.getName() : "";

            case "in_clan":
                return clan != null ? "yes" : "no";

            case "members":
                return clan != null ? String.valueOf(clan.getMemberCount()) : "0";

            case "max_members":
                return clan != null ? String.valueOf(clan.getMaxMembers()) : "0";

            case "power":
                return clan != null ? String.valueOf(clan.getPower()) : "0";

            case "max_power":
                return clan != null ? String.valueOf(clan.getMaxPower()) : "0";

            case "level":
                return clan != null ? String.valueOf(clan.getLevel()) : "0";

            case "exp":
            case "experience":
                return clan != null ? String.valueOf(clan.getExperience()) : "0";

            case "claims":
                return clan != null ? String.valueOf(clan.getClaimCount()) : "0";

            case "max_claims":
                return clan != null ? String.valueOf(clan.getMaxClaims()) : "0";

            case "leader":
                return clan != null ? clan.getLeaderName() : "";

            case "desc":
            case "description":
                return clan != null ? safe(clan.getDescription()) : "";

            case "created":
                return clan != null ? clan.getFormattedCreationDate() : "";

            case "allies":
                return clan != null ? String.valueOf(clan.getAllianceIds().size()) : "0";

            case "is_open":
                return clan != null && clan.isOpen() ? "yes" : "no";

            case "role":
                if (clan == null) return "";
                if (clan.isLeader(player.getUniqueId())) return "leader";
                if (clan.isOfficer(player.getUniqueId())) return "officer";
                return "member";

            case "role_formatted":
                if (clan == null) return "";
                if (clan.isLeader(player.getUniqueId())) return "Leader";
                if (clan.isOfficer(player.getUniqueId())) return "Officer";
                return "Member";

            default:
                return null;
        }
    }

    private String safe(String s) {
        if (s == null) return "";
        return s.replace("\n", " ").replace("\r", " ").trim();
    }
}