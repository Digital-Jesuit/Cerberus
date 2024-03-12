package me.youhavetrouble.cerberus;

import com.velocitypowered.api.proxy.Player;
import me.youhavetrouble.cerberus.listeners.DiscordBotListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.annotation.Nullable;

public class CerberusDiscordBot extends ListenerAdapter {

    private final Cerberus plugin;
    private final JDA bot;

    protected CerberusDiscordBot(Cerberus plugin) throws InterruptedException {
        this.plugin = plugin;
        bot =  JDABuilder.createDefault(plugin.getDiscordToken())
                .enableIntents(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_PRESENCES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.GUILD_MESSAGES
                )
                .setAutoReconnect(true)
                .addEventListeners(new DiscordBotListener(plugin))
                .build();
        bot.getPresence().setPresence(OnlineStatus.ONLINE, Activity.customStatus("Gatekeeping"));
        bot.awaitReady();
        createLinkingCommand();
    }

    /**
     * Checks if a player is on the common Discord server.
     * @param player The player to check.
     * @return true if the player is on the common Discord server, false otherwise.
     */
    public boolean isPlayerOnCommonDiscordServer(Player player) {
        if (plugin.getDatabase() == null) return false;
        Long snowflake = plugin.getDatabase().getDiscordSnowflakeByMinecraftUuid(player.getUniqueId());
        if (snowflake == null) return false;
        User user = bot.getUserById(snowflake);
        return isUserOnCommonDiscordServer(user);
    }

    /**
     * Checks if a user is on a common Discord server.
     * @param user The user to check.
     * @return True if the user is on a common Discord server, false otherwise.
     */
    public boolean isUserOnCommonDiscordServer(@Nullable User user) {
        if (user == null) return false;
        return !bot.getMutualGuilds(user).isEmpty();
    }

    public void createLinkingCommand() {
        bot.getGuilds().forEach(guild -> guild.upsertCommand(
                "link-minecraft",
                "Allows to link discord account to minecraft one").queue()
        );
    }

}
