package co.neweden.playtimer;


import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MoveConfig {

    public static void getConfigAndUpdate() {
        Configuration config = Main.getPlugin().getConfig();
        boolean allowConfigMovement = config.getBoolean("moveconfig");
        if (allowConfigMovement) {
            ConfigurationSection playersConfig = config.getConfigurationSection("players");
            for (String uuid : playersConfig.getKeys(false)) {
                int totalTime = playersConfig.getInt(uuid + ".totaltime");
                addConfigToDatabase(uuid, totalTime);
            }
            Main.getPlugin().getLogger().info("Config has been moved");
        } else Main.getPlugin().getLogger().info("`moveconfig` has been set to false in the Config !!!");
    }

    private static void addConfigToDatabase(String uuid, int TotalPlayTime){
        String dbServerName = "server_" + Main.getCurrentServerName();
        String sql = "INSERT INTO `users` (`UUID`, `TotalPlaytime`, `promotepending`, `" + dbServerName + "`) VALUES (?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE `TotalPlaytime` = TotalPlaytime + ?, `" + dbServerName + "` = ?;";
        try {
            PreparedStatement stmt = Main.connection.prepareStatement(sql);
            stmt.setString(1, uuid);
            stmt.setInt(2, TotalPlayTime);
            stmt.setBoolean(3, false);
            stmt.setInt(4, TotalPlayTime);
            stmt.setInt(5, TotalPlayTime);
            stmt.setInt(6, TotalPlayTime);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
