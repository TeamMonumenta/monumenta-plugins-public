package pe.project.commands;

import java.util.Stack;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import org.bukkit.ChatColor;
import pe.project.Main;

public class Forward implements CommandExecutor {
	Main mPlugin;

	public Forward(Main plugin) {
		mPlugin = plugin;
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
		if (arg3.length != 0) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		if (!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This command can only be run by players");
			return true;
		}

		Player player = (Player)sender;
		UUID playerUUID = player.getUniqueId();

		// Get the stack of previous teleport locations
		Stack<Location> forwardStack = mPlugin.mForwardLocations.get(playerUUID);
		if (forwardStack == null || forwardStack.empty()) {
			sender.sendMessage(ChatColor.RED + "No forward location to teleport to");
			return true;
		}

		// Get the last entry and update the forward locations
		Location target = forwardStack.pop();

		// Save updated stack
		mPlugin.mForwardLocations.put(playerUUID, forwardStack);

		// Teleport the player
		player.teleport(target);

		sender.sendMessage("Teleporting forward");

		return true;
	}
}
