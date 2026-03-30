package org.pablito.clanAscend.gui;

import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.listeners.PendingChatAction;
import org.pablito.clanAscend.managers.ClanManager;
import org.pablito.clanAscend.managers.LanguageManager;
import org.pablito.clanAscend.objects.Clan;

import java.util.UUID;

public class GUIListener implements Listener {

    private final ClanAscend plugin;
    private final ClanManager clanManager;
    private final ClanGUI clanGUI;
    private final LanguageManager lang;

    private final NamespacedKey memberUuidKey;

    public GUIListener(ClanAscend plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
        this.clanGUI = plugin.getClanGUI();
        this.lang = plugin.getLanguageManager(); // CORREGIDO
        this.memberUuidKey = new NamespacedKey(plugin, "member_uuid");
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (!(holder instanceof ClanGUI.ClanGuiHolder)) return;

        ClanGUI.ClanGuiHolder h = (ClanGUI.ClanGuiHolder) holder;
        if (h.getType() != ClanGUI.GuiType.CLAN_CHEST) {
            event.setCancelled(true);
        }

        int slot = event.getRawSlot();
        if (slot < 0 || slot >= inv.getSize()) return;

        Player player = (Player) event.getWhoClicked();

        Clan clan = clanManager.getPlayerClan(player);
        if (clan == null) {
            player.closeInventory();
            return;
        }

        switch (h.getType()) {
            case MAIN:
                handleMain(player, clan, slot);
                break;

            case MEMBERS:
                handleMembers(player, clan, slot, event.getCurrentItem());
                break;

            case MEMBER_MANAGE:
                handleMemberManage(player, clan, h, slot);
                break;

            case CLAIMS:
                handleClaims(player, clan, slot);
                break;

            case SETTINGS:
                handleSettings(player, clan, slot);
                break;

            case CLAN_CHEST:
                break;

            case CONFIRM_DISBAND:
                handleConfirmDisband(player, clan, slot);
                break;

            default:
                break;
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        Inventory inv = event.getInventory();
        InventoryHolder holder = inv.getHolder();
        if (!(holder instanceof ClanGUI.ClanGuiHolder)) return;

        ClanGUI.ClanGuiHolder h = (ClanGUI.ClanGuiHolder) holder;
        if (h.getType() != ClanGUI.GuiType.CLAN_CHEST) return;

        Clan clan = clanManager.getClan(h.getClanId());
        if (clan == null) return;

        String base64 = org.pablito.clanAscend.utils.InventorySerializer.itemStackArrayToBase64(inv.getContents());
        clan.getSettings().put("clanChest", base64);
        clanManager.saveClan(clan);
    }

    private void handleMain(Player player, Clan clan, int slot) {
        switch (slot) {
            case 20:
                clanGUI.openMembersGUI(player);
                break;

            case 22:
                if (plugin.getConfig().getBoolean("claims.enabled", true)) {
                    clanGUI.openClaimsGUI(player);
                }
                break;

            case 24:
                // Show power info
                lang.send(player, "gui.main.power_chat",
                        lang.placeholders(
                                "power", String.valueOf(clan.getPower()),
                                "max", String.valueOf(clan.getMaxPower())
                        ));
                player.closeInventory();
                break;

            case 31:
                if (clan.hasPermission(player.getUniqueId(), "officer")) {
                    clanGUI.openSettingsGUI(player);
                }
                break;

            case 33:
                clanGUI.openClanChestGUI(player);
                break;

            case 40:
                if (plugin.getClanChatListener() == null) {
                    lang.send(player, "error.chat_not_available");
                    return;
                }
                boolean enabled = plugin.getClanChatListener().toggleClanChat(player);
                lang.send(player, enabled ? "chat.toggled_on" : "chat.toggled_off");
                break;

            case 42:
                if (plugin.getLeaderboardManager() != null) {
                    plugin.getLeaderboardManager().displayTopClans(player);
                }
                player.closeInventory();
                break;

            case 49:
                player.closeInventory();
                break;

            case 53:
                if (clan.isLeader(player.getUniqueId())) {
                    clanGUI.openDisbandConfirmGUI(player);
                }
                break;

            default:
                break;
        }
    }

    private void handleMembers(Player player, Clan clan, int slot, ItemStack clicked) {
        if (slot == 49) {
            clanGUI.openClanGUI(player);
            return;
        }

        if (slot >= 0 && slot <= 44) {
            if (!clan.hasPermission(player.getUniqueId(), "officer")) return;
            if (clicked == null || clicked.getType().isAir()) return;

            ItemMeta meta = clicked.getItemMeta();
            if (meta == null) return;

            String uuidStr = meta.getPersistentDataContainer().get(memberUuidKey, PersistentDataType.STRING);
            if (uuidStr == null || uuidStr.isEmpty()) return;

            try {
                UUID targetId = UUID.fromString(uuidStr);
                clanGUI.openMemberManageGUI(player, targetId);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    private void handleMemberManage(Player player, Clan clan, ClanGUI.ClanGuiHolder holder, int slot) {
        String targetUuidStr = holder.getTargetUuid();
        if (targetUuidStr == null) {
            player.closeInventory();
            return;
        }

        UUID targetId;
        try {
            targetId = UUID.fromString(targetUuidStr);
        } catch (IllegalArgumentException e) {
            player.closeInventory();
            return;
        }

        if (slot == 18) {
            clanGUI.openMembersGUI(player);
            return;
        }

        boolean isLeader = clan.isLeader(player.getUniqueId());

        // Handle promote/demote (slot 10)
        if (slot == 10 && isLeader) {
            if (!clan.isLeader(targetId) && !player.getUniqueId().equals(targetId)) {
                if (clan.isOfficer(targetId)) {
                    clan.removeOfficer(targetId);
                    lang.send(player, "clan.member_demoted");
                } else {
                    if (clan.isMember(targetId)) {
                        clan.addOfficer(targetId);
                        lang.send(player, "clan.member_promoted");
                    }
                }
                clanManager.saveClan(clan);
            }
            clanGUI.openMemberManageGUI(player, targetId);
            return;
        }

        // Handle transfer leadership (slot 16)
        if (slot == 16 && isLeader) {
            if (!clan.isLeader(targetId) && !player.getUniqueId().equals(targetId) && clan.isMember(targetId)) {
                UUID oldLeader = clan.getLeader();
                clan.setLeader(targetId);
                clan.removeOfficer(targetId);
                clan.addOfficer(oldLeader);
                clanManager.saveClan(clan);

                lang.send(player, "clan.leadership_transferred");
                clanGUI.openClanGUI(player);
            }
            return;
        }

        // Handle kick (slot 22)
        if (slot == 22 && clan.hasPermission(player.getUniqueId(), "officer")) {
            if (!clan.isLeader(targetId) && !player.getUniqueId().equals(targetId) && clan.isMember(targetId)) {
                clan.removeMember(targetId);

                Player target = plugin.getServer().getPlayer(targetId);
                if (target != null && target.isOnline()) {
                    lang.send(target, "clan.player_kicked_clan",
                            lang.placeholders(
                                    "player", target.getName(),
                                    "by", player.getName()
                            ));
                }

                clanManager.saveClan(clan);
                clanGUI.openMembersGUI(player);
            }
            return;
        }

        // Handle ban (slot 24)
        if (slot == 24 && clan.hasPermission(player.getUniqueId(), "officer")) {
            if (!clan.isLeader(targetId) && !player.getUniqueId().equals(targetId) && clan.isMember(targetId)) {
                clan.ban(targetId);
                clan.removeMember(targetId);

                Player target = plugin.getServer().getPlayer(targetId);
                if (target != null && target.isOnline()) {
                    lang.send(target, "clan.player_kicked_clan",
                            lang.placeholders(
                                    "player", target.getName(),
                                    "by", player.getName()
                            ));
                }

                clanManager.saveClan(clan);
                clanGUI.openMembersGUI(player);
            }
        }
    }

    private void handleClaims(Player player, Clan clan, int slot) {
        switch (slot) {
            case 47:
                if (clan.hasPermission(player.getUniqueId(), "officer")) {
                    player.closeInventory();
                    clanManager.claimChunk(player);
                }
                break;

            case 49:
                clanGUI.openClanGUI(player);
                break;

            case 51:
                if (clan.hasPermission(player.getUniqueId(), "officer")) {
                    player.closeInventory();
                    clanManager.unclaimChunk(player);
                }
                break;

            default:
                break;
        }
    }

    private void handleSettings(Player player, Clan clan, int slot) {
        switch (slot) {
            case 10:
                player.closeInventory();
                plugin.setPendingChatAction(player.getUniqueId(), PendingChatAction.SET_DESCRIPTION);
                lang.send(player, "gui.prompts.new_description");
                lang.send(player, "gui.prompts.cancel_hint");
                break;

            case 12:
                clan.setOpen(!clan.isOpen());
                clanManager.saveClan(clan);
                clanGUI.openSettingsGUI(player);

                String state = clan.isOpen()
                        ? lang.getRaw("gui.settings.open.status_open")
                        : lang.getRaw("gui.settings.open.status_closed");
                lang.send(player, "clan.toggled_open",
                        lang.placeholders("state", state));
                break;

            case 14:
                Object ffObj = clan.getSettings().get("friendlyFire");
                boolean current = false;
                if (ffObj instanceof Boolean) {
                    current = ((Boolean) ffObj).booleanValue();
                } else if (ffObj != null) {
                    current = Boolean.parseBoolean(ffObj.toString());
                }

                clan.getSettings().put("friendlyFire", !current);
                clanManager.saveClan(clan);
                clanGUI.openSettingsGUI(player);

                if (!current) {
                    lang.send(player, "clan.friendly_fire_enabled");
                } else {
                    lang.send(player, "clan.friendly_fire_disabled");
                }
                break;

            case 16:
                player.closeInventory();
                plugin.setPendingChatAction(player.getUniqueId(), PendingChatAction.SET_MAX_MEMBERS);
                lang.send(player, "gui.prompts.new_maxmembers");
                lang.send(player, "gui.prompts.cancel_hint");
                break;

            case 22:
                clanGUI.openClanGUI(player);
                break;

            default:
                break;
        }
    }

    private void handleConfirmDisband(Player player, Clan clan, int slot) {
        switch (slot) {
            case 11:
                player.closeInventory();
                clanManager.disbandClan(clan, player.getName());
                break;

            case 15:
                clanGUI.openClanGUI(player);
                break;

            default:
                break;
        }
    }
}