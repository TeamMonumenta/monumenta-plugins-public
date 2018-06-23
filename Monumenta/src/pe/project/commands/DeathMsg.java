package pe.project.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import pe.project.Plugin;
import pe.project.utils.ScoreboardUtils;

public class DeathMsg implements CommandExecutor {
	Plugin mPlugin;

	public DeathMsg(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length > 1) {
			return false;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command can only be run by a player!");
			return false;
		}

		int newState = 0;
		if (arg3.length == 1) {
			String argument = arg3[0].toLowerCase();
			if (argument.equals("on") || argument.equals("true")) {
				newState = 0;
			} else if (argument.equals("off") || argument.equals("false")) {
				newState = 1;
			} else {
				sender.sendMessage(ChatColor.RED + "The argument to this function should be 'on' or 'off'");
				return false;
			}
		}

		Player player = (Player)sender;

		if (arg3.length == 1) {
			ScoreboardUtils.setScoreboardValue(player, "DeathMessage", newState);
		} else {
			newState = ScoreboardUtils.getScoreboardValue(player, "DeathMessage");
		}

		sender.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "Death Message Settings");
		sender.sendMessage(ChatColor.AQUA + "When you die, your death message will be shown to:");

		if (newState == 0) {
			sender.sendMessage(ChatColor.GREEN + "  All players on the current shard");
		} else {
			sender.sendMessage(ChatColor.GREEN + "  Only you");
		}

		sender.sendMessage(ChatColor.AQUA + "Change this with " + ChatColor.GOLD + "/deathmsg on" +
		                   ChatColor.AQUA + " or " + ChatColor.GOLD + "/deathmsg off");

		return true;
	}
}
