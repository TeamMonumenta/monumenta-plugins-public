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

public class Back implements CommandExecutor {
	Main mMain;

	public Back(Main main) {
		mMain = main;
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
		Stack<Location> backStack = mMain.mBackLocations.get(playerUUID);
		if (backStack == null || backStack.empty()) {
			sender.sendMessage(ChatColor.RED + "No back location to teleport to");
			return true;
		}

		// Get the last teleport location and update the back locations
		Location target = backStack.pop();

		// Get the stack of previous /back locations and push the target location to it
		Stack<Location> forwardStack = mMain.mForwardLocations.get(playerUUID);
		if (forwardStack == null) {
			forwardStack = new Stack<Location>();
		}
		forwardStack.push(player.getLocation());

		// Set the status to indicate that the next teleport shouldn't be added to the list
		mMain.mSkipBackLocation.put(playerUUID, new Boolean(true));

		// Save updated stacks
		mMain.mBackLocations.put(playerUUID, backStack);
		mMain.mForwardLocations.put(playerUUID, forwardStack);

		// Teleport the player
		player.teleport(target);

		sender.sendMessage("Teleporting back");

		return true;
	}
}
