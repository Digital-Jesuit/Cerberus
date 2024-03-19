package me.youhavetrouble.cerberus;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.velocitypowered.api.proxy.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.SecureRandom;
import java.util.Locale;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ConnectionManager {

    private final Cerberus plugin;
    private final Random random = new SecureRandom();

    private final HttpClient httpClient = HttpClient.newHttpClient();

    private final Gson gson = new Gson();

    private final Cache<String, UUID> connectionUuids = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();
    private final Cache<UUID, String> connectionCodes = CacheBuilder.newBuilder()
            .expireAfterWrite(3, TimeUnit.MINUTES)
            .build();

    private final Cache<String, UUID> nameCache = CacheBuilder.newBuilder()
            .expireAfterWrite(1, TimeUnit.HOURS)
            .maximumSize(200)
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
        for (int i = 0; i < 7; i++) {
            int index = random.nextInt(characters.length());
            builder.append(characters.charAt(index));
        }
        return builder.toString();
    }

    /**
     * Retrieves the UUID associated with a Minecraft username.
     * @param minecraftName The Minecraft username.
     * @return A CompletableFuture that resolves to the UUID associated with the username,
     * or null if the username is not found or if there is an error.
     */
    public CompletableFuture<UUID> getUuidFromName(@NotNull String minecraftName) {
        String lowercaseName = minecraftName.toLowerCase(Locale.ENGLISH);
        UUID playerId = nameCache.getIfPresent(lowercaseName);
        if (playerId != null) return CompletableFuture.completedFuture(playerId);

        Optional<Player> optionalPlayer = plugin.getServer().getPlayer(lowercaseName);
        if (optionalPlayer.isPresent()) {
            Player player = optionalPlayer.get();
            nameCache.put(lowercaseName, player.getUniqueId());
            return CompletableFuture.completedFuture(player.getUniqueId());
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("https://api.mojang.com/users/profiles/minecraft/%s".formatted(lowercaseName)))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenApply(responseBody -> {
                        UUID id = UUID.fromString(gson.fromJson(responseBody, java.util.Map.class).get("id").toString().replaceFirst(
                                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}+)", "$1-$2-$3-$4-$5"
                        ));
                        nameCache.put(lowercaseName, id);
                        return id;
                    })
                    .exceptionally(throwable -> null);
        } catch (URISyntaxException e) {
            return CompletableFuture.completedFuture(null);
        }
    }

    public enum ConnectionStatus {
        SUCCESS,
        INVALID_CODE,
        DUPLICATE_MINECRAFT_ID,
        DUPLICATE_DISCORD_ID,
        OTHER_ERROR,
    }

}
