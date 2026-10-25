package work.alsace.buildercore.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.plugin.Plugin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class DataBaseManager {

    private final Plugin plugin;
    private HikariDataSource dataSource;

    public DataBaseManager(Plugin plugin, String host, String database, String username, String password) {
        this.plugin = plugin;

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + "/" + database + "?useSSL=false&allowPublicKeyRetrieval=true&characterEncoding=utf8");
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        config.setUsername(username);
        config.setPassword(password);

        // Hikari 常用连接池优化参数
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setIdleTimeout(30000);
        config.setConnectionTimeout(10000);

        try {
            this.dataSource = new HikariDataSource(config);
            createTable();
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "数据库连接初始化失败！", e);
        }
    }

    private void createTable() {
        CompletableFuture.runAsync(() -> {
            String sql = """
                    CREATE TABLE IF NOT EXISTS player_agreement (
                        player_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                        agreed BOOLEAN NOT NULL DEFAULT FALSE,
                        agreed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
                    );""";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.execute();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "创建 player_agreement 数据表失败！", e);
            }
        });
    }

    public CompletableFuture<Boolean> hasPlayerAgreed(UUID playerUUID) {
        return CompletableFuture.supplyAsync(() -> {
            String sql = "SELECT agreed FROM player_agreement WHERE player_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUUID.toString());
                try (ResultSet resultSet = statement.executeQuery()) {
                    if (resultSet.next()) {
                        return resultSet.getBoolean("agreed");
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "查询玩家协议状态失败: " + playerUUID, e);
            }
            return false;
        });
    }

    public CompletableFuture<Void> setPlayerAgreed(UUID playerUUID, boolean agreed) {
        return CompletableFuture.runAsync(() -> {
            // 使用 ON DUPLICATE KEY UPDATE 解决重复插入主键的问题
            String sql = """
                    INSERT INTO player_agreement (player_uuid, agreed) VALUES (?, ?)
                    ON DUPLICATE KEY UPDATE agreed = VALUES(agreed);""";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUUID.toString());
                statement.setBoolean(2, agreed);
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "更新玩家协议状态失败: " + playerUUID, e);
            }
        });
    }

    public CompletableFuture<Void> delPlayerAgreed(UUID playerUUID) {
        return CompletableFuture.runAsync(() -> {
            String sql = "DELETE FROM player_agreement WHERE player_uuid = ?";
            try (Connection connection = dataSource.getConnection();
                 PreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, playerUUID.toString());
                statement.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "删除玩家协议记录失败: " + playerUUID, e);
            }
        });
    }

    public void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }
}