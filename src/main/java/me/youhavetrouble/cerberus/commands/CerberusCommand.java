package me.youhavetrouble.cerberus.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import me.youhavetrouble.cerberus.Cerberus;
import net.dv8tion.jda.api.entities.User;
import java.util.UUID;

public class CerberusCommand {

    public static BrigadierCommand createCerberusCommand(Cerberus plugin) {
        LiteralCommandNode<CommandSource> cerberusCommand = BrigadierCommand.literalArgumentBuilder("cerberus")
                .requires(commandSource -> commandSource.hasPermission("cerberus.command"))
                .executes(commandContext -> {
                    Package pkg = CerberusCommand.class.getPackage();
                    commandContext.getSource().sendPlainMessage(pkg.getImplementationTitle() + " " + pkg.getImplementationVersion());
                    return Command.SINGLE_SUCCESS;
                })
                .then(BrigadierCommand.literalArgumentBuilder("reload")
                        .requires(commandSource -> commandSource.hasPermission("cerberus.command.reload"))
                        .executes(commandContext -> {
                            commandContext.getSource().sendPlainMessage("Reloading Cerberus...");
                            plugin.reloadPlugin();
                            commandContext.getSource().sendPlainMessage("Cerberus reloaded");
                            return Command.SINGLE_SUCCESS;
                        })
                        .build()
                )
                .then(BrigadierCommand.literalArgumentBuilder("check")
                        .requires(commandSource -> commandSource.hasPermission("cerberus.command.check"))
                        .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.string()))
                        .executes(commandContext -> {
                            String playerName = commandContext.getArgument("player", String.class);
                            if (plugin.getConnectionManager() == null) return Command.SINGLE_SUCCESS;
                            UUID uuid = plugin.getConnectionManager().getUuidFromName(playerName).getNow(null);
                            User user = plugin.getDiscordBot().getUser(uuid);

                            if (user == null) {
                                commandContext.getSource().sendPlainMessage("Could not resolve discord user");
                                return Command.SINGLE_SUCCESS;
                            }

                            commandContext.getSource().sendRichMessage(
                                    "Minecraft name: " + playerName + "<newline>" +
                                    "Minecraft ID: " + uuid + "<newline>" +
                                    "Discord name: " + user.getName() + "<newline>" +
                                    "Discord ID: " + user.getId()
                            );

                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
        return new BrigadierCommand(cerberusCommand);
    }

}
