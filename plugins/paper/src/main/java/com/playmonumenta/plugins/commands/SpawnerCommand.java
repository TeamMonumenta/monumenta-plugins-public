package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.spawners.SpawnerActionManager;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.checkerframework.checker.nullness.qual.Nullable;

public class SpawnerCommand {

	public static void register() {
		new CommandAPICommand("spawner")
			.withPermission("monumenta.command.spawner")
			.withSubcommands(
				new CommandAPICommand("shields")
					.withArguments(new IntegerArgument("shields"))
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}

						int shields = (int) args[0];
						SpawnerUtils.setShields(item, shields);
					}),
				new CommandAPICommand("lospool")
					.withArguments(new GreedyStringArgument("los pool").includeSuggestions(
						ArgumentSuggestions.stringCollection(info -> LibraryOfSoulsIntegration.getPoolNames())
					))
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}

						String losPool = (String) args[0];
						if (!isLosPoolValid(losPool)) {
							player.sendMessage(Component.text("The selected pool has 0 elements.", NamedTextColor.RED));
							return;
						}

						SpawnerUtils.setLosPool(item, losPool);
					}),
				new CommandAPICommand("action")
					.withSubcommands(
						new CommandAPICommand("add")
							.withArguments(
								new StringArgument("name").includeSuggestions(
									ArgumentSuggestions.strings(SpawnerCommand::getAvailableActionIdentifiersSuggestions)
								)
							)
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								String action = (String) args[0];
								if (!SpawnerActionManager.actionExists(action)) {
									player.sendMessage(Component.text("The specified action does not exist.", NamedTextColor.RED));
									return;
								}

								SpawnerUtils.addBreakAction(item, action);
							}),
						new CommandAPICommand("remove")
							.withArguments(
								new StringArgument("name")
									.includeSuggestions(ArgumentSuggestions.strings(SpawnerCommand::getActionIdentifierSuggestions))
							)
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								String action = (String) args[0];
								if (!SpawnerActionManager.actionExists(action)) {
									player.sendMessage(Component.text("The specified action does not exist.", NamedTextColor.RED));
									return;
								}

								SpawnerUtils.removeBreakAction(item, action);
							}),
						new CommandAPICommand("parameter")
							.withArguments(
								new StringArgument("action identifier")
									.includeSuggestions(ArgumentSuggestions.strings(SpawnerCommand::getActionIdentifierSuggestions)),
								new StringArgument("parameter name")
									.includeSuggestions(ArgumentSuggestions.strings(SpawnerCommand::getParameterNameSuggestions)),
								new LiteralArgument("set"),
								new TextArgument("value")
									.includeSuggestions(ArgumentSuggestions.strings(SpawnerCommand::getDefaultParameterValueSuggestion))
							)
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								String actionIdentifier = (String) args[0];
								String parameterName = (String) args[1];
								String valueString = (String) args[2];
								if (!SpawnerActionManager.actionExists(actionIdentifier)) {
									player.sendMessage(Component.text("The specified action does not exist.", NamedTextColor.RED));
									return;
								}

								SpawnerUtils.setParameterValue(item, actionIdentifier, parameterName, valueString);
							}),
							new CommandAPICommand("parameter")
								.withArguments(
									new StringArgument("action identifier")
										.includeSuggestions(ArgumentSuggestions.strings(SpawnerCommand::getActionIdentifierSuggestions)),
									new StringArgument("parameter name")
										.includeSuggestions(ArgumentSuggestions.strings(SpawnerCommand::getParameterNameSuggestions)),
									new LiteralArgument("reset")
								)
								.executesPlayer((player, args) -> {
									ItemStack item = getHeldItemAndSendErrors(player);
									if (item == null) {
										return;
									}

									String actionIdentifier = (String) args[0];
									String parameterName = (String) args[1];
									if (!SpawnerActionManager.actionExists(actionIdentifier)) {
										player.sendMessage(Component.text("The specified action does not exist.", NamedTextColor.RED));
										return;
									}

									Object value = SpawnerActionManager.getActionParameters(actionIdentifier).get(parameterName);

									if (value != null) {
										SpawnerUtils.setParameterValue(item, actionIdentifier, parameterName, value);
									}
								})
					)
			)
			.register();
	}

	private static boolean isLosPoolValid(String losPool) {
		return LibraryOfSoulsIntegration.getPool(losPool).keySet().size() > 0;
	}

	private static @Nullable ItemStack getHeldItemAndSendErrors(Player player) {
		if (player.getGameMode() != GameMode.CREATIVE) {
			player.sendMessage(Component.text("Must be in creative mode to use this command!", NamedTextColor.RED));
			return null;
		}
		ItemStack item = player.getInventory().getItemInMainHand();
		if (!SpawnerUtils.isSpawner(item)) {
			player.sendMessage(Component.text("Must be holding a spawner!", NamedTextColor.RED));
			return null;
		}
		return item;
	}

	// ----- Suggestions ----- //

	// Returns all break action identifiers that are set on the item.
	private static String[] getActionIdentifierSuggestions(SuggestionInfo info) {
		if (info.sender() instanceof Player player) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (SpawnerUtils.isSpawner(item)) {
				List<String> breakActions = SpawnerUtils.getBreakActionIdentifiers(item);
				return breakActions.size() > 0 ? breakActions.toArray(new String[0]) : new String[0];
			}
		}
		return new String[0];
	}

	// Only returns the break action identifiers that are not already set on the item.
	private static String[] getAvailableActionIdentifiersSuggestions(SuggestionInfo info) {
		if (info.sender() instanceof Player player) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (SpawnerUtils.isSpawner(item)) {
				List<String> breakActions = SpawnerUtils.getBreakActionIdentifiers(item);
				Set<String> actionIdentifiers = SpawnerActionManager.getActionKeySet();
				return actionIdentifiers.stream().filter(key -> !breakActions.contains(key)).toArray(String[]::new);
			}
		}

		return new String[0];
	}

	// Returns the names of the parameters available for the specified break action identifier
	private static String[] getParameterNameSuggestions(SuggestionInfo info) {
		if (SpawnerActionManager.actionExists((String) info.previousArgs()[0])) {
			return SpawnerActionManager.getActionParameters((String) info.previousArgs()[0]).keySet().toArray(new String[0]);
		}
		return new String[0];
	}

	// Returns the default value for the specified break action parameter
	private static String[] getDefaultParameterValueSuggestion(SuggestionInfo info) {
		String actionIdentifier = (String) info.previousArgs()[0];
		String parameterName = (String) info.previousArgs()[1];
		if (SpawnerActionManager.actionExists(actionIdentifier)) {
			Object value = SpawnerActionManager.getActionParameters(actionIdentifier).get(parameterName);
			if (value != null) {
				return new String[]{value.toString()};
			}
		}
		return new String[0];
	}
}
