package org.pablito.clanAscend.listeners;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.objects.Clan;

public class FriendlyFireListener implements Listener {

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player)) return;

        Player victim = (Player) event.getEntity();
        Player attacker = getAttacker(event.getDamager());
        if (attacker == null) return;

        if (attacker.getUniqueId().equals(victim.getUniqueId())) return;

        Clan attackerClan = ClanAscend.getInstance().getClanManager().getPlayerClan(attacker);
        Clan victimClan = ClanAscend.getInstance().getClanManager().getPlayerClan(victim);

        if (attackerClan == null || victimClan == null) return;

        if (attackerClan.getId().equals(victimClan.getId())) {
            Object ffObj = attackerClan.getSettings().get("friendlyFire");
            boolean friendlyFire = false;
            if (ffObj instanceof Boolean) {
                friendlyFire = (Boolean) ffObj;
            } else if (ffObj != null) {
                friendlyFire = Boolean.parseBoolean(ffObj.toString());
            }

            if (!friendlyFire) {
                event.setCancelled(true);
            }
            return;
        }

        boolean disableAllyPvp = ClanAscend.getInstance().getConfig().getBoolean("alliances.disable-pvp-between-allies", true);
        if (disableAllyPvp && ClanAscend.getInstance().getClanManager().isAllied(attackerClan, victimClan)) {
            event.setCancelled(true);
        }
    }

    private Player getAttacker(org.bukkit.entity.Entity damager) {
        if (damager instanceof Player) return (Player) damager;

        if (damager instanceof Projectile) {
            Object shooter = ((Projectile) damager).getShooter();
            if (shooter instanceof Player) return (Player) shooter;
        }
        return null;
    }
}