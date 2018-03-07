package pe.project.commands;

import java.util.Stack;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ProxiedCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.metadata.FixedMetadataValue;

import pe.project.Constants;
import pe.project.Plugin;
import pe.project.utils.CommandUtils;

public class Forward implements CommandExecutor {
	Plugin mPlugin;

	public Forward(Plugin plugin) {
		mPlugin = plugin;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean onCommand(CommandSender sender, Command command, String arg2, String[] arg3) {
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

		int num_steps = 1;

		if (arg3.length == 1) {
			try {
				num_steps = CommandUtils.parseIntFromString(sender, arg3[0]);
			} catch (Exception e) {
				return false;
			}
		} else if (arg3.length != 0) {
			sender.sendMessage(ChatColor.RED + "Invalid number of parameters!");
			return false;
		}

		Stack<Location> forwardStack = null;
		Stack<Location> backStack = null;
		boolean endOfList = false;

		// Get the stack of previous teleport locations
		if (player.hasMetadata(Constants.PLAYER_FORWARD_STACK_METAKEY)) {
			forwardStack = (Stack<Location>)player.getMetadata(Constants.PLAYER_FORWARD_STACK_METAKEY).get(0).value();
		}
		if (forwardStack == null || forwardStack.empty()) {
			player.sendMessage(ChatColor.RED + "No forward location to teleport to");
			return true;
		}

		// Get the stack of previous /forward locations and push the target location to it
		if (player.hasMetadata(Constants.PLAYER_BACK_STACK_METAKEY)) {
			backStack = (Stack<Location>)player.getMetadata(Constants.PLAYER_BACK_STACK_METAKEY).get(0).value();
		}
		if (backStack == null) {
			backStack = new Stack<Location>();
		}

		// Pop items off the stack, adding popped elements to the opposite stack
		Location target = player.getLocation();
		for (int i = 0; i < num_steps; i++) {
			backStack.push(target);
			target = forwardStack.pop();

			if (forwardStack.empty()) {
				endOfList = true;
				break;
			}
		}

		// Set the status to indicate that the next teleport shouldn't be added to the list
		player.setMetadata(Constants.PLAYER_SKIP_BACK_ADD_METAKEY, new FixedMetadataValue(mPlugin, true));

		// Save updated stacks
		player.setMetadata(Constants.PLAYER_FORWARD_STACK_METAKEY, new FixedMetadataValue(mPlugin, forwardStack));
		player.setMetadata(Constants.PLAYER_BACK_STACK_METAKEY, new FixedMetadataValue(mPlugin, backStack));

		// Teleport the player
		player.teleport(target);

		if (endOfList) {
			player.sendMessage("Teleporting forward (end of list)");
		} else {
			player.sendMessage("Teleporting forward");
		}

		return true;
	}
}
