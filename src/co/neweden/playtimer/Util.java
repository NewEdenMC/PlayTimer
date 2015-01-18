package co.neweden.playtimer;

public final class Util {
  
  private Util() { }
  
	public static String formatString(String text) {
		text = text.replaceAll("&0", "§0"); // Black
		text = text.replaceAll("&1", "§1"); // Dark Blue
		text = text.replaceAll("&2", "§2"); // Dark Green
		text = text.replaceAll("&3", "§3"); // Dark Aqua
		text = text.replaceAll("&4", "§4"); // Dark Red
		text = text.replaceAll("&5", "§5"); // Dark Purple
		text = text.replaceAll("&6", "§6"); // Gold
		text = text.replaceAll("&7", "§7"); // Gray
		text = text.replaceAll("&8", "§8"); // Dark Gray
		text = text.replaceAll("&9", "§9"); // Blue
		text = text.replaceAll("&a", "§a"); // Green
		text = text.replaceAll("&b", "§b"); // Aqua
		text = text.replaceAll("&c", "§c"); // Red
		text = text.replaceAll("&d", "§d"); // Light Purple
		text = text.replaceAll("&e", "§e"); // Yellow
		text = text.replaceAll("&f", "§f"); // White
		
		text = text.replaceAll("&k", "§k"); // Obfuscated
		text = text.replaceAll("&l", "§l"); // Bold
		text = text.replaceAll("&m", "§m"); // Strikethrough
		text = text.replaceAll("&o", "§o"); // Italic
		text = text.replaceAll("&r", "§r"); // Reset
		
		return text;
	}
  
}
