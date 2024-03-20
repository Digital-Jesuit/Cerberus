package me.youhavetrouble.cerberus;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import me.youhavetrouble.cerberus.commands.CerberusCommand;
import me.youhavetrouble.cerberus.listeners.JoinAttemptListener;
import me.youhavetrouble.cerberus.storage.Database;
import me.youhavetrouble.cerberus.storage.MysqlDatabase;
import me.youhavetrouble.cerberus.storage.SqliteDatabase;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

@Plugin(
        id = "cerberus",
        name = "Cerberus",
        version = "1.0",
        description = "Plugin managing access to the server via discord",
        authors = {"YouHaveTrouble"}
)
public class Cerberus {

    @Inject
    private Logger logger;
    private final ProxyServer server;
    private CerberusDiscordBot discordBot;
    private Database database;
    private final Path configFolder;
    private CerberusConfig config;

    private ConnectionManager connectionManager;
    @Inject
    public Cerberus(ProxyServer server, @DataDirectory final Path configFolder) {
        this.server = server;
        this.configFolder = configFolder;
        server.getCommandManager().register(CerberusCommand.createCerberusCommand(this));
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) throws SQLException {
        reloadPlugin();
        String databaseType = config.databaseType;
        try {
            switch (databaseType) {
                case "sqlite":
                    this.database = new SqliteDatabase(logger);
                    break;
                case "mysql":
                    this.database = new MysqlDatabase(
                            logger,
                            config.databaseHost,
                            config.databasePort,
                            config.database,
                            config.databaseUser,
                            config.databasePassword,
                            config.databaseSsl,
                            config.databaseVerifyCertificate
                    );
                    break;
                default:
                    logger.error("Failed to recognize database type %s, defaulting to sqlite.".formatted(databaseType));
                    this.database = new SqliteDatabase(logger);
            }
        } catch (Exception e) {
            logger.error("Failed to connect to %s database, defaulting to sqlite.".formatted(databaseType));
            this.database = new SqliteDatabase(logger);
        }

        initDiscordBot();
        discordBot.setStatus(config.status);
        this.connectionManager = new ConnectionManager(this);
        server.getEventManager().register(this, new JoinAttemptListener(this));
    }

    public boolean canJoin(Player player) {
        UUID uuid = player.getUniqueId();
        if (!connectionManager.isConnected(uuid)) return false;
        return discordBot.isPlayerOnCommonDiscordServer(player);
    }

    @Nullable
    public Database getDatabase() {
        return database;
    }

    @Nullable
    public ConnectionManager getConnectionManager() {
        return this.connectionManager;
    }

    private void initDiscordBot() {
        try {
            discordBot = new CerberusDiscordBot(this);
        } catch (InterruptedException e) {
            discordBot = null;
            logger.error("Failed to connect to discord bot.");
        }
    }

    public void reloadPlugin() {
        File folder = configFolder.toFile();
        File file = new File(folder, "config.toml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }
        if (!file.exists()) {
            try (InputStream input = getClass().getResourceAsStream("/" + file.getName())) {
                if (input != null) {
                    Files.copy(input, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                return;
            }
        }
        config = new CerberusConfig(new Toml().read(file));
        if (discordBot == null) return;
        discordBot.setStatus(config.status);
        discordBot.createCommands();
        discordBot.sendLinkingMessage();
    }

    public CerberusConfig getConfig() {
        return config;
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getServer() {
        return server;
    }

    public CerberusDiscordBot getDiscordBot() {
        return discordBot;
    }
}
