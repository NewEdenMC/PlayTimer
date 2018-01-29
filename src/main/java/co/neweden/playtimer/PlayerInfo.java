package co.neweden.playtimer;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;
import java.util.logging.Level;

public class PlayerInfo {

    private UUID uuid;
    private String playerName;
    private int totalPlaytime;
    private boolean promotepending;
    private boolean seniorNominationMsgSent;

    PlayerInfo(UUID uuid, int totalTime, boolean promotePending, boolean seniorNominationMsgSent) {
        this.uuid = uuid;
        totalPlaytime = totalTime;
        this.promotepending = promotePending;
        this.seniorNominationMsgSent = seniorNominationMsgSent;

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid); // we need to support player checking
        playerName = player != null ? player.getName() : uuid.toString(); // just show the UUID if the server has never seen the player
    }

    public String getPlayerName() { return playerName; }

    public int getTotalPlayTime() { return totalPlaytime; }

    public boolean incrementPlayTime() { return incrementPlayTime(1); }

    public boolean incrementPlayTime(int amount) {
        String dbServerName = "server_" + Main.getCurrentServerName();
        try {
            PreparedStatement stmt = Main.connection.prepareStatement("INSERT INTO `users` (`UUID`, `TotalPlaytime`, `promotepending`, `" + dbServerName + "`) VALUES (?, ?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE `TotalPlaytime` = TotalPlaytime + ?, `" + dbServerName + "` = " + dbServerName + " + ?;");
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, amount);
            stmt.setBoolean(3, false);
            stmt.setInt(4, amount);
            stmt.setInt(5, amount);
            stmt.setInt(6, amount);
            stmt.executeUpdate();
            totalPlaytime++;
            return true;
        } catch (SQLException e) {
            Main.getPlugin().getLogger().log(Level.SEVERE, "Could not increment the play time for user '" + uuid + "'", e);
        }
        return false;
    }

    public boolean isPromotePending() { return promotepending; }

    public boolean setPromotePending(boolean isPending) {
        try {
            PreparedStatement stmt = Main.connection.prepareStatement("INSERT INTO `users` (`UUID`, `TotalPlaytime`, `promotepending`) VALUES (?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE `promotepending` = ?");
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, 1);
            stmt.setBoolean(3, isPending);
            stmt.setBoolean(4, isPending);
            stmt.executeUpdate();
            promotepending = isPending;
            return true;
        } catch (SQLException e) {
            Main.getPlugin().getLogger().log(Level.SEVERE, "Could not set promote pending for user '" + uuid + "'", e);
        }
        return false;
    }

    public boolean isSeniorNominationMsgSent() { return seniorNominationMsgSent; }

    public boolean setSeniorNominationMsgSent(boolean hasSent) {
        try {
            PreparedStatement stmt = Main.connection.prepareStatement("INSERT INTO `users` (`UUID`, `TotalPlaytime`, `promotepending`) VALUES (?, ?, ?)" +
                    " ON DUPLICATE KEY UPDATE `seniorNominationMsgSent` = ?");
            stmt.setString(1, uuid.toString());
            stmt.setInt(2, 1);
            stmt.setBoolean(3, hasSent);
            stmt.setBoolean(4, hasSent);
            stmt.executeUpdate();
            seniorNominationMsgSent = hasSent;
            return true;
        } catch (SQLException e) {
            Main.getPlugin().getLogger().log(Level.SEVERE, "Could not set senior nomination message sent status for user '" + uuid + "'", e);
        }
        return false;
    }

}
