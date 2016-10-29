package co.neweden.playtimer;

import java.util.ArrayList;
import java.util.UUID;

import co.neweden.websitelink.User;
import co.neweden.websitelink.jsonstorage.UserObject;

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

import co.neweden.playtimer.database.SQLite;

public class PTMain extends JavaPlugin implements Listener {

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
		getLogger().info("PTMain started!");

		// Setup vault perms
		setupPermissions();

		// Schedule plugin to run every minute (1200 Ticks) and update users4
		// currently connected in the config
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
			public void run() {
				updateAllPlayers();
			}

		}, 0L, 1200L);

		// Register Events
		getServer().getPluginManager().registerEvents(this, this);

		// Create default config.yml in the pugins folder
		this.saveDefaultConfig();

	}

	@EventHandler
	public void onLogin(PlayerJoinEvent eventLogin) {
		// Get user UUID and write it into the config file
		UUID pID = eventLogin.getPlayer().getUniqueId();

		if (!this.getConfig().isInt("players." + pID.toString() + ".totaltime")) {
			updatePlayer(eventLogin.getPlayer());
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

		if (totalTime >= 719) {
			// Check if explorer, if yes promote to builder, message player and
			// broadcast to server

			if (permission.getPrimaryGroup(player).equals("Explorer")) {
				// Checks if an Explorer has been added to the verified users
				// list
				if (verifiedUsers.contains(player))
					return;
				
				// Checks if the user exists on the forums and gives a
				// registration link if they don't
				UserObject apiObject = User.getUser(player);
				if (apiObject != null && !apiObject.userExists) {
					player.sendMessage(Util
							.formatString("&f[&7PlayTimer&f]: "
									+ "&6Congratulations! You have played for at least 12 hours, this means you are eligible to be promoted to from Explorer to Builder, all you need to do now is to register on our forum, go to http://pngn.co/16 to get started. Remember to register using your Minecraft username, otherwise you won't get promoted!"));
					return;
				}

				permission.playerRemoveGroup(null, player, "Explorer");
				permission.playerAddGroup(null, player, "Builder");
				
				// Adds the new Builder to a verified users list to stop
				// rank-up message spam until relog
				verifiedUsers.add(player);
				
				// Informs player of new rank
				player.sendMessage(Util
						.formatString("&2You have been auto-promoted to Builder! :D"));
				
				// Broadcasts user rank-up for all online players to see
				getServer().broadcastMessage(
						Util.formatString("&f[&7PlayTimer&f]: "
								+ ChatColor.DARK_GREEN + player.getName()
								+ " has just been promoted to Builder!"));
			}
		}

		if (totalTime >= 259200) {
			// Checks if player  has been on the server for a total time  of three days,
			// if so promotes the player to builder+

			if (permission.getPrimaryGroup(player).equals("Builder")) {

				permission.playerRemoveGroup(null, player, "Builder");
				permission.playerAddGroup(null, player, "Builder+");

				// Informs player of new rank
				player.sendMessage(Util.formatString("&2You have player for 3 days total, as a reward you have been promoted to Builder+, thanks for continuing to play!"));

				//Broadcasts user rank-up to server chat
				getServer().broadcastMessage(
						Util.formatString("&f[&7PlayTimer&f]: "
								+ ChatColor.DARK_GREEN + player.getName()
								+ " has just been promoted to Builder+!"));
			}
		}

		this.getConfig().set("players." + player.getUniqueId() + ".totaltime",
				totalTime + 1);
		this.getConfig().set("players." + player.getUniqueId() + ".playername", player.getName());
		this.saveConfig();

	}

	// Loops around the online players and updates the config accordingly
	public void updateAllPlayers() {
		// listPlayers refers to the player list
		for (Player listPlayers : getServer().getOnlinePlayers()) {
			updatePlayer(listPlayers);

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

	public void onDisable() {
		getLogger().info("PTMain stopped!");
	}

}
