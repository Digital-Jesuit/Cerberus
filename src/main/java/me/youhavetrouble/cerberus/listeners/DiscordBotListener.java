package me.youhavetrouble.cerberus.listeners;

import me.youhavetrouble.cerberus.Cerberus;
import me.youhavetrouble.cerberus.ConnectionManager;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.ModalInteractionEvent;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;


public class DiscordBotListener extends ListenerAdapter {

    private final TextInput codeInput = TextInput.create("linking-code", "Code", TextInputStyle.SHORT)
            .setMinLength(7)
            .setMaxLength(7)
            .setRequired(true)
            .setPlaceholder("Enter code you got from the minecraft server")
            .build();

    private final Modal modal = Modal.create("account-link-modal", "Link your minecraft account")
            .addActionRow(codeInput)
            .build();

    private Cerberus plugin;

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
            event.reply("Internal error occured. Try again later!").queue();
            return;
        }
        if (connectionManager.isConnected(userId)) {
            event.reply("Your minecraft account is already connected!").queue();
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
            event.reply("Internal error occured. Try again later!").queue();
            return;
        }
        ModalMapping mapping = event.getValues().get(0);
        String code = mapping.getAsString();
        UUID uuid = connectionManager.getUuidFromCode(code);
        event.deferReply(true).queue(interactionHook -> {
            if (uuid == null) {
                interactionHook.editOriginal("Invalid code!").queue();
                return;
            }
            ConnectionManager.ConnectionStatus status = connectionManager.tryConnectAccounts(uuid, event.getUser().getIdLong(), code);
            switch (status) {
                case SUCCESS -> interactionHook.editOriginal("Account connected! Now you can log in and play!").queue();
                case INVALID_CODE -> interactionHook.editOriginal("Invalid code!").queue();
                case DUPLICATE_MINECRAFT_ID -> interactionHook.editOriginal("Minecraft account already connected!").queue();
                case DUPLICATE_DISCORD_ID -> interactionHook.editOriginal("This discord account is already connected to another account!").queue();
                case OTHER_ERROR -> interactionHook.editOriginal("Internal error occured. Try again later!").queue();
            }
        });

    }

}
