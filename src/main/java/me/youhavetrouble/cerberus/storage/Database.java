package me.youhavetrouble.cerberus.storage;

import me.youhavetrouble.cerberus.ConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface Database {

    /**
     * Attempts to save the new connection.
     * @param minecraftUuid Minecraft account's UUID
     * @param discordSnowflake Discord snowflake
     * @return Result of the attempt
     */
    ConnectionManager.ConnectionStatus saveConnection(@NotNull UUID minecraftUuid, long discordSnowflake);

    /**
     * Retrieves the Discord snowflake associated with the given Minecraft UUID.
     * @param minecraftUuid The Minecraft UUID to search for.
     * @return The Discord snowflake associated with the Minecraft UUID, or null if not found.
     */
    @Nullable
    Long getDiscordSnowflakeByMinecraftUuid(@NotNull UUID minecraftUuid);

    /**
     * Retrieves the Minecraft UUID associated with the given Discord snowflake.
     * @param discordSnowflake The Discord snowflake to search for.
     * @return The Minecraft UUID associated with the Discord snowflake, or null if not found.
     */
    @Nullable
    UUID getMinecraftuuidByDiscordSnowflake(long discordSnowflake);


}
