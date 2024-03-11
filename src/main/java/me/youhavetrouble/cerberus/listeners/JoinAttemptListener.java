package me.youhavetrouble.cerberus.listeners;

import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import me.youhavetrouble.cerberus.Cerberus;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

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
            event.setResult(ResultedEvent.ComponentResult.denied(Component.text("Error occured. Try again later")));
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();
        String code = plugin.getConnectionManager().codeForUuid(playerId);

        // TODO make the message configurable
        Component disconnectMessage = Component.empty()
                .append(Component.text("You need to connect your discord account to play!"))
                .append(Component.newline())
                .append(Component.text("Join <link to discord here> and use our bot to type"))
                .append(Component.newline())
                .append(Component.newline())
                .append(Component.text(code, NamedTextColor.RED))
                .append(Component.newline())
                .append(Component.text("The code will expire within 3 minutes."));

        event.setResult(ResultedEvent.ComponentResult.denied(disconnectMessage));


    }

}
