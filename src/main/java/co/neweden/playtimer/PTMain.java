package co.neweden.playtimer;

import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.UUID;

import co.neweden.websitelink.User;
import co.neweden.websitelink.jsonstorage.UserObject;


import com.mysql.jdbc.Connection;
import net.milkbowl.vault.permission.Permission;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;


public class PTMain extends JavaPlugin implements Listener {


	// MySQL Database variables
	final String username = "root";
	final String password = "A=gc7bjPdDb/3WaXUvi]=";
	final String url = "jdbc:mysql://localhost:3306/playtimer_data";

	//Connection Variables
	static Connection connection;

	public static ArrayList<Player> verifiedUsers = new ArrayList<>();

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

	// Plugin enable sends login message and starts a repeating process to
	// update the config/database
	public void onEnable() {
		// Greetings console!
		getLogger().info("PlayTimer started!");

		// Setup vault perms
		setupPermissions();

		// Schedule plugin to run every minute (1200 Ticks) and update users
		// currently connected in the config
		BukkitScheduler updatePlayerScheduler = Bukkit.getServer().getScheduler();
		updatePlayerScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				updateAllPlayers();
			}

		}, 0L, 1200L);

		// Schedule plugin to run every hour to check for pending registrations
		BukkitScheduler promotePendingScheduler = Bukkit.getServer().getScheduler();
		promotePendingScheduler.scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				checkPromotePending();
			}
		}, 0L, 1200L);

		// Register Events
		getServer().getPluginManager().registerEvents(this, this);

		// Start MySQL connector
		try { //We use a try catch to avoid errors, hopefully we don't get any.
			Class.forName("com.mysql.jdbc.Driver"); //this accesses Driver in jdbc.
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
			System.err.println("jdbc driver unavailable!");
			return;
		}
		try { //Another try catch to get any SQL errors (for example connections errors)
			connection = (Connection) DriverManager.getConnection(url,username,password);
			//with the method getConnection() from DriverManager, we're trying to set
			//the connection's url, username, password to the variables we made earlier and
			//trying to get a connection at the same time. JDBC allows us to do this.
		} catch (SQLException e) { //catching errors)
			e.printStackTrace(); //prints out SQLException errors to the console (if any)
		}

		String sql = "CREATE TABLE `playtimer_data`.`users` (" +
                "  `UUID` VARCHAR(64) NOT NULL," +
                "  `TotalPlaytime` INT UNSIGNED NULL DEFAULT 0," +
                "  `PlayerName` VARCHAR(64) NULL," +
                "  `promotepending` TINYINT NULL," +
                "  PRIMARY KEY (`UUID`)," +
                "  UNIQUE INDEX `UUID_UNIQUE` (`UUID` ASC)," +
                "  UNIQUE INDEX `PlayerName_UNIQUE` (`PlayerName` ASC));";

		try {
			PreparedStatement stmt = connection.prepareStatement(sql);
			// I use executeUpdate() to update the databases table.
			stmt.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
		}

		// Create default config.yml in the plugins folder
		this.saveDefaultConfig();

	}

	@EventHandler
	public void onLogin(PlayerJoinEvent eventLogin) {
		// Get user UUID and write it into the config file
		UUID pID = eventLogin.getPlayer().getUniqueId();

		if (!this.getConfig().isInt("players." + pID.toString() + ".totaltime")) {
			updatePlayer(eventLogin.getPlayer());
			promotePending(eventLogin.getPlayer());
		}



	}

	// Player time updater and rank increaser
	public void updatePlayer(Player player) {
		int totalTime;
		try {
			totalTime = this.getConfig().getInt(
					"players." + player.getUniqueId() + ".totaltime");
		} catch (NullPointerException e) {
			totalTime = 0;
		}

		this.getConfig().set("players." + player.getUniqueId() + ".totaltime",
				totalTime + 1);
		this.getConfig().set("players." + player.getUniqueId() + ".playername", player.getName());
		this.getConfig().set("players." + player.getUniqueId() + ".promotepending", false);
		this.saveConfig();

		if (totalTime >= 360) {
			// Check if newcomer has registered and reached 6 hours, if yes promote to Member, message player and
			// broadcast to server

			if (permission.getPrimaryGroup(player).equals("Newcomer")) {
				// Checks if a newcomer has been added to the verified users
				// list
				if (verifiedUsers.contains(player))
					return;

				// Checks if the user exists on the forums and gives a
				// registration link if they don't
				if (getConfig().getBoolean("players." + player.getUniqueId() + ".promotepending", true)) {

					getLogger().info("Tried to promote " + player.getName());

					permission.playerRemoveGroup(null, player, "Newcomer");
					permission.playerAddGroup(null, player, "Member");

					// Adds the new Builder to a verified users list to stop
					// rank-up message spam until relog
					verifiedUsers.add(player);

					// Informs player of new rank
					player.sendMessage(Util
							.formatString("&2You have been auto-promoted to Member! :D"));

					// Broadcasts user rank-up for all online players to see
					getServer().broadcastMessage(
							Util.formatString("&f[&7PlayTimer&f]: "
									+ ChatColor.DARK_GREEN + player.getName()
									+ " has just been promoted to Member!"));
				}
			}
		}

		if (totalTime >= 2880) {
			// Checks if player  has been on the server for a total time of 48 hours,
			// if so promotes the player to Member+

			if (permission.getPrimaryGroup(player).equals("Member")) {

				permission.playerRemoveGroup(null, player, "Member");
				permission.playerAddGroup(null, player, "Member+");

				// Informs player of new rank
				player.sendMessage(Util.formatString("&2You have played for 48 hours total, as a reward you have been promoted to Member+, Congratulations!"));

				//Broadcasts user rank-up to server chat
				getServer().broadcastMessage(
						Util.formatString("&f[&7PlayTimer&f]: "
								+ ChatColor.DARK_GREEN + player.getName()
								+ " has just been promoted to Member+!"));
			}
		}

		if (totalTime >= 4320) {
			// Checks if player has been on the server for a total time of 72 hours,
			// if so promotes the player to Experienced

			if (permission.getPrimaryGroup(player).equals("Member+")) {

				permission.playerRemoveGroup(null, player, "Member+");
				permission.playerAddGroup(null, player, "Experienced");

				// Informs player of new rank
				player.sendMessage(Util.formatString("&2You have played for 72 hours total, as a reward you have been promoted to Experience, Congratulations!"));

				//Broadcasts user rank-up to server chat
				getServer().broadcastMessage(
						Util.formatString("&f[&7PlayTimer&f]: "
								+ ChatColor.DARK_GREEN + player.getName()
								+ " has just been promoted to Experienced!"));
			}
		}

		if (totalTime >= 9000) {
			// Checks if player has been on the server for a total time of 150 hours,
			// if so promotes the player Trusted

			if (permission.getPrimaryGroup(player).equals("Experienced")) {

				permission.playerRemoveGroup(null, player, "Experienced");
				permission.playerAddGroup(null, player, "Trusted");

				// Informs player of new rank
				player.sendMessage(Util.formatString("&2You have played for 150 hours total, as a reward you have been promoted to Trusted, Congratulations!"));

				//Broadcasts user rank-up to server chat
				getServer().broadcastMessage(
						Util.formatString("&f[&7PlayTimer&f]: "
								+ ChatColor.DARK_GREEN + player.getName()
								+ " has just been promoted to Trusted!"));
			}
		}

		if (totalTime >= 21000) {
			// Checks if player has been on the server for a total time of 350 hours,
			// if so it sends a message to the player and server that they can be nominated for senior

			if (permission.getPrimaryGroup(player).equals("Trusted")) {

				// Informs player of new rank
				player.sendMessage(Util.formatString("&2You have played for 350 hours total, it is now eligible for people to nominate you to senior "));

				//Broadcasts user rank-up to server chat
				getServer().broadcastMessage(
						Util.formatString("&f[&7PlayTimer&f]: "
								+ ChatColor.DARK_GREEN + player.getName()
								+ " has reached 350 hours, do you think they're ready for senior? Go nominate them on the website!"));
			}
		}

		if (totalTime >= 42000) {
			// Checks if player has been on the server for a total time of 700 hours and is senior,
			// if so promotes the player Senior+

			if (permission.getPrimaryGroup(player).equals("Senior")) {

				permission.playerRemoveGroup(null, player, "Senior");
				permission.playerAddGroup(null, player, "Senior+");

				// Informs player of new rank
				player.sendMessage(Util.formatString("&2You have played for 700 hours total, as a reward you have been promoted to Senior+, Congratulations!"));

				//Broadcasts user rank-up to server chat
				getServer().broadcastMessage(
						Util.formatString("&f[&7PlayTimer&f]: "
								+ ChatColor.DARK_GREEN + player.getName()
								+ " has just been promoted to Senior+!"));
			}
		}
	}


	// Checks to see if the player has a promotion pending and reminds them that they need to register on the forums
	public void promotePending(Player player) {
		if (this.getConfig().getBoolean("players." + player.getUniqueId() + ".promotepending", false)) {
			UserObject apiObject = User.getUser(player);
			if (apiObject != null && !apiObject.userExists) {
				player.sendMessage(Util
						.formatString("&f[&7PlayTimer&f]: "
								+ "&6Congratulations! You have played for at least 6 hours, this means you are eligible to be promoted to from Explorer to Builder, all you need to do now is register on our forum, go to http://pngn.co/16 to get started. Remember to register using your Minecraft username, otherwise you won't get promoted!"));
				this.getConfig().set("players." + player.getUniqueId() + ".promotepending", true);
			}
		}
	}

	// Loops around the online players and updates the config accordingly
	public void updateAllPlayers() {
		// listPlayers refers to the player list
		for (Player listPlayers : getServer().getOnlinePlayers()) {
			updatePlayer(listPlayers);
		}
	}

	// Loops and checks for pending promotions
	public void checkPromotePending() {
		for (Player listPlayers : getServer().getOnlinePlayers()) {
			promotePending(listPlayers);
		}
	}

	// Commands used ingame
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {

		if (label.equalsIgnoreCase("playtime")) {
			if (sender.hasPermission("playtimer.use")) {
				if (args.length == 0) {
					if (sender instanceof Player) {
						Player player = (Player) sender;
						int pT = this.getConfig().getInt(
								"players." + player.getUniqueId()
										+ ".totaltime");

						int pDays = 0;
						double pHours = Math.floor(pT / 60);
						int pMinutes = pT % 60;

						sender.sendMessage(ChatColor.GOLD + "Your playtime: "
								+ ChatColor.DARK_GREEN + (int) pHours
								+ " Hours" + " " + pMinutes + " Minutes");
					}
					return true;
				} else {
					if (args[0].equalsIgnoreCase("zero")) {
						sender.sendMessage(ChatColor.GOLD + "Your playtime: "
								+ ChatColor.DARK_GREEN
								+ "Doomworks is an idiot.");
						return true;
					} else {
						@SuppressWarnings("deprecation")
						OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
						String uName = this.getConfig().getString(
								"players." + target.getUniqueId()
										+ ".playername");

						if (uName == null) {
							sender.sendMessage(ChatColor.GOLD
									+ "No user under that name.");
						}

						else {
							int uT = this.getConfig().getInt(
									"players." + target.getUniqueId()
											+ ".totaltime");
							double uHours = Math.floor(uT / 60);
							int uMinutes = uT % 60;

							sender.sendMessage(ChatColor.GOLD + uName
									+ " playtime: " + ChatColor.DARK_GREEN
									+ (int) uHours + " Hours" + " " + uMinutes
									+ " Minutes");
							return true;
						}
					}

				}
			} else {
				sender.sendMessage("You do not have permission to use that.");
			}
		}

		return false;
	}

	// Plugin disable, shuts down the database connection and ends the process
	public void onDisable() {
		try { //using a try catch to catch connection errors (like wrong sql password...)
			if (connection!=null && !connection.isClosed()){ //checking if connection isn't null to
				//avoid receiving a nullpointer
				connection.close(); //closing the connection field variable.
			}
		} catch(Exception e) {
			e.printStackTrace();
		}

		getLogger().info("PlayTimer stopped!");
	}

}
