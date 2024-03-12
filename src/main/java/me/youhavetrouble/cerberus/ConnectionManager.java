package me.youhavetrouble.cerberus;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.SecureRandom;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ConnectionManager {

    private final Cerberus plugin;
    private final Random random = new SecureRandom();

    private final Cache<String, UUID> connectionUuids = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();
    private final Cache<UUID, String> connectionCodes = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();

    protected ConnectionManager(Cerberus plugin) {
        this.plugin = plugin;
    }

    /**
     * Get or create a code for given UUID
     * @param uuid UUID of the player
     * @return 7 character code used to link the accounts
     */
    @NotNull
    public String codeForUuid(@NotNull UUID uuid) {
        String code = connectionCodes.getIfPresent(uuid);
        if (code != null) return code;
        code = generateCode();

        // safety measure to not duplicate codes
        while (connectionUuids.asMap().containsKey(code)) {
            code = generateCode();
        }

        connectionCodes.put(uuid, code);
        connectionUuids.put(code, uuid);
        return code;
    }

    public ConnectionStatus tryConnectAccounts(@NotNull UUID minecraftId, long discordId, @NotNull String code) {
        String cachedCode = connectionCodes.getIfPresent(minecraftId);
        if (!code.equalsIgnoreCase(cachedCode)) return ConnectionStatus.INVALID_CODE;
        if (plugin.getDatabase() == null) return ConnectionStatus.OTHER_ERROR;
        ConnectionStatus status = plugin.getDatabase().saveConnection(minecraftId, discordId);
        if (!ConnectionStatus.SUCCESS.equals(status)) return status;
        connectionCodes.invalidate(minecraftId);
        connectionUuids.invalidate(code);
        return status;
    }

    @Nullable
    public UUID getUuidFromCode(@NotNull String code) {
        return connectionUuids.getIfPresent(code);
    }

    public boolean isConnected(@NotNull UUID id) {
        if (plugin.getDatabase() == null) return false;
        return plugin.getDatabase().getDiscordSnowflakeByMinecraftUuid(id) != null;
    }

    public boolean isConnected(long id) {
        if (plugin.getDatabase() == null) return false;
        return plugin.getDatabase().getMinecraftuuidByDiscordSnowflake(id) != null;
    }

    /**
     * Generates a random code consisting of alphanumeric characters.
     * @return a string representing the generated code
     */
    private String generateCode() {
        StringBuilder builder = new StringBuilder(7);
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890";
        for(int i = 0; i < 7; i++) {
            int index = random.nextInt(characters.length());
            builder.append(characters.charAt(index));
        }
        return builder.toString();
    }

    public enum ConnectionStatus {
        SUCCESS,
        INVALID_CODE,
        DUPLICATE_MINECRAFT_ID,
        DUPLICATE_DISCORD_ID,
        OTHER_ERROR,
    }

}
