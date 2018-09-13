package com.playmonumenta.plugins.command;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.command.commands.*;
import com.playmonumenta.plugins.managers.potion.PotionManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import org.bukkit.World;
import org.bukkit.plugin.java.JavaPlugin;

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
			createCommand(plugin, new Back(plugin));
			createCommand(plugin, new BroadcastCommand(plugin, properties.getBroadcastCommandEnabled()));
			createCommand(plugin, new CheckEmptyInventory(plugin));
			createCommand(plugin, new ClearEffects(plugin, potionManager));
			createCommand(plugin, new DeathMsg(plugin));
			createCommand(plugin, new DebugInfo(plugin, potionManager));
			createCommand(plugin, new Forward(plugin));
			createCommand(plugin, new GetScore(plugin));
			createCommand(plugin, new GildifyHeldItem(plugin));
			createCommand(plugin, new GiveSoulbound(plugin));
			createCommand(plugin, new HopeifyHeldItem(plugin));
			createCommand(plugin, new IncrementDaily(plugin));
			createCommand(plugin, new MinusExp(plugin));
			createCommand(plugin, new RemoveTags(plugin));
			createCommand(plugin, new TrackedEffect(plugin, potionManager));
			createCommand(plugin, new TransferScores(plugin));
			createCommand(plugin, new TransferServer(plugin, properties));
			createCommand(plugin, new UpdateApartments(plugin));

			// Streamlined command for performance
			plugin.getCommand("testNoScore").setExecutor(new TestNoScore());
		}

		if (Constants.CLASSES_ENABLED) {
			createCommand(plugin, new RefreshClassEffects(plugin, potionManager));
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
