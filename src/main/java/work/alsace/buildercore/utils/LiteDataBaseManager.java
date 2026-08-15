package work.alsace.buildercore.utils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import work.alsace.buildercore.BuilderCore;

import java.sql.*;

public class LiteDataBaseManager {

    private Connection connection;
    private final BuilderCore plugin;

    public LiteDataBaseManager(BuilderCore plugin) {
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

        String sqlLocation = "CREATE TABLE IF NOT EXISTS player_locations (" +
                "uuid TEXT PRIMARY KEY, " +
                "world TEXT, " +
                "x DOUBLE, " +
                "y DOUBLE, " +
                "z DOUBLE, " +
                "yaw FLOAT, " +
                "pitch FLOAT);";

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
            statement.executeUpdate(sqlLocation);
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

    public void savePlayerLocation(String uuid, Location location) throws SQLException {
        String sql = "REPLACE INTO player_locations (uuid, world, x, y, z, yaw, pitch) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid);
            statement.setString(2, location.getWorld().getName());
            statement.setDouble(3, location.getX());
            statement.setDouble(4, location.getY());
            statement.setDouble(5, location.getZ());
            statement.setFloat(6, location.getYaw());
            statement.setFloat(7, location.getPitch());
            statement.executeUpdate();
        }
    }

    public Location loadPlayerLocation(String uuid) throws SQLException {
        String sql = "SELECT world, x, y, z, yaw, pitch FROM player_locations WHERE uuid = ?";
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, uuid);

            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    String worldName = resultSet.getString("world");
                    double x = resultSet.getDouble("x");
                    double y = resultSet.getDouble("y");
                    double z = resultSet.getDouble("z");
                    float yaw = resultSet.getFloat("yaw");
                    float pitch = resultSet.getFloat("pitch");

                    World world = Bukkit.getWorld(worldName);
                    if (world != null) {
                        return new Location(world, x, y, z, yaw, pitch);
                    }
                }
            }
        }
        return null;
    }

}
