package me.youhavetrouble.cerberus;

import com.velocitypowered.api.proxy.Player;
import me.youhavetrouble.cerberus.listeners.DiscordBotListener;
import net.dv8tion.jda.api.*;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.requests.GatewayIntent;

import javax.annotation.Nullable;
import java.util.List;
import java.util.UUID;

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
        createCommands();
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

    @Nullable
    public User getUser(@Nullable UUID minecraftId) {
        if (plugin.getDatabase() == null) return null;
        if (minecraftId == null) return null;
        Long discordId = plugin.getDatabase().getDiscordSnowflakeByMinecraftUuid(minecraftId);
        if (discordId == null) return null;
        return bot.getUserById(discordId);
    }

    public void createCommands() {
        bot.getGuilds().forEach(guild -> guild.updateCommands().addCommands(
                Commands.slash("link-minecraft", plugin.getConfig().linkingCommandDescription)
                        .setGuildOnly(true),
                Commands.context(Command.Type.USER, "Get connected minecraft account")
                        .setDefaultPermissions(DefaultMemberPermissions.DISABLED)
        ).queue());
    }

    public void setStatus(String status) {
        if (status == null) return;
        bot.getPresence().setPresence(OnlineStatus.ONLINE, Activity.customStatus(status));
    }

    public void sendLinkingMessage() {
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
