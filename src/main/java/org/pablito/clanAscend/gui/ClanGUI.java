package org.pablito.clanAscend.gui;

import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.managers.ClanManager;
import org.pablito.clanAscend.managers.LanguageManager;
import org.pablito.clanAscend.objects.Clan;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.*;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class ClanGUI {

    public enum GuiType { MAIN, MEMBERS, MEMBER_MANAGE, CLAIMS, SETTINGS, CLAN_CHEST, CONFIRM_DISBAND }

    public static class ClanGuiHolder implements InventoryHolder {
        private final GuiType type;
        private final String clanId;
        private final String targetUuid;

        public ClanGuiHolder(GuiType type, String clanId) {
            this(type, clanId, null);
        }

        public ClanGuiHolder(GuiType type, String clanId, String targetUuid) {
            this.type = type;
            this.clanId = clanId;
            this.targetUuid = targetUuid;
        }

        public GuiType getType() { return type; }
        public String getClanId() { return clanId; }
        public String getTargetUuid() { return targetUuid; }

        @Override
        public @Nullable Inventory getInventory() { return null; }
    }

    private final ClanAscend plugin;
    private final ClanManager clanManager;
    private final LanguageManager lang;

    private final NamespacedKey memberUuidKey;

    public ClanGUI(ClanAscend plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.lang = plugin.getLang();
        this.memberUuidKey = new NamespacedKey(plugin, "member_uuid");
    }

    // ---------------- MAIN ----------------

    public void openClanGUI(Player player) {
        Clan clan = clanManager.getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        String title = lang.get("gui.titles.main").replace("{clan}", clan.getName());
        Inventory gui = Bukkit.createInventory(new ClanGuiHolder(GuiType.MAIN, clan.getId()), 54, net.kyori.adventure.text.Component.text(title));

        gui.setItem(4, createInfoItem(clan, player));
        gui.setItem(20, createMembersItem(clan));
        if (plugin.getConfig().getBoolean("claims.enabled", true)) {
            gui.setItem(22, createClaimsItem(clan));
        }
        gui.setItem(24, createPowerItem(clan));

        gui.setItem(33, createChestItem());

        if (clan.hasPermission(player.getUniqueId(), "officer")) {
            gui.setItem(31, createSettingsItem());
        }

        gui.setItem(40, createChatItem());
        gui.setItem(42, createTopClansItem());
        gui.setItem(49, createCloseItem());

        if (clan.isLeader(player.getUniqueId())) {
            gui.setItem(53, createDisbandItem());
        }

        player.openInventory(gui);
    }

    // ---------------- CLAN CHEST ----------------

    public void openClanChestGUI(Player player) {
        Clan clan = clanManager.getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        String title = lang.get("gui.titles.chest").replace("{clan}", clan.getName());
        Inventory inv = Bukkit.createInventory(new ClanGuiHolder(GuiType.CLAN_CHEST, clan.getId()), 54, net.kyori.adventure.text.Component.text(title));

        Object raw = clan.getSettings().get("clanChest");
        if (raw instanceof String) {
            try {
                ItemStack[] contents = org.pablito.clanAscend.utils.InventorySerializer
                        .itemStackArrayFromBase64((String) raw);
                for (int i = 0; i < Math.min(contents.length, inv.getSize()); i++) {
                    inv.setItem(i, contents[i]);
                }
            } catch (Exception ignored) {
            }
        }

        player.openInventory(inv);
    }

    public void openMembersGUI(Player player) {
        Clan clan = clanManager.getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        String title = lang.get("gui.titles.members").replace("{clan}", clan.getName());
        Inventory gui = Bukkit.createInventory(new ClanGuiHolder(GuiType.MEMBERS, clan.getId()), 54, net.kyori.adventure.text.Component.text(title));

        int slot = 0;
        for (UUID memberId : clan.getMembers()) {
            if (slot >= 45) break;

            ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta meta = (SkullMeta) skull.getItemMeta();

            OfflinePlayer off = Bukkit.getOfflinePlayer(memberId);
            String name = (off.getName() != null) ? off.getName() : lang.get("system.offline_name");

            meta.setOwningPlayer(off);
            meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.members.member_name").replace("{player}", name)));

            boolean online = Bukkit.getPlayer(memberId) != null;

            String roleKey = clan.isLeader(memberId) ? "gui.members.roles.leader"
                    : clan.isOfficer(memberId) ? "gui.members.roles.officer"
                    : "gui.members.roles.member";

            String statusKey = online ? "gui.members.status.online" : "gui.members.status.offline";

            String extra = clan.hasPermission(player.getUniqueId(), "officer")
                    ? lang.get("gui.members.extra.leader_hint")
                    : lang.get("gui.members.extra.no_perm");

            List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
            for (String line : lang.getList("gui.members.member_lore")) {
                String formatted = line
                        .replace("{role}", lang.get(roleKey))
                        .replace("{status}", lang.get(statusKey))
                        .replace("{extra}", extra);
                lore.add(net.kyori.adventure.text.Component.text(formatted));
            }
            meta.lore(lore);

            meta.getPersistentDataContainer().set(memberUuidKey, PersistentDataType.STRING, memberId.toString());

            skull.setItemMeta(meta);
            gui.setItem(slot++, skull);
        }

        gui.setItem(49, createBackItem());
        player.openInventory(gui);
    }

    public void openMemberManageGUI(Player executor, UUID targetId) {
        Clan clan = clanManager.getPlayerClan(executor);
        if (clan == null) {
            lang.send(executor, "clan.not_in_clan");
            return;
        }

        OfflinePlayer target = Bukkit.getOfflinePlayer(targetId);
        String targetName = (target.getName() != null) ? target.getName() : lang.get("system.offline_name");

        String title = lang.get("gui.manage_member.title").replace("{player}", targetName);

        Inventory gui = Bukkit.createInventory(
                new ClanGuiHolder(GuiType.MEMBER_MANAGE, clan.getId(), targetId.toString()),
                27,
                net.kyori.adventure.text.Component.text(title)
        );

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(net.kyori.adventure.text.Component.text(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);

        gui.setItem(4, createMemberInfoItem(clan, targetId, targetName));

        boolean executorLeader = clan.isLeader(executor.getUniqueId());
        boolean executorOfficer = clan.hasPermission(executor.getUniqueId(), "officer");

        boolean targetIsLeader = clan.isLeader(targetId);
        boolean targetIsSelf = executor.getUniqueId().equals(targetId);

        if (executorLeader && !targetIsLeader && !targetIsSelf) {
            gui.setItem(10, clan.isOfficer(targetId) ? createDemoteItem() : createPromoteItem());
        } else {
            String reason;
            if (targetIsSelf) reason = "gui.manage_member.disabled.reason_self";
            else if (targetIsLeader) reason = "gui.manage_member.disabled.reason_leader";
            else reason = "gui.manage_member.disabled.reason_no_perm";
            gui.setItem(10, createDisabledItem(reason));
        }

        if (executorLeader && !targetIsLeader && !targetIsSelf) {
            gui.setItem(16, createTransferItem());
        } else {
            String reason;
            if (targetIsSelf) reason = "gui.manage_member.disabled.reason_self";
            else if (targetIsLeader) reason = "gui.manage_member.disabled.reason_leader";
            else reason = "gui.manage_member.disabled.reason_no_perm";
            gui.setItem(16, createDisabledItem(reason));
        }

        if (executorOfficer && !targetIsLeader && !targetIsSelf) {
            gui.setItem(22, createKickItem());
        } else {
            String reason;
            if (targetIsSelf) reason = "gui.manage_member.disabled.reason_self";
            else if (targetIsLeader) reason = "gui.manage_member.disabled.reason_leader";
            else reason = "gui.manage_member.disabled.reason_no_perm";
            gui.setItem(22, createDisabledItem(reason));
        }

        if (executorOfficer && !targetIsLeader && !targetIsSelf) {
            gui.setItem(24, createBanItem());
        } else {
            String reason;
            if (targetIsSelf) reason = "gui.manage_member.disabled.reason_self";
            else if (targetIsLeader) reason = "gui.manage_member.disabled.reason_leader";
            else reason = "gui.manage_member.disabled.reason_no_perm";
            gui.setItem(24, createDisabledItem(reason));
        }

        gui.setItem(18, createBackItem());

        executor.openInventory(gui);
    }

    private ItemStack createMemberInfoItem(Clan clan, UUID targetId, String targetName) {
        ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) skull.getItemMeta();

        OfflinePlayer off = Bukkit.getOfflinePlayer(targetId);
        meta.setOwningPlayer(off);

        String role = clan.isLeader(targetId) ? lang.get("gui.members.roles.leader")
                : clan.isOfficer(targetId) ? lang.get("gui.members.roles.officer")
                : lang.get("gui.members.roles.member");

        boolean online = Bukkit.getPlayer(targetId) != null;

        String onlineStr = online ? lang.get("gui.members.status.online") : lang.get("gui.members.status.offline");

        String bannedStr = clan.isBanned(targetId)
                ? lang.get("gui.manage_member.info.banned_yes")
                : lang.get("gui.manage_member.info.banned_no");

        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.manage_member.player.name").replace("{player}", targetName)));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.manage_member.player.lore")) {
            String formatted = line
                    .replace("{role}", role)
                    .replace("{online}", onlineStr)
                    .replace("{uuid}", targetId.toString())
                    .replace("{banned}", bannedStr);
            lore.add(net.kyori.adventure.text.Component.text(formatted));
        }
        meta.lore(lore);

        skull.setItemMeta(meta);
        return skull;
    }

    public void openClaimsGUI(Player player) {
        Clan clan = clanManager.getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }

        String title = lang.get("gui.titles.claims").replace("{clan}", clan.getName());
        Inventory gui = Bukkit.createInventory(new ClanGuiHolder(GuiType.CLAIMS, clan.getId()), 54, net.kyori.adventure.text.Component.text(title));

        int claimCost = plugin.getConfig().getInt("claims.claim-cost", plugin.getConfig().getInt("claims.claim_cost_power", 10));
        int refund = plugin.getConfig().getInt("claims.claim-refund", 5);

        ItemStack info = new ItemStack(Material.MAP);
        ItemMeta meta = info.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.claims.info.name")));

        List<net.kyori.adventure.text.Component> infoLore = new ArrayList<>();
        for (String line : lang.getList("gui.claims.info.lore")) {
            String formatted = line
                    .replace("{claims}", String.valueOf(clan.getClaimCount()))
                    .replace("{max}", String.valueOf(clan.getMaxClaims()))
                    .replace("{power}", String.valueOf(clan.getPower()))
                    .replace("{max_power}", String.valueOf(clan.getMaxPower()))
                    .replace("{cost}", String.valueOf(claimCost))
                    .replace("{refund}", String.valueOf(refund));
            infoLore.add(net.kyori.adventure.text.Component.text(formatted));
        }
        meta.lore(infoLore);
        info.setItemMeta(meta);
        gui.setItem(4, info);

        ItemStack claimedItem = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta claimMeta = claimedItem.getItemMeta();
        claimMeta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.claims.claimed_item.name")));

        List<net.kyori.adventure.text.Component> claimedLore = new ArrayList<>();
        for (String line : lang.getList("gui.claims.claimed_item.lore")) {
            claimedLore.add(net.kyori.adventure.text.Component.text(line));
        }
        claimMeta.lore(claimedLore);
        claimedItem.setItemMeta(claimMeta);

        int slot = 9;
        for (int i = 0; i < Math.min(clan.getClaimCount(), 36); i++) {
            gui.setItem(slot, claimedItem);
            slot++;
            if ((slot - 8) % 9 == 0) slot += 2;
        }

        if (clan.hasPermission(player.getUniqueId(), "officer")) {
            ItemStack newClaim = new ItemStack(Material.EMERALD_BLOCK);
            ItemMeta newMeta = newClaim.getItemMeta();
            newMeta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.claims.claim_button.name")));

            List<net.kyori.adventure.text.Component> newClaimLore = new ArrayList<>();
            for (String line : lang.getList("gui.claims.claim_button.lore")) {
                newClaimLore.add(net.kyori.adventure.text.Component.text(line.replace("{cost}", String.valueOf(claimCost))));
            }
            newMeta.lore(newClaimLore);
            newClaim.setItemMeta(newMeta);
            gui.setItem(47, newClaim);

            ItemStack unclaim = new ItemStack(Material.REDSTONE_BLOCK);
            ItemMeta unMeta = unclaim.getItemMeta();
            unMeta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.claims.unclaim_button.name")));

            List<net.kyori.adventure.text.Component> unclaimLore = new ArrayList<>();
            for (String line : lang.getList("gui.claims.unclaim_button.lore")) {
                unclaimLore.add(net.kyori.adventure.text.Component.text(line.replace("{refund}", String.valueOf(refund))));
            }
            unMeta.lore(unclaimLore);
            unclaim.setItemMeta(unMeta);
            gui.setItem(51, unclaim);
        }

        gui.setItem(49, createBackItem());
        player.openInventory(gui);
    }

    public void openSettingsGUI(Player player) {
        Clan clan = clanManager.getPlayerClan(player);
        if (clan == null || !clan.hasPermission(player.getUniqueId(), "officer")) {
            lang.send(player, "clan.no_permission");
            return;
        }

        String title = lang.get("gui.titles.settings").replace("{clan}", clan.getName());
        Inventory gui = Bukkit.createInventory(new ClanGuiHolder(GuiType.SETTINGS, clan.getId()), 27, net.kyori.adventure.text.Component.text(title));

        ItemStack desc = new ItemStack(Material.BOOK);
        ItemMeta descMeta = desc.getItemMeta();
        descMeta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.settings.description.name")));

        List<net.kyori.adventure.text.Component> descLore = new ArrayList<>();
        for (String line : lang.getList("gui.settings.description.lore")) {
            descLore.add(net.kyori.adventure.text.Component.text(line.replace("{desc}", clan.getDescription())));
        }
        descMeta.lore(descLore);
        desc.setItemMeta(descMeta);
        gui.setItem(10, desc);

        ItemStack open = new ItemStack(clan.isOpen() ? Material.LIME_DYE : Material.GRAY_DYE);
        ItemMeta openMeta = open.getItemMeta();

        openMeta.displayName(net.kyori.adventure.text.Component.text(clan.isOpen()
                ? lang.get("gui.settings.open.name_open")
                : lang.get("gui.settings.open.name_closed")));

        List<net.kyori.adventure.text.Component> openLore = new ArrayList<>();
        for (String line : lang.getList("gui.settings.open.lore")) {
            openLore.add(net.kyori.adventure.text.Component.text(line.replace("{status}", clan.isOpen()
                    ? lang.get("gui.settings.open.status_open")
                    : lang.get("gui.settings.open.status_closed"))));
        }
        openMeta.lore(openLore);
        open.setItemMeta(openMeta);
        gui.setItem(12, open);

        Object ffObj = clan.getSettings().get("friendlyFire");
        boolean friendlyFire = false;
        if (ffObj instanceof Boolean) {
            friendlyFire = (Boolean) ffObj;
        } else if (ffObj != null) {
            friendlyFire = Boolean.parseBoolean(ffObj.toString());
        }

        ItemStack ff = new ItemStack(friendlyFire ? Material.TNT : Material.SHIELD);
        ItemMeta ffMeta = ff.getItemMeta();

        ffMeta.displayName(net.kyori.adventure.text.Component.text(friendlyFire
                ? lang.get("gui.settings.friendlyfire.name_on")
                : lang.get("gui.settings.friendlyfire.name_off")));

        List<net.kyori.adventure.text.Component> ffLore = new ArrayList<>();
        for (String line : lang.getList("gui.settings.friendlyfire.lore")) {
            ffLore.add(net.kyori.adventure.text.Component.text(line.replace("{state}", friendlyFire
                    ? lang.get("gui.settings.friendlyfire.state_on")
                    : lang.get("gui.settings.friendlyfire.state_off"))));
        }
        ffMeta.lore(ffLore);
        ff.setItemMeta(ffMeta);
        gui.setItem(14, ff);

        ItemStack maxMembers = new ItemStack(Material.PAPER);
        ItemMeta maxMeta = maxMembers.getItemMeta();
        maxMeta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.settings.maxmembers.name")));

        List<net.kyori.adventure.text.Component> maxLore = new ArrayList<>();
        for (String line : lang.getList("gui.settings.maxmembers.lore")) {
            maxLore.add(net.kyori.adventure.text.Component.text(line.replace("{max}", String.valueOf(clan.getMaxMembers()))));
        }
        maxMeta.lore(maxLore);
        maxMembers.setItemMeta(maxMeta);
        gui.setItem(16, maxMembers);

        gui.setItem(22, createBackItem());
        player.openInventory(gui);
    }

    public void openDisbandConfirmGUI(Player player) {
        Clan clan = clanManager.getPlayerClan(player);
        if (clan == null) {
            lang.send(player, "clan.not_in_clan");
            return;
        }
        if (!clan.isLeader(player.getUniqueId())) {
            lang.send(player, "clan.not_leader");
            return;
        }

        String title = lang.get("gui.confirm_disband.title");
        Inventory gui = Bukkit.createInventory(new ClanGuiHolder(GuiType.CONFIRM_DISBAND, clan.getId()), 27, net.kyori.adventure.text.Component.text(title));

        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta fm = filler.getItemMeta();
        fm.displayName(net.kyori.adventure.text.Component.text(" "));
        filler.setItemMeta(fm);
        for (int i = 0; i < 27; i++) gui.setItem(i, filler);

        ItemStack info = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta im = info.getItemMeta();
        im.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.confirm_disband.info.name")));

        List<net.kyori.adventure.text.Component> infoLore = new ArrayList<>();
        for (String line : lang.getList("gui.confirm_disband.info.lore")) {
            infoLore.add(net.kyori.adventure.text.Component.text(line));
        }
        im.lore(infoLore);
        info.setItemMeta(im);
        gui.setItem(13, info);

        ItemStack confirm = new ItemStack(Material.LIME_WOOL);
        ItemMeta cm = confirm.getItemMeta();
        cm.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.confirm_disband.accept.name")));

        List<net.kyori.adventure.text.Component> confirmLore = new ArrayList<>();
        for (String line : lang.getList("gui.confirm_disband.accept.lore")) {
            confirmLore.add(net.kyori.adventure.text.Component.text(line));
        }
        cm.lore(confirmLore);
        confirm.setItemMeta(cm);
        gui.setItem(11, confirm);

        ItemStack cancel = new ItemStack(Material.RED_WOOL);
        ItemMeta xm = cancel.getItemMeta();
        xm.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.confirm_disband.cancel.name")));

        List<net.kyori.adventure.text.Component> cancelLore = new ArrayList<>();
        for (String line : lang.getList("gui.confirm_disband.cancel.lore")) {
            cancelLore.add(net.kyori.adventure.text.Component.text(line));
        }
        xm.lore(cancelLore);
        cancel.setItemMeta(xm);
        gui.setItem(15, cancel);

        player.openInventory(gui);
    }

    private ItemStack createInfoItem(Clan clan, Player player) {
        ItemStack item = new ItemStack(Material.BEACON);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.main.items.info.name")
                .replace("{clan}", clan.getName())
                .replace("{tag}", clan.getTag())));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.main.items.info.lore")) {
            String formatted = line
                    .replace("{leader}", clan.getLeaderName())
                    .replace("{members}", String.valueOf(clan.getMemberCount()))
                    .replace("{max_members}", String.valueOf(clan.getMaxMembers()))
                    .replace("{power}", String.valueOf(clan.getPower()))
                    .replace("{max_power}", String.valueOf(clan.getMaxPower()))
                    .replace("{claims}", String.valueOf(clan.getClaimCount()))
                    .replace("{max_claims}", String.valueOf(clan.getMaxClaims()))
                    .replace("{role}", getPlayerRole(clan, player))
                    .replace("{desc}", clan.getDescription());
            lore.add(net.kyori.adventure.text.Component.text(formatted));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createMembersItem(Clan clan) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.main.items.members.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.main.items.members.lore")) {
            String formatted = line
                    .replace("{total}", String.valueOf(clan.getMemberCount()))
                    .replace("{online}", String.valueOf(getOnlineMembersCount(clan)));
            lore.add(net.kyori.adventure.text.Component.text(formatted));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createClaimsItem(Clan clan) {
        ItemStack item = new ItemStack(Material.GRASS_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.main.items.claims.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.main.items.claims.lore")) {
            String formatted = line
                    .replace("{claims}", String.valueOf(clan.getClaimCount()))
                    .replace("{max}", String.valueOf(clan.getMaxClaims()));
            lore.add(net.kyori.adventure.text.Component.text(formatted));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPowerItem(Clan clan) {
        ItemStack item = new ItemStack(Material.NETHER_STAR);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.main.items.power.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.main.items.power.lore")) {
            String formatted = line
                    .replace("{power}", String.valueOf(clan.getPower()))
                    .replace("{max}", String.valueOf(clan.getMaxPower()));
            lore.add(net.kyori.adventure.text.Component.text(formatted));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createSettingsItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_TORCH);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.main.items.settings.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.main.items.settings.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createChatItem() {
        ItemStack item = new ItemStack(Material.BOOK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.main.items.chat.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.main.items.chat.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTopClansItem() {
        ItemStack item = new ItemStack(Material.GOLD_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.main.items.top.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.main.items.top.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createChestItem() {
        ItemStack item = new ItemStack(Material.CHEST);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.main.items.chest.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.main.items.chest.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createCloseItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.common.close")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBackItem() {
        ItemStack item = new ItemStack(Material.ARROW);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.common.back")));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDisbandItem() {
        ItemStack item = new ItemStack(Material.REDSTONE_BLOCK);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.main.items.disband.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.main.items.disband.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createPromoteItem() {
        ItemStack item = new ItemStack(Material.EMERALD);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.manage_member.promote.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.manage_member.promote.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDemoteItem() {
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.manage_member.demote.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.manage_member.demote.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createTransferItem() {
        ItemStack item = new ItemStack(Material.NETHERITE_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.manage_member.transfer.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.manage_member.transfer.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createKickItem() {
        ItemStack item = new ItemStack(Material.REDSTONE);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.manage_member.kick.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.manage_member.kick.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createBanItem() {
        ItemStack item = new ItemStack(Material.BARRIER);
        ItemMeta meta = item.getItemMeta();
        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.manage_member.ban.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.manage_member.ban.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createDisabledItem(String reasonKey) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(net.kyori.adventure.text.Component.text(lang.get("gui.manage_member.disabled.name")));

        List<net.kyori.adventure.text.Component> lore = new ArrayList<>();
        for (String line : lang.getList("gui.manage_member.disabled.lore")) {
            lore.add(net.kyori.adventure.text.Component.text(line.replace("{reason}", lang.get(reasonKey))));
        }
        meta.lore(lore);

        item.setItemMeta(meta);
        return item;
    }

    private String getPlayerRole(Clan clan, Player player) {
        if (clan.isLeader(player.getUniqueId())) return lang.get("gui.members.roles.leader");
        if (clan.isOfficer(player.getUniqueId())) return lang.get("gui.members.roles.officer");
        return lang.get("gui.members.roles.member");
    }

    private int getOnlineMembersCount(Clan clan) {
        int count = 0;
        for (UUID memberId : clan.getMembers()) {
            Player member = Bukkit.getPlayer(memberId);
            if (member != null && member.isOnline()) count++;
        }
        return count;
    }
}