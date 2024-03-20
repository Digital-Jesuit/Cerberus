package me.youhavetrouble.cerberus.storage;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import me.youhavetrouble.cerberus.ConnectionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.sqlite.JDBC;
import javax.sql.DataSource;
import java.io.File;
import java.sql.*;
import java.util.UUID;

public class SqliteDatabase implements Database {

    private final Logger logger;
    private final DataSource dataSource;

    public SqliteDatabase(Logger logger) throws SQLException {
        this.logger = logger;
        DriverManager.registerDriver(new JDBC());
        File dbFile = new File("plugins/cerberus");
        if (!dbFile.exists()) {
            dbFile.mkdir();
        }
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:sqlite:plugins/cerberus/data.db");
        hikariConfig.setMaximumPoolSize(1); // sqlite is not exactly thread safe
        hikariConfig.setPoolName("CerberusPool-SQLite");
        this.dataSource = new HikariDataSource(hikariConfig);
        createTables();
    }

    private void createTables() {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement createConnectionsTable = connection.prepareStatement(
                    "create table if not exists `connections` (minecraft_id varchar(36) not null unique primary key, discord_id bigint unique not null);"
            );
            createConnectionsTable.executeUpdate();
        } catch (SQLException e) {
            logger.error("Failed to initialize database!");
            throw new RuntimeException(e);
        }
    }

    @Override
    public ConnectionManager.ConnectionStatus saveConnection(@NotNull UUID minecraftUuid, long discordSnowflake) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("insert into connections (minecraft_id, discord_id) values (?, ?);");
            statement.setString(1, minecraftUuid.toString());
            statement.setLong(2, discordSnowflake);
            statement.executeUpdate();
            return ConnectionManager.ConnectionStatus.SUCCESS;
        } catch (SQLException e) {
            if (e.getErrorCode() == 21) { // error 21 is unique constraint failed
                if (e.getMessage().contains("discord_id")) return ConnectionManager.ConnectionStatus.DUPLICATE_DISCORD_ID;
                if (e.getMessage().contains("minecraft_id")) return ConnectionManager.ConnectionStatus.DUPLICATE_MINECRAFT_ID;
                return ConnectionManager.ConnectionStatus.OTHER_ERROR;
            }
            logger.error("Failed to save connection!");
            return ConnectionManager.ConnectionStatus.OTHER_ERROR;
        }
    }

    @Override
    public @Nullable Long getDiscordSnowflakeByMinecraftUuid(@NotNull UUID minecraftUuid) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("select discord_id from connections where minecraft_id = ?;");
            statement.setString(1, minecraftUuid.toString());
            ResultSet result = statement.executeQuery();
            long id = result.getLong("discord_id");
            if (id == 0) return null;
            return id;
        } catch (SQLException e) {
            return null;
        }
    }

    @Override
    public @Nullable UUID getMinecraftuuidByDiscordSnowflake(long discordSnowflake) {
        try (Connection connection = dataSource.getConnection()) {
            PreparedStatement statement = connection.prepareStatement("select minecraft_id from connections where discord_id = ?;");
            statement.setLong(1, discordSnowflake);
            ResultSet result = statement.executeQuery();
            String stringId = result.getString("minecraft_id");
            if (stringId == null) return null;
            return UUID.fromString(stringId);
        } catch (SQLException|IllegalArgumentException e) {
            return null;
        }
    }
}
