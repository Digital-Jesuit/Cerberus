package me.youhavetrouble.cerberus.listeners;

import me.youhavetrouble.cerberus.Cerberus;
import me.youhavetrouble.cerberus.ConnectionManager;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class DiscordBotListener extends ListenerAdapter {

    private static final TextInput codeInput = TextInput.create("linking-code", "Code", TextInputStyle.SHORT)
            .setMinLength(7)
            .setMaxLength(7)
            .setRequired(true)
            .setPlaceholder("Enter code you got from the minecraft server")
            .build();

    public static final Modal modal = Modal.create("account-link-modal", "Link your minecraft account")
            .addActionRow(codeInput)
            .build();

    private final Cerberus plugin;

    public DiscordBotListener(Cerberus plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        User user = event.getUser();
        if (user.isBot()) return;
        long userId = user.getIdLong();
        ConnectionManager connectionManager = plugin.getConnectionManager();
        if (connectionManager == null) {
            event.deferReply(true).queue((interactionHook -> {
                interactionHook.editOriginal(plugin.getConfig().otherErrorDiscord).queue();
            }));
            return;
        }
        if (connectionManager.isConnected(userId)) {
            event.deferReply(true).queue(interactionHook -> {
                interactionHook.editOriginal(plugin.getConfig().alreadyConnected).queue();
            });
            return;
        }
        event.replyModal(modal).queue();
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        User user = event.getUser();
        if (user.isBot()) return;
        long userId = user.getIdLong();
        ConnectionManager connectionManager = plugin.getConnectionManager();
        if (connectionManager == null) {
            event.deferReply(true).queue((interactionHook -> {
                interactionHook.editOriginal(plugin.getConfig().otherErrorDiscord).queue();
            }));
            return;
        }
        if (connectionManager.isConnected(userId)) {
            event.deferReply(true).queue(interactionHook -> {
                interactionHook.editOriginal(plugin.getConfig().alreadyConnected).queue();
            });
            return;
        }
        event.replyModal(modal).queue();
    }

    @Override
    public void onModalInteraction(@NotNull ModalInteractionEvent event) {
        if (!event.getModalId().equals(modal.getId())) return;
        if (event.getValues().isEmpty()) return;
        ConnectionManager connectionManager = plugin.getConnectionManager();
        if (connectionManager == null) {
            event.deferReply(true).queue(interactionHook -> {
                interactionHook.editOriginal(plugin.getConfig().otherErrorDiscord).queue();
            });
            return;
        }
        ModalMapping mapping = event.getValues().get(0);
        String code = mapping.getAsString();
        UUID uuid = connectionManager.getUuidFromCode(code);
        event.deferReply(true).queue(interactionHook -> {
            if (uuid == null) {
                interactionHook.editOriginal(plugin.getConfig().invalidCode).queue();
                return;
            }
            ConnectionManager.ConnectionStatus status = connectionManager.tryConnectAccounts(uuid, event.getUser().getIdLong(), code);
            switch (status) {
                case SUCCESS -> interactionHook.editOriginal(plugin.getConfig().accountConnected).queue();
                case INVALID_CODE -> interactionHook.editOriginal(plugin.getConfig().invalidCode).queue();
                case DUPLICATE_MINECRAFT_ID ->
                        interactionHook.editOriginal(plugin.getConfig().duplicateMinecraftId).queue();
                case DUPLICATE_DISCORD_ID ->
                        interactionHook.editOriginal(plugin.getConfig().duplicateDiscordId).queue();
                case OTHER_ERROR -> interactionHook.editOriginal(plugin.getConfig().otherErrorDiscord).queue();
            }
        });

    }

    @Override
    public void onUserContextInteraction(@NotNull UserContextInteractionEvent event) {
        Member clickingMember = event.getMember();
        if (clickingMember == null) return;
        Guild guild = event.getGuild();
        if (guild == null) return;

        User target = event.getTarget();
        if (plugin.getConnectionManager() == null) return;
        long targetId = target.getIdLong();

        event.deferReply(true).queue(interactionHook -> {
            UUID uuid = plugin.getConnectionManager().getMinecraftIdFromDiscordId(targetId);
            if (uuid == null) {
                interactionHook.editOriginal(plugin.getConfig().minecraftNotConnected).queue();
                return;
            }
            interactionHook.editOriginal("https://namemc.com/profile/%s".formatted(uuid.toString())).queue();
        });

    }

}
