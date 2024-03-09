package me.youhavetrouble.bouncer;

import com.velocitypowered.api.proxy.Player;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.Nullable;

public class CerberusDiscordBot {

    private final Cerberus plugin;
    private final JDA bot;

    protected CerberusDiscordBot(Cerberus plugin, String token) throws InterruptedException {
        this.plugin = plugin;
        JDABuilder builder = JDABuilder.createDefault(token);
        builder.disableCache(CacheFlag.STICKER, CacheFlag.VOICE_STATE, CacheFlag.SCHEDULED_EVENTS, CacheFlag.FORUM_TAGS);
        bot = builder.build();
        bot.setAutoReconnect(true);
        bot.getPresence().setPresence(OnlineStatus.ONLINE, Activity.customStatus("Gatekeeping"));
        bot.awaitReady();
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

}
