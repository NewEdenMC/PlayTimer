package net.doomworks.playtimer;

import java.util.UUID;

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

public class PlayTimer extends JavaPlugin implements Listener {

	public static Permission permission = null;
	
	private boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> permissionProvider = getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }
        return (permission != null);
    }
	
	// Plugin enable sends login message and starts a repeating process to update the config/database
	public void onEnable() {
		// Greetings console!
		getLogger().info("PlayTimer started!");
		
		// Setup vault perms
		setupPermissions();
		
		// Schedule plugin to run every minute (1200 Ticks) and update users currently connected in the config
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
		
		
		if (this.getConfig().isInt("players." + pID.toString() + ".totaltime") == false) {
			updatePlayer(eventLogin.getPlayer());
		}		
	}
	
	// Player time updater
	public void updatePlayer(Player player) {
		int totalTime = 0;
		String userName = player.getName().toString();
		try {
			totalTime = this.getConfig().getInt("players." + player.getUniqueId() + ".totaltime");
		} catch (NullPointerException e) {
			totalTime = 0;
		}
		
		if(totalTime >=719) {
			// Check if explorer, if yes promote to builder, message player and broadcast to server
			
			if (permission.getPrimaryGroup(player).equals("Explorer")) {
				if (HTTPRequest.forumUserExists(userName) == false) {
					player.sendMessage(ChatColor.WHITE + "[" + ChatColor.GRAY + "PlayTimer" + ChatColor.WHITE + "]:" + " " + ChatColor.DARK_GREEN + "You need to register on the forums before we can promote you to Builder. Please register at http://pngn.co/q");
					return;
				} else {
					permission.playerRemoveGroup(null, player, "Explorer");
					permission.playerAddGroup(null, player, "Builder");
				
					player.sendMessage(ChatColor.DARK_GREEN + "You have been auto-promoted to Builder! :D");
				
					getServer().broadcastMessage(ChatColor.WHITE + "[" + ChatColor.GRAY + "PlayTimer" + ChatColor.WHITE + "]:" + " " + ChatColor.DARK_GREEN + player.getName() + " has just been promoted to Builder!");
				}
			}
		}
		
		this.getConfig().set("players." + player.getUniqueId() + ".totaltime", totalTime + 1);
		this.getConfig().set("players." + player.getUniqueId() + ".playername",  userName);
		this.saveConfig();
		
	}
	
	// Loops around the online players and updates the config accordingly
	public void updateAllPlayers() {
		// listPlayers refers to the player list
		for(Player listPlayers : getServer().getOnlinePlayers()) {
			updatePlayer(listPlayers);
			
		}
	}
	
	// Commands used ingame
	public boolean onCommand(CommandSender sender, Command cmd, String label, String [] args) {
		
		Player player = (Player) sender;
		
		int pT = this.getConfig().getInt("players." + player.getUniqueId() + ".totaltime"); 
		
		double pHours = Math.floor(pT / 60);
		int pMinutes = pT % 60;
		
		
		if (label.equalsIgnoreCase("playtime")) {
			if(sender.hasPermission("playtimer.use")) {	
				if(args.length == 0) {
					if(sender instanceof Player) {
						sender.sendMessage(ChatColor.GOLD + "Your playtime: " + ChatColor.DARK_GREEN + (int) pHours + " Hours" + " " + pMinutes + " Minutes");
					}
					return true;
				} else {
					if(args[0].equalsIgnoreCase("zero")) {
						if(sender instanceof Player) {
							sender.sendMessage(ChatColor.GOLD + "Your playtime: " + ChatColor.DARK_GREEN + "Doomworks is an idiot.");
						}
						return true;
					} else {
						@SuppressWarnings("deprecation")
						OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
						String uName = this.getConfig().getString("players." + target.getUniqueId() + ".playername");
						
						if(uName == null) {
							sender.sendMessage(ChatColor.GOLD + "No user under that name.");
						}
						
						else {
							if(sender instanceof Player) {
								int uT = this.getConfig().getInt("players." + target.getUniqueId() + ".totaltime");
								double uHours = Math.floor(uT / 60);
								int uMinutes = uT % 60;
								
								sender.sendMessage(ChatColor.GOLD + uName + " playtime: " + ChatColor.DARK_GREEN + (int) uHours + " Hours" + " " + uMinutes + " Minutes");
							}
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
		getLogger().info("PlayTimer stopped!");
	}
	
}