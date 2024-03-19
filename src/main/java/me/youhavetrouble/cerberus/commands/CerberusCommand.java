package me.youhavetrouble.cerberus.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import me.youhavetrouble.cerberus.Cerberus;
import net.dv8tion.jda.api.entities.User;
import net.kyori.adventure.text.Component;

import java.util.*;

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
                        .then(BrigadierCommand.requiredArgumentBuilder("player", StringArgumentType.string())
                                .requires(commandSource -> commandSource.hasPermission("cerberus.command.check"))
                                .suggests((commandContext, suggestionsBuilder) -> {
                                    String[] args = suggestionsBuilder.getInput().split(" ");
                                    String lastArg = args[args.length-1].toLowerCase(Locale.ENGLISH);
                                    Map<String, Message> suggestions = new HashMap<>();
                                    plugin.getServer().getAllPlayers().forEach(player -> {
                                        String playerName = player.getUsername().toLowerCase(Locale.ENGLISH);
                                        if (args.length == 3 && !playerName.startsWith(lastArg)) return;
                                        suggestions.put(playerName, new LiteralMessage(player.getUniqueId().toString()));
                                    });
                                    if (plugin.getConnectionManager() != null) {
                                        plugin.getConnectionManager().getCachedNames().forEach((name, uuid) -> {
                                            if (args.length == 3 && !name.startsWith(lastArg)) return;
                                            suggestions.put(name, new LiteralMessage(uuid.toString()));
                                        });
                                    }
                                    suggestions.forEach(suggestionsBuilder::suggest);
                                    return suggestionsBuilder.buildFuture();
                                })
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
                        .executes(commandContext -> {
                            commandContext.getSource().sendMessage(Component.text("/cerberus check <player>"));
                            return Command.SINGLE_SUCCESS;
                        })
                )
                .build();
        return new BrigadierCommand(cerberusCommand);
    }

}
