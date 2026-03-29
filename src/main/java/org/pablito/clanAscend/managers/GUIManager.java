package org.pablito.clanAscend.managers;

import org.pablito.clanAscend.ClanAscend;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

public class GUIManager implements Listener {

    private static final String TITLE = "ClanAscend - Menú Principal";
    private final ClanAscend plugin;

    public GUIManager(ClanAscend plugin) {
        this.plugin = plugin;
    }

    @SuppressWarnings("unused")
    public void openClanMenu(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, net.kyori.adventure.text.Component.text(TITLE));

        gui.setItem(10, item(Material.PAPER, "§6Información del Clan",
                "§7Click para ver información", "§7detallada de tu clan"));

        gui.setItem(12, item(Material.PLAYER_HEAD, "§aMiembros del Clan",
                "§7Click para ver la lista de", "§7miembros de tu clan"));

        gui.setItem(14, item(Material.GRASS_BLOCK, "§2Territorio del Clan",
                "§7Click para abrir la GUI", "§7de territorio del clan"));

        gui.setItem(16, item(Material.GOLD_BLOCK, "§eTop de Clanes",
                "§7Click para ver el ranking", "§7de los mejores clanes"));

        gui.setItem(30, item(Material.REDSTONE_TORCH, "§dConfiguración del Clan",
                "§7Click para configurar", "§7tu clan (líder/oficial)"));

        gui.setItem(32, item(Material.BOOK, "§bChat del Clan",
                "§7Click para alternar", "§7chat del clan"));

        gui.setItem(34, item(Material.BARRIER, "§cSalir del Clan",
                "§7Click para abandonar", "§7tu clan actual"));

        gui.setItem(49, item(Material.ARROW, "§aCerrar", "§7Click para cerrar"));

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();

        if (!event.getView().title().equals(net.kyori.adventure.text.Component.text(TITLE))) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;

        Material type = event.getCurrentItem().getType();

        switch (type) {
            case PAPER:
                player.closeInventory();
                player.performCommand("clan info");
                break;

            case PLAYER_HEAD:
                player.closeInventory();
                player.performCommand("clan members");
                break;

            case GRASS_BLOCK:
                player.closeInventory();
                player.performCommand("clan gui");
                break;

            case GOLD_BLOCK:
                player.closeInventory();
                player.performCommand("clan top");
                break;

            case REDSTONE_TORCH:
                player.closeInventory();
                player.performCommand("clan settings");
                break;

            case BOOK:
                player.closeInventory();
                player.performCommand("clan chat");
                break;

            case BARRIER:
                player.closeInventory();
                player.performCommand("clan leave");
                break;

            case ARROW:
                player.closeInventory();
                break;

            default:
                break;
        }
    }

    private ItemStack item(Material mat, String name, String... lore) {
        ItemStack item = new ItemStack(mat);
        ItemMeta meta = item.getItemMeta();

        meta.displayName(net.kyori.adventure.text.Component.text(name));

        java.util.List<net.kyori.adventure.text.Component> loreComponents = new java.util.ArrayList<>();
        for (String line : lore) {
            loreComponents.add(net.kyori.adventure.text.Component.text(line));
        }
        meta.lore(loreComponents);

        item.setItemMeta(meta);
        return item;
    }
}