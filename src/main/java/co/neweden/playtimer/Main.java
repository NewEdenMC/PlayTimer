package co.neweden.playtimer;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import co.neweden.websitelink.User;
import co.neweden.websitelink.jsonstorage.UserObject;

import net.milkbowl.vault.permission.Permission;

import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin implements Listener {

	//HashMap to store UserInfo
    static Map<UUID,PlayerInfo> UserMap = new HashMap<>();

    private static String serverName;

	//Connection Variables
	static Connection connection;
	private static Main plugin;

	public static Main getPlugin() { return plugin; }

	public static String getCurrentServerName() { return serverName; }

	public static Permission permission = null;

	private boolean setupPermissions() {
		RegisteredServiceProvider<Permission> permissionProvider = getServer()
				.getServicesManager().getRegistration(
						net.milkbowl.vault.permission.Permission.class);
		if (permissionProvider != null) {
			permission = permissionProvider.getProvider();
		}
		return (permission != null);
	}

	public void onEnable() {
		plugin = this;
        this.saveDefaultConfig();
        serverName = getConfig().getString("servername", "survival");

        plugin.getLogger().info("This Server name is: server_"+serverName);

		String host = getConfig().getString("mysql.host", null);
		String port = getConfig().getString("mysql.port", null);
		String database = getConfig().getString("mysql.database", null);
		if (host == null || port == null || database == null) {
			getLogger().log(Level.INFO, "No database information received from config.");
			return;
		}

		String url = String.format("jdbc:mysql://%s:%s/%s?autoReconnect=true", host, port, database);

		try {
			connection = DriverManager.getConnection(url, getConfig().getString("mysql.user", ""), getConfig().getString("mysql.password", ""));
		} catch (SQLException e) { //catching errors)
			getLogger().log(Level.SEVERE, "An SQL Exception occurred while connecting to database.", e); //prints out SQLException errors to the console (if any)
			return;
		}

		try {
			connection.createStatement().execute(
					"CREATE TABLE IF NOT EXISTS `users` (" +
					"  `UUID` VARCHAR(64) NOT NULL," +
					"  `TotalPlaytime` INT UNSIGNED NULL DEFAULT 0," +
					"  `promotepending` TINYINT NOT NULL DEFAULT 0," +
					"  `seniorNominationMsgSent` TINYINT NOT NULL DEFAULT 0," +
					"  PRIMARY KEY (`UUID`)" +
					");"
			);
		} catch (SQLException e) {
			getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to create the database tables.", e);
		}

		// Setup vault perms
		setupPermissions();

		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> updateAllPlayers(), 0L, 1200L);
		Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> checkPromotePending(), 0L, 36000L);
		Bukkit.getPluginManager().registerEvents(this, this);

        checkTable();
		//MoveConfig.getConfigAndUpdate(); //Remove this after first use
	}

	public void onDisable() {
		try {
			if (connection != null && !connection.isClosed()) {
				connection.close();
			}
		} catch(Exception e) {
			getLogger().log(Level.SEVERE, "Unable to close SQL Connection.", e);
		}
		UserMap.clear();
		serverName = null;
		Bukkit.getScheduler().cancelTasks(this);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerJoin(PlayerJoinEvent event) {
		PlayerInfo playerInfo = getUser(event.getPlayer().getUniqueId());
		checkUpdateRank(event.getPlayer(), playerInfo);
		checkSendPromoteMessage(event.getPlayer(), playerInfo);
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPlayerQuit(PlayerQuitEvent event) {
		UUID uuid = event.getPlayer().getUniqueId();
		if (UserMap.containsKey(uuid))
			UserMap.remove(uuid);
	}

	public void checkUpdateRank(Player player, PlayerInfo playerInfo) {
        String primaryGroup = permission.getPrimaryGroup(player);

		if (playerInfo.getTotalPlayTime() >= 360 && primaryGroup.equalsIgnoreCase("newcomer")) {
			// Check if newcomer has registered and reached 6 hours, if yes promote to Member, message player and
			// broadcast to server

			UserObject apiObject = User.getUser(player);

			if (!playerInfo.isPromotePending()) {
				// run this if the user just hit the promotion time, only once
				playerInfo.setPromotePending(true);
				if (apiObject == null || !apiObject.userExists)
					player.sendMessage(Util.formatString("&f[&7PlayTimer&f]: &6Congratulations! You have played for at least 6 hours, this means you are eligible to be promoted to from Newcomer to Member, all you need to do now is register on our website, type &e/register&6 to register!"));
			}

			if (playerInfo.isPromotePending() && apiObject != null && apiObject.userExists) {
				// this will run if the user has hit or passed promotion time and registered

				permission.playerRemoveGroup(null, player, "Newcomer");
				permission.playerAddGroup(null, player, "Member");
				playerInfo.setPromotePending(false);

				player.sendMessage(Util.formatString("&2You have been auto-promoted to Member! :D"));
				getServer().broadcastMessage(Util.formatString("&f[&7PlayTimer&f]: &2" + playerInfo.getPlayerName() + " has just been promoted to Member!"));
			}
		}

		if (playerInfo.getTotalPlayTime() >= 2880 && primaryGroup.equalsIgnoreCase("member")) {
			// Checks if player  has been on the server for a total time of 48 hours,
			// if so promotes the player to Member+

			permission.playerRemoveGroup(null, player, "Member");
			permission.playerAddGroup(null, player, "Member+");

			player.sendMessage(Util.formatString("&2You have played for 48 hours total, as a reward you have been promoted to Member+, Congratulations!"));
			getServer().broadcastMessage(Util.formatString("&f[&7PlayTimer&f]: &2" + playerInfo.getPlayerName() + " has just been promoted to Member+!"));
		}

		if (playerInfo.getTotalPlayTime() >= 4320 && primaryGroup.equalsIgnoreCase("member+")) {
			// Checks if player has been on the server for a total time of 72 hours,
			// if so promotes the player to Experienced

			permission.playerRemoveGroup(null, player, "Member+");
			permission.playerAddGroup(null, player, "Experienced");

			player.sendMessage(Util.formatString("&2You have played for 72 hours total, as a reward you have been promoted to Experience, Congratulations!"));
			getServer().broadcastMessage(Util.formatString("&f[&7PlayTimer&f]: &2" + playerInfo.getPlayerName() + " has just been promoted to Experienced!"));
		}

		if (playerInfo.getTotalPlayTime() >= 9000 && primaryGroup.equalsIgnoreCase("experienced")) {
			// Checks if player has been on the server for a total time of 150 hours,
			// if so promotes the player Trusted

			permission.playerRemoveGroup(null, player, "Experienced");
			permission.playerAddGroup(null, player, "Trusted");

			player.sendMessage(Util.formatString("&2You have played for 150 hours total, as a reward you have been promoted to Trusted, Congratulations!"));
			getServer().broadcastMessage(Util.formatString("&f[&7PlayTimer&f]: &2" + playerInfo.getPlayerName() + " has just been promoted to Trusted!"));
		}

		if (playerInfo.getTotalPlayTime() >= 21000 && primaryGroup.equalsIgnoreCase("trusted")) {
			// Checks if player has been on the server for a total time of 350 hours,
			// if so it sends a message to the player and server that they can be nominated for senior

			if (!playerInfo.isSeniorNominationMsgSent()) {
				playerInfo.setSeniorNominationMsgSent(true);
				player.sendMessage(Util.formatString("&2You have played for 350 hours total, it is now eligible for people to nominate you to senior "));
				getServer().broadcastMessage(Util.formatString("&f[&7PlayTimer&f]: &2" + playerInfo.getPlayerName() + " has reached 350 hours, do you think they're ready for senior? Go nominate them on the website!"));
			}
		}

		if (playerInfo.getTotalPlayTime() >= 42000 && primaryGroup.equalsIgnoreCase("senior")) {
			// Checks if player has been on the server for a total time of 700 hours and is senior,
			// if so promotes the player Senior+

			permission.playerRemoveGroup(null, player, "Senior");
			permission.playerAddGroup(null, player, "Senior+");

			player.sendMessage(Util.formatString("&2You have played for 700 hours total, as a reward you have been promoted to Senior+, Congratulations!"));
			getServer().broadcastMessage(Util.formatString("&f[&7PlayTimer&f]: &2" + playerInfo.getPlayerName() + " has just been promoted to Senior+!"));
		}
	}

    public PlayerInfo getUser(UUID uuid) {
        // Checks if user was added previously in the hashmap
		PlayerInfo info = UserMap.get(uuid);
        if (info != null) return info;

        // since we're not caching the player go find them
		// setup some default values in case we don't find them
        int totalTime = 0;
        boolean promotePending = false;
        boolean seniorNominationMsgSent = false;

        try {
        	PreparedStatement st = connection.prepareStatement("SELECT * FROM users WHERE uuid=?");
        	st.setString(1, uuid.toString());
        	ResultSet rs = st.executeQuery();
        	if (rs.next()) {
        		totalTime = rs.getInt("TotalPlaytime");
        		promotePending = rs.getBoolean("promotepending");
				seniorNominationMsgSent = rs.getBoolean("seniorNominationMsgSent");
			}
		} catch (SQLException e) {
        	getLogger().log(Level.SEVERE, "An SQL Exception occurred while getting user data.", e);
		}

		// setup new PlayerInfo with either values from DB or Default Values, add it to the map for caching and return
		PlayerInfo userInfo = new PlayerInfo(uuid, totalTime, promotePending, seniorNominationMsgSent);
		UserMap.put(uuid, userInfo);
		return userInfo;
	}

    // Checks to see if the player has a promotion pending and reminds them that they need to register on the forums
	public void checkSendPromoteMessage(Player player, PlayerInfo playerInfo) {
		if (playerInfo.isPromotePending())
			player.sendMessage(Util.formatString("&f[&7PlayTimer&f]: &6Remember as you have now played for at least 6 hours you are eligible to be promoted to from Newcomer to Member, all you need to do now is register on our website, type &e/register&6 to register!"));
	}

	// Loops around the online players and updates the config accordingly
	public void updateAllPlayers() {
		// listPlayers refers to the player list
		for (Player player : getServer().getOnlinePlayers()) {
			PlayerInfo playerInfo = getUser(player.getUniqueId());
			playerInfo.incrementPlayTime();
			checkUpdateRank(player, playerInfo);
		}
	}

	// Loops and checks for pending promotions
	public void checkPromotePending() {
		for (Player player : getServer().getOnlinePlayers()) {
			PlayerInfo playerInfo = getUser(player.getUniqueId());
			checkSendPromoteMessage(player, playerInfo);
		}
	}

	public void checkTable(){
        try {
                connection.createStatement().execute("ALTER TABLE `users` ADD COLUMN `server_" + serverName + "` VARCHAR(45) NOT NULL DEFAULT 0;");
        } catch (SQLException e) {
            if   (!e.getSQLState().equals("42S21")) {
                getLogger().log(Level.SEVERE, "An SQL Exception occurred while trying to add the ServerName Column", e);
            }
        }
    }

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (args.length == 0) {
			if (!(sender instanceof Player)) {
				sender.sendMessage("Only players can check their play time, to check the play time of someone else run this command followed by their name.");
				return true;
			}

			Player player = (Player) sender;
			PlayerInfo userInfo = getUser(player.getUniqueId());
			int pT = userInfo.getTotalPlayTime();

			double pHours = Math.floor(pT / 60);
			int pMinutes = pT % 60;

			sender.sendMessage(ChatColor.GOLD + "Your playtime: "
					+ ChatColor.DARK_GREEN + (int) pHours
					+ " Hours" + " " + pMinutes + " Minutes");
			return true;
		}

		if (args[0].equalsIgnoreCase("reload")) {
			if (!sender.hasPermission("playtimer.reload")) {
				sender.sendMessage("You do not have permission to run this sub-command.");
				return true;
			}
			onDisable();
			reloadConfig();
			onEnable();
			sender.sendMessage("Plugin reloaded");
			return true;
		}

		if (args[0].equalsIgnoreCase("zero")) {
			sender.sendMessage(ChatColor.GOLD + "Your playtime: "
					+ ChatColor.DARK_GREEN
					+ "Doomworks is an idiot.");
			return true;
		}

		OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
		if (target == null) {
			sender.sendMessage(ChatColor.RED + "This player has not connected to the server before.");
			return true;
		}

		PlayerInfo targetInfo = getUser(target.getUniqueId());
		String uName = targetInfo.getPlayerName();

		if (targetInfo.getTotalPlayTime() == 0) {
			sender.sendMessage(ChatColor.GOLD
					+ "No user under that name.");
		}

		else {
			int uT = targetInfo.getTotalPlayTime();
			double uHours = Math.floor(uT / 60);
			int uMinutes = uT % 60;

			sender.sendMessage(ChatColor.GOLD + uName
					+ " playtime: " + ChatColor.DARK_GREEN
					+ (int) uHours + " Hours" + " " + uMinutes
					+ " Minutes");
			return true;
		}

		return false;
	}

}
