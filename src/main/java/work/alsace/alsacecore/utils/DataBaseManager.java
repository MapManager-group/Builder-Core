package work.alsace.alsacecore.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.*;
import java.util.UUID;

public class DataBaseManager {

    private final HikariDataSource dataSource;

    public DataBaseManager(String host, String database, String username, String password) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://" + host + "/" + database + "?useSSL=false");
        config.setUsername(username);
        config.setPassword(password);

        dataSource = new HikariDataSource(config);
        createTable();
        createInventoryTables();
    }

    private void createTable() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "CREATE TABLE IF NOT EXISTS player_agreement (" +
                             "id INT AUTO_INCREMENT PRIMARY KEY," +
                             "player_uuid VARCHAR(36) NOT NULL," +
                             "agreed BOOLEAN NOT NULL);")) {
            statement.execute();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createInventoryTables() {
        String sql = "CREATE TABLE IF NOT EXISTS player_inventories (" +
                "uuid VARCHAR(36) PRIMARY KEY, " +
                "inventory TEXT);";
        try (Statement statement = dataSource.getConnection().createStatement()) {
            statement.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean hasPlayerAgreed(UUID playerUUID) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT agreed FROM player_agreement WHERE player_uuid = ?")) {
            statement.setString(1, playerUUID.toString());
            ResultSet resultSet = statement.executeQuery();
            return resultSet.next() && resultSet.getBoolean("agreed");
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void setPlayerAgreed(UUID playerUUID, boolean agreed) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO player_agreement (player_uuid, agreed) VALUES (?, ?)")) {
            statement.setString(1, playerUUID.toString());
            statement.setBoolean(2, agreed);
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void delPlayerAgreed(UUID playerUUID, boolean agreed) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "DELETE FROM player_agreement WHERE player_uuid = ?")) {
            statement.setString(1, playerUUID.toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void closeDataSource() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public void savePlayerInventory(String uuid, String inventoryData) throws SQLException {
        String sql = "INSERT INTO player_inventories (uuid, inventory) " +
                "VALUES (?, ?) " +
                "ON DUPLICATE KEY UPDATE inventory = VALUES(inventory);";
        try (PreparedStatement statement = dataSource.getConnection().prepareStatement(sql)) {
            statement.setString(1, uuid);
            statement.setString(2, inventoryData);
            statement.executeUpdate();
        }
    }

    public String loadPlayerInventory(String uuid) throws SQLException {
        String sql = "SELECT inventory FROM player_inventories WHERE uuid = ?;";
        try (PreparedStatement statement = dataSource.getConnection().prepareStatement(sql)) {
            statement.setString(1, uuid);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return resultSet.getString("inventory");
                }
            }
        }
        return null;
    }
}
