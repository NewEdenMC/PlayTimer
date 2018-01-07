package co.neweden.playtimer;


import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class MoveConfig {

    private static Main plugin;
    MoveConfig(Main pl){plugin = pl;}

    public static void getConfigAndUpdate(){
        Configuration config = plugin.getConfig();
        boolean allowConfigMovement = config.getBoolean("moveconfig");
        if (allowConfigMovement) {
            ConfigurationSection playersConfig = config.getConfigurationSection("players");
            for (String uuid : playersConfig.getKeys(false)) {
                int totalTime = playersConfig.getInt(uuid + ".totaltime");
                addConfigToDatabase(uuid, totalTime);
            }
            plugin.getLogger().info("Config has been moved");
        } else {plugin.getLogger().info("`moveconfig` has been set to false in the Config !!!");}
    }

    private static void addConfigToDatabase(String uuid, int TotalPlayTime){
        String serverName = plugin.getConfig().getString("servername", "survival");
        String sql = "INSERT INTO `users` (`UUID`, `TotalPlaytime`, `promotepending`, `server_" + serverName + "`) VALUES (?, ?, ?, ?)" +
                " ON DUPLICATE KEY UPDATE `TotalPlaytime` = TotalPlaytime + ?, `server_" + serverName + "` = ?;";
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
