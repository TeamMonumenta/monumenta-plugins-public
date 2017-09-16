package pe.project.utils;

import org.bukkit.entity.Player;

import pe.project.Main;

public class MessagingUtils {
	public static void sendActionBarMessage(Main plugin, Player target, String message) {
		String commandStr = String.format("title %s actionbar {\"text\":\"%s\",\"color\":\"yellow\"}", target.getName(), message);	
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), commandStr);
	}
	
	public static void sendAbilityTriggeredMessage(Main plugin, Player target, String message) {
		String commandStr = String.format("title %s actionbar {\"text\":\"%s\",\"color\":\"red\"}", target.getName(), message);	
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), commandStr);
	}
	
	public static void sendNPCMessage(Main plugin, Player target, String npcName, String message) {
		String str = String.format("tellraw %s [{\"text\":\"[%s] \",\"color\":\"gold\"},{\"text\":\"%s\",\"color\":\"white\"}]", target.getName(), npcName, message);	
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), str);
	}
	
	public static void sendClickableNPCMessage(Main plugin, Player target, String message, String commandStr) {
		String str = String.format("tellraw %s {\"text\":\"[%s]\",\"color\":\"light_purple\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%s\"}}", target.getName(), message, commandStr);	
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), str);
	}
}
