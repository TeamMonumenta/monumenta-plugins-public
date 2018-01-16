package pe.project.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Plugin;

public class MessagingUtils {
	public static void sendActionBarMessage(Plugin plugin, Player player, String message) {
		// TODO: Is there a better way to send actionbar messages?
		String commandStr = String.format("title %s actionbar {\"text\":\"%s\",\"color\":\"yellow\"}", player.getName(), message);
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), commandStr);
	}

	public static void sendAbilityTriggeredMessage(Plugin plugin, Player player, String message) {
		// TODO: Is there a better way to send actionbar messages?
		String commandStr = String.format("title %s actionbar {\"text\":\"%s\",\"color\":\"red\"}", player.getName(), message);
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), commandStr);
	}

	public static void sendNPCMessage(Player player, String displayName, String message) {
		sendRawMessage(player, String.format(ChatColor.GOLD + "[%s] " + ChatColor.WHITE + "%s", displayName, message));
	}

	public static void sendRawMessage(Player player, String message) {
		player.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
	}

	public static void sendClickableNPCMessage(Plugin plugin, Player player, String message, String commandStr) {
		// TODO: Is there a better way to send clickable messages?
		String str = String.format("tellraw %s {\"text\":\"[%s]\",\"color\":\"light_purple\",\"clickEvent\":{\"action\":\"run_command\",\"value\":\"%s\"}}", player.getName(), message, commandStr);
		plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), str);
	}

	public static void sendStackTrace(CommandSender sender, Exception e) {
		// Get the first 1000 characters of the stacktrace and send them to the player
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		String sStackTrace = sw.toString();
		sStackTrace = sStackTrace.substring(0, Math.min(sStackTrace.length(), 1000));
		sender.sendMessage(ChatColor.GOLD + sStackTrace);
	}
}
