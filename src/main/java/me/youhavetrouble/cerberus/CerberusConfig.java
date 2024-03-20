package me.youhavetrouble.cerberus;

import com.moandjiezana.toml.Toml;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public class CerberusConfig {

    protected final String botToken, status, databaseType, databaseHost, databaseUser, databasePassword,
            linkingCommandDescription;
    public final String linkingKickReason, otherErrorDiscord, invalidCode, duplicateMinecraftId, duplicateDiscordId,
            accountConnected, alreadyConnected, connectionEmbedTitle, connectionEmbedContent, minecraftNotConnected;
    protected final Long databasePort, discordChannelId;

    public final Component otherErrorMinecraft ;

    protected CerberusConfig(Toml config) {
        Toml discord = config.getTable("Discord");
        botToken = discord.getString("token");
        discordChannelId = discord.getLong("connection-channel-id");
        status = discord.getString("status");

        Toml database = config.getTable("Database");
        databaseType = database.getString("type", "sqlite");
        databaseHost = database.getString("host", "localhost");
        databaseUser = database.getString("user", "cerberus");
        databasePassword = database.getString("password", "cerberuspassword");
        databasePort = database.getLong("port", 3306L);

        Toml messages = config.getTable("Messages");
        String linkingCommandDescription = messages.getString("linking-command-description", "Allows to link discord account to minecraft one");
        // trim description to 100 characters because of the discord api limit
        this.linkingCommandDescription = linkingCommandDescription.substring(0, Math.min(linkingCommandDescription.length() -1, 100));
        otherErrorMinecraft = MiniMessage.miniMessage().deserialize(messages.getString("error-other", "Error occured. Try again later"));
        linkingKickReason = messages.getString("linking-kick-reason", "Tell your server's administrator to configure this message");
        otherErrorDiscord = messages.getString("error-other-discord", "Error occured. Try again later");
        invalidCode = messages.getString("invalid-code", "Invalid code!");
        duplicateMinecraftId = messages.getString("duplicate-minecraft-id", "Minecraft account already connected!");
        duplicateDiscordId = messages.getString("duplicate-discord-id", "This discord account is already connected to another account!");
        accountConnected = messages.getString("account-connected", "Account connected! Now you can log in and play!");
        alreadyConnected = messages.getString("already-connected", "Your minecraft account is already connected!");
        connectionEmbedTitle = messages.getString("connection-embed-title", "Connect your minecraft account");
        connectionEmbedContent = messages.getString("connection-embed-content", "Try to join the server to get the linking code.\n Click the button below to enter it.");
        minecraftNotConnected = messages.getString("minecraft-account-not-connected", "User doesn't have minecraft account connected");
    }

}
