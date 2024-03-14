package me.youhavetrouble.cerberus;

import com.velocitypowered.api.proxy.Player;
import me.youhavetrouble.cerberus.listeners.DiscordBotListener;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.OnlineStatus;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.annotation.Nullable;
import java.util.List;

public class CerberusDiscordBot extends ListenerAdapter {

    private final Cerberus plugin;
    private final JDA bot;

    protected CerberusDiscordBot(Cerberus plugin) throws InterruptedException {
        this.plugin = plugin;
        bot = JDABuilder.createDefault(plugin.getConfig().botToken)
                .enableIntents(
                        GatewayIntent.GUILD_MEMBERS,
                        GatewayIntent.GUILD_PRESENCES,
                        GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .setAutoReconnect(true)
                .addEventListeners(new DiscordBotListener(plugin))
                .build();
        bot.awaitReady();
        createLinkingCommand();
        sendLinkingMessage();
    }

    /**
     * Checks if a player is on the common Discord server.
     *
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
     *
     * @param user The user to check.
     * @return True if the user is on a common Discord server, false otherwise.
     */
    public boolean isUserOnCommonDiscordServer(@Nullable User user) {
        if (user == null) return false;
        return !bot.getMutualGuilds(user).isEmpty();
    }

    private void createLinkingCommand() {
        bot.getGuilds().forEach(guild -> guild.upsertCommand(
                "link-minecraft",
                plugin.getConfig().linkingCommandDescription).queue()
        );
    }

    public void setStatus(String status) {
        if (status == null) return;
        bot.getPresence().setPresence(OnlineStatus.ONLINE, Activity.customStatus(status));
    }

    private void sendLinkingMessage() {
        TextChannel textChannel = bot.getTextChannelById(plugin.getConfig().discordChannelId);
        if (textChannel == null) return;
        List<Message> messageHistory = textChannel.getHistory().retrievePast(1).complete();

        EmbedBuilder embedBuilder = new EmbedBuilder()
                .setTitle(plugin.getConfig().connectionEmbedTitle)
                .setDescription(plugin.getConfig().connectionEmbedContent)
                .setFooter(getClass().getPackage().getImplementationTitle() + " " + getClass().getPackage().getImplementationVersion());

        if (messageHistory.isEmpty()) {
            textChannel.sendMessageEmbeds(embedBuilder.build()).complete();
        }
        messageHistory = textChannel.getHistory().retrievePast(100).complete();

        for (Message message : messageHistory) {
            if (message.getAuthor().getIdLong() != bot.getSelfUser().getIdLong()) continue;
            message.editMessageEmbeds(embedBuilder.build()).queue();
            message.editMessageComponents().setActionRow(Button.primary(DiscordBotListener.modal.getId(), "Link your account")).queue();
            return;
        }
    }

}
