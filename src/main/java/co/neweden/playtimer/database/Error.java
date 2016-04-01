package co.neweden.playtimer.database;

import co.neweden.playtimer.PlayTimer;

import java.util.logging.Level;

public class Error {
    public static void execute(PlayTimer plugin, Exception ex){
        plugin.getLogger().log(Level.SEVERE, "Couldn't execute MySQL statement: ", ex);
    }
    public static void close(PlayTimer plugin, Exception ex){
        plugin.getLogger().log(Level.SEVERE, "Failed to close MySQL connection: ", ex);
    }
}