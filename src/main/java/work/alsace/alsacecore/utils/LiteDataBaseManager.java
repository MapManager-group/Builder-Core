package work.alsace.alsacecore.utils;

import work.alsace.alsacecore.AlsaceCore;

import java.sql.*;

public class LiteDataBaseManager {

    private Connection connection;
    private final AlsaceCore plugin;

    public LiteDataBaseManager(AlsaceCore plugin) {
        this.plugin = plugin;
    }

    public void connect() {
        try {
            connection = DriverManager.getConnection("jdbc:sqlite:" + plugin.getDataFolder()+ "/inventory.db");
            createTables();
        } catch (SQLException e) {
            plugin.getLogger().severe("[背包同步] 数据库连接错误");
        }
    }

    public void disconnect() {
        if (connection!= null) {
            try {
                connection.close();
            }   catch (SQLException e) {
                plugin.getLogger().severe("[背包同步] 数据库关闭错误");
            }
        }
    }

    private void createTables() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS player_inventories (" +
                "uuid TEXT PRIMARY KEY, " +
                "inventory TEXT);";
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    public void savePlayerInventory(String uuid, String inventoryData) throws SQLException {
        String sql = "INSERT OR REPLACE INTO player_inventories (uuid, inventory) VALUES (?, ?);";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid);
            statement.setString(2, inventoryData);
            statement.executeUpdate();
        }
    }

    public String loadPlayerInventory(String uuid) throws SQLException {
        String sql = "SELECT inventory FROM player_inventories WHERE uuid = ?;";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
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
