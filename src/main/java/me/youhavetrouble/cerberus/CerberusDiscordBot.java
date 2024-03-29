package me.youhavetrouble.cerberus;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
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
import java.util.concurrent.TimeUnit;

public class CerberusDiscordBot extends ListenerAdapter {

    private final Cerberus plugin;
    private final JDA bot;

    private final Cache<Long, Byte> interactionCooldown = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.SECONDS)
            .build();

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

    protected void setInteractionCooldown(long snowflake) {
        this.interactionCooldown.put(snowflake, (byte) 0);
    }

    protected boolean isUserOnCooldown(long snowflake) {
        return this.interactionCooldown.getIfPresent(snowflake) != null;
    }

    /**
     * Checks if a user is on cooldown and sets the interaction cooldown if not.
     * @param snowflake the ID of the user (snowflake) to check cooldown for
     * @return true if the user is on cooldown, false otherwise
     */
    public boolean checkAndSetCooldown(long snowflake) {
        if (isUserOnCooldown(snowflake)) return true;
        setInteractionCooldown(snowflake);
        return false;
    }

}
