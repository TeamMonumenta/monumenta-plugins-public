package com.playmonumenta.plugins.command;

import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.command.commands.TestNoScore;
import com.playmonumenta.plugins.managers.potion.PotionManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;

public class CommandFactory {

	/**
	 * Create and register all command.
	 *
	 * @param plugin        the plugin object
	 * @param properties    the plugin configuration
	 * @param world         the default world
	 * @param potionManager the potion manager
	 */
	public static void createCommands(JavaPlugin plugin, ServerProperties properties, World world, PotionManager potionManager) {
		if (Constants.COMMANDS_SERVER_ENABLED) {

			// Streamlined command for performance
			plugin.getCommand("testNoScore").setExecutor(new TestNoScore());
		}
	}

	/**
	 * Create and register a single command.
	 *
	 * @param plugin  the plugin object
	 * @param command the new command object
	 */
	private static void createCommand(JavaPlugin plugin, AbstractCommand command) {
		plugin.getCommand(command.getName()).setExecutor(command);
	}
}
