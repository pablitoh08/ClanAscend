package org.pablito.clanAscend.managers;

import org.pablito.clanAscend.ClanAscend;
import org.pablito.clanAscend.objects.Clan;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Duration;

public class EffectsManager {

    private final ClanAscend plugin;
    private final LanguageManager lang;

    public EffectsManager(ClanAscend plugin) {
        this.plugin = plugin;
        this.lang = plugin.getLanguageManager();
    }

    public void showClaimEffect(Location center, @SuppressWarnings("unused") Clan clan) {
        if (!plugin.getConfig().getBoolean("effects.claim_particles", true)) {
            return;
        }

        World world = center.getWorld();
        if (world == null) return;

        new BukkitRunnable() {
            final double radius = 8.0;
            double angle = 0;
            int height = 1;

            @Override
            public void run() {
                for (int i = 0; i < 4; i++) {
                    double x = center.getX() + (radius * Math.cos(angle));
                    double z = center.getZ() + (radius * Math.sin(angle));
                    double y = center.getY() + height;

                    Location particleLoc = new Location(world, x, y, z);

                    world.spawnParticle(Particle.CRIT, particleLoc, 1);

                    angle += Math.PI / 32;
                    if (angle >= 2 * Math.PI) {
                        angle = 0;
                        height++;
                        if (height > 3) {
                            this.cancel();
                            return;
                        }
                    }
                }
            }
        }.runTaskTimer(plugin, 0L, 1L);
    }

    public void showPowerGainEffect(Player player, int amount) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) return;

        for (int i = 0; i < 10; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 2;

            Location particleLoc = location.clone().add(offsetX, offsetY, offsetZ);
            world.spawnParticle(Particle.HEART, particleLoc, 1);
        }

        String titleRaw = lang.getRaw("power.gain_title");
        String subtitleRaw = lang.getRaw("power.gain_subtitle");

        String title = titleRaw != null ? titleRaw.replace("{amount}", String.valueOf(amount)) : "&a+{amount} Power";
        String subtitle = subtitleRaw != null ? subtitleRaw : "&7Power gained!";

        title = title.replace("&", "§");
        subtitle = subtitle.replace("&", "§");

        player.showTitle(
                net.kyori.adventure.title.Title.title(
                        net.kyori.adventure.text.Component.text(title),
                        net.kyori.adventure.text.Component.text(subtitle),
                        net.kyori.adventure.title.Title.Times.times(
                                Duration.ofMillis(500),
                                Duration.ofMillis(2000),
                                Duration.ofMillis(500)
                        )
                )
        );
    }

    public void showPowerLossEffect(Player player, int amount) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) return;

        for (int i = 0; i < 10; i++) {
            double offsetX = (Math.random() - 0.5) * 2;
            double offsetY = Math.random() * 2;
            double offsetZ = (Math.random() - 0.5) * 2;

            Location particleLoc = location.clone().add(offsetX, offsetY, offsetZ);
            world.spawnParticle(Particle.CLOUD, particleLoc, 1);
        }

        String titleRaw = lang.getRaw("power.loss_title");
        String subtitleRaw = lang.getRaw("power.loss_subtitle");

        String title = titleRaw != null ? titleRaw.replace("{amount}", String.valueOf(amount)) : "&c-{amount} Power";
        String subtitle = subtitleRaw != null ? subtitleRaw : "&7Power lost!";

        title = title.replace("&", "§");
        subtitle = subtitle.replace("&", "§");

        player.showTitle(
                net.kyori.adventure.title.Title.title(
                        net.kyori.adventure.text.Component.text(title),
                        net.kyori.adventure.text.Component.text(subtitle),
                        net.kyori.adventure.title.Title.Times.times(
                                Duration.ofMillis(500),
                                Duration.ofMillis(2000),
                                Duration.ofMillis(500)
                        )
                )
        );
    }

    @SuppressWarnings("unused")
    public void showLevelUpEffect(Player player, Clan clan, int newLevel) {
        Location location = player.getLocation();
        World world = location.getWorld();
        if (world == null) return;

        world.spawnParticle(Particle.FIREWORK, location, 30, 0.5, 0.5, 0.5, 0);

        try {
            world.playSound(location, Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
        } catch (Exception e) {
            world.playSound(location, Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        }

        String titleRaw = lang.getRaw("power.levelup_title");
        String subtitleRaw = lang.getRaw("power.levelup_subtitle");

        String title = titleRaw != null ? titleRaw.replace("{level}", String.valueOf(newLevel)) : "&aLevel Up!";
        String subtitle = subtitleRaw != null ? subtitleRaw : "&7Clan reached level {level}";
        subtitle = subtitle.replace("{level}", String.valueOf(newLevel));

        // Reemplazar colores
        title = title.replace("&", "§");
        subtitle = subtitle.replace("&", "§");

        player.showTitle(
                net.kyori.adventure.title.Title.title(
                        net.kyori.adventure.text.Component.text(title),
                        net.kyori.adventure.text.Component.text(subtitle),
                        net.kyori.adventure.title.Title.Times.times(
                                Duration.ofMillis(500),
                                Duration.ofMillis(2000),
                                Duration.ofMillis(500)
                        )
                )
        );
    }

    @SuppressWarnings("unused")
    public void playJoinSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_NOTE_BLOCK_PLING, 1.0f, 1.5f);
        } catch (Exception e) {
            // Ignorar si no existe el sonido
        }
    }

    public void playClaimSound(Player player) {
        try {
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_PLACE, 0.5f, 1.0f);
        } catch (Exception e) {
            player.playSound(player.getLocation(), Sound.BLOCK_STONE_BREAK, 0.5f, 1.0f);
        }
    }
}