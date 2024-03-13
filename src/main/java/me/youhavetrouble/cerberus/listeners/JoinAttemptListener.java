package me.youhavetrouble.cerberus.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import me.youhavetrouble.cerberus.Cerberus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.minimessage.MiniMessage;

import java.util.UUID;

public class JoinAttemptListener {

    private final Cerberus plugin;

    public JoinAttemptListener(Cerberus plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onAttemptJoin(LoginEvent event) {
        // Player not allowed to join in the first place, no need to check further
        if (!event.getResult().isAllowed()) return;

        if (plugin.canJoin(event.getPlayer())) return;

        if (plugin.getConnectionManager() == null) {
            event.setResult(ResultedEvent.ComponentResult.denied(plugin.getConfig().otherErrorMinecraft));
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();
        String code = plugin.getConnectionManager().codeForUuid(playerId);

        String rawKickMessage = plugin.getConfig().linkingKickReason.replaceAll("%code%", code);
        Component disconnectMessage = MiniMessage.miniMessage().deserialize(rawKickMessage);
        event.setResult(ResultedEvent.ComponentResult.denied(disconnectMessage));
    }

}
