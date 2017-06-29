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
}
