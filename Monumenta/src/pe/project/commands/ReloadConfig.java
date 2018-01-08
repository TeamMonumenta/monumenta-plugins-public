package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import org.bukkit.ChatColor;
import pe.project.Plugin;

public class ReloadConfig implements CommandExecutor {
	Plugin mPlugin;

	public ReloadConfig(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length > 0) {
			sender.sendMessage(ChatColor.RED + "No parameters are needed for this function!");
			return false;
		}

		sender.sendMessage(ChatColor.GOLD + "Reloading config...");
		mPlugin.mNpcManager.reload(mPlugin, sender);

		return true;
	}
}
