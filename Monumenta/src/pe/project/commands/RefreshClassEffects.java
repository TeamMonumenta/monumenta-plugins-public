package pe.project.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;

import org.bukkit.ChatColor;
import pe.project.Plugin;

public class RefreshClassEffects implements CommandExecutor {
	Plugin mPlugin;

	public RefreshClassEffects(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length > 0) {
			sender.sendMessage(ChatColor.RED + "No parameters are needed for this function!");
			return false;
		}

		Player player = null;
		if (sender instanceof Player) {
			player = (Player)sender;
		} else if (sender instanceof ProxiedCommandSender) {
			CommandSender callee = ((ProxiedCommandSender)sender).getCallee();
			if (callee instanceof Player) {
				player = (Player)callee;
			}
		}

		if (player == null) {
			sender.sendMessage(ChatColor.RED + "This command must be run by/on a player!");
			return false;
		}

		mPlugin.mPotionManager.refreshClassEffects(player);
		sender.sendMessage(ChatColor.GOLD + "Refreshed class effects for player '" + player.getName() + "'");

		return true;
	}

}
