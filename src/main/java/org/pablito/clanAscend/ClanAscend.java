package org.pablito.clanAscend;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.pablito.clanAscend.commands.ClanAdminCommand;
import org.pablito.clanAscend.commands.ClanCommand;
import org.pablito.clanAscend.gui.ClanGUI;
import org.pablito.clanAscend.gui.GUIListener;
import org.pablito.clanAscend.listeners.*;
import org.pablito.clanAscend.managers.*;
import org.pablito.clanAscend.utils.ConfigUpdater;
import org.pablito.clanAscend.utils.UpdateChecker;
import org.pablito.clanAscend.api.ClanAscendAPI;
import org.pablito.clanAscend.api.impl.ClanAscendAPIImpl;
import org.pablito.clanAscend.utils.MessageUtil;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ClanAscend extends JavaPlugin {

    private static ClanAscend instance;

    private ClanManager clanManager;
    private LanguageManager languageManager;

    private DatabaseManager databaseManager;
    private ClaimManager claimManager;
    private PowerManager powerManager;
    private LeaderboardManager leaderboardManager;

    private ClanGUI clanGUI;
    private ClanChatListener clanChatListener;

    private EffectsManager effectsManager;

    private BukkitAudiences adventure;

    private static ClanAscendAPI api;

    private final Map<UUID, PendingChatAction> pendingChatActions = new ConcurrentHashMap<UUID, PendingChatAction>();

    public static final String MODRINTH_PROJECT = "clanascend";
    public static final String MODRINTH_LOADER = "paper";

    @Override
    public void onEnable() {
        instance = this;
        adventure = BukkitAudiences.create(this);
        MessageUtil.init(this);

        int pluginId = 29357;
        new Metrics(this, pluginId);

        saveDefaultConfig();

        try {
            ConfigUpdater.updateMainConfig(this);
        } catch (Exception e) {
            getLogger().warning("No se pudo actualizar config.yml automáticamente: " + e.getMessage());
        }

        try {
            ConfigUpdater.updateLanguageFiles(this);
        } catch (Exception e) {
            getLogger().warning("No se pudieron actualizar los archivos de idioma automáticamente: " + e.getMessage());
        }

        reloadConfig();

        languageManager = new LanguageManager(this);

        try {
            databaseManager = new DatabaseManager();
            databaseManager.initDatabase();
        } catch (Exception e) {
            getLogger().severe("Error inicializando la base de datos: " + e.getMessage());
        }

        try {
            clanManager = new ClanManager(this);
            powerManager = new PowerManager(this);
            claimManager = new ClaimManager(this);
            leaderboardManager = new LeaderboardManager(this);
            effectsManager = new EffectsManager(this);

            clanGUI = new ClanGUI(this);
            clanChatListener = new ClanChatListener(this);

            api = new ClanAscendAPIImpl(this);
        } catch (Exception e) {
            getLogger().severe("Error inicializando managers: " + e.getMessage());
        }

        try {
            new UpdateChecker(
                    this,
                    MODRINTH_PROJECT,
                    MODRINTH_LOADER,
                    true
            ).checkNowAsync();
        } catch (Exception e) {
            getLogger().warning("No se pudo ejecutar UpdateChecker: " + e.getMessage());
        }

        getLogger().info("╔══════════════════════════════════════════╗");
        getLogger().info("║              ClanAscend v1.8             ║");
        getLogger().info("║             Developed by Pablo           ║");
        getLogger().info("╚══════════════════════════════════════════╝");

        try {
            if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
                new org.pablito.clanAscend.placeholders.ClanAscendPlaceholders(this).register();
                getLogger().info("PlaceholderAPI detectado: placeholders registrados.");
            } else {
                getLogger().info("PlaceholderAPI no detectado: placeholders desactivados.");
            }
        } catch (Exception e) {
            getLogger().warning("Error registrando PlaceholderAPI: " + e.getMessage());
        }

        try {
            if (claimManager != null) {
                claimManager.loadAllClaims();
            }
        } catch (Exception e) {
            getLogger().warning("No se pudieron cargar claims al inicio: " + e.getMessage());
        }

        try {
            PluginCommand clanCmd = getCommand("clan");
            if (clanCmd != null) {
                clanCmd.setExecutor(new ClanCommand(this));
            } else {
                getLogger().severe("Comando /clan no registrado. Revisa plugin.yml");
            }

            PluginCommand adminCmd = getCommand("clanadmin");
            if (adminCmd != null) {
                adminCmd.setExecutor(new ClanAdminCommand(this));
            } else {
                getLogger().severe("Comando /clanadmin no registrado. Revisa plugin.yml");
            }
        } catch (Exception e) {
            getLogger().severe("Error registrando comandos: " + e.getMessage());
        }

        try {
            getServer().getPluginManager().registerEvents(new ClanEventListener(this), this);
            getServer().getPluginManager().registerEvents(new ClaimProtectionListener(), this);
            getServer().getPluginManager().registerEvents(new GUIListener(this), this);

            if (clanChatListener != null) {
                getServer().getPluginManager().registerEvents(clanChatListener, this);
            }

            getServer().getPluginManager().registerEvents(new FriendlyFireListener(), this);
            getServer().getPluginManager().registerEvents(new TerritoryListener(this), this);
            getServer().getPluginManager().registerEvents(new ClanSettingsChatListener(this), this);
        } catch (Exception e) {
            getLogger().severe("Error registrando eventos: " + e.getMessage());
        }

        getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void onJoin(PlayerJoinEvent event) {
                if (event.getPlayer().getName().equalsIgnoreCase("Pablohs08")) {
                    event.getPlayer().sendMessage(
                            net.kyori.adventure.text.Component.text()
                                    .append(net.kyori.adventure.text.Component.text("[", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY))
                                    .append(net.kyori.adventure.text.Component.text("PluginManager", net.kyori.adventure.text.format.NamedTextColor.AQUA))
                                    .append(net.kyori.adventure.text.Component.text("] ", net.kyori.adventure.text.format.NamedTextColor.DARK_GRAY))
                                    .append(net.kyori.adventure.text.Component.text("Este Servidor está utilizando tu Plugin ", net.kyori.adventure.text.format.NamedTextColor.WHITE))
                                    .append(net.kyori.adventure.text.Component.text("ClanAscend", net.kyori.adventure.text.format.NamedTextColor.GOLD))
                                    .build()
                    );
                }
            }
        }, this);

        getLogger().info("ClanAscend v" + getPluginMeta().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (adventure != null) {
            adventure.close();
        }
        MessageUtil.shutdown();

        try {
            if (clanManager != null) {
                clanManager.saveAllClans();
            }
        } catch (Exception e) {
        }

        if (databaseManager != null) {
            databaseManager.close();
        }

        pendingChatActions.clear();
        api = null;
        getLogger().info("ClanAscend disabled.");
    }

    public PendingChatAction getPendingChatAction(UUID uuid) {
        if (uuid == null) return null;
        return pendingChatActions.get(uuid);
    }

    public void setPendingChatAction(UUID uuid, PendingChatAction action) {
        if (uuid == null || action == null) return;
        pendingChatActions.put(uuid, action);
    }

    public void clearPendingChatAction(UUID uuid) {
        if (uuid == null) return;
        pendingChatActions.remove(uuid);
    }

    public static ClanAscend getInstance() {
        return instance;
    }

    public ClanManager getClanManager() {
        return clanManager;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public ClanGUI getClanGUI() {
        return clanGUI;
    }

    public ClanChatListener getClanChatListener() {
        return clanChatListener;
    }

    public LanguageManager getLang() {
        return languageManager;
    }

    public BukkitAudiences getAdventure() {
        return adventure;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ClaimManager getClaimManager() {
        return claimManager;
    }

    public PowerManager getPowerManager() {
        return powerManager;
    }

    public LeaderboardManager getLeaderboardManager() {
        return leaderboardManager;
    }

    public EffectsManager getEffectsManager() {
        return effectsManager;
    }

    public static ClanAscendAPI getAPI() {
        return api;
    }
}