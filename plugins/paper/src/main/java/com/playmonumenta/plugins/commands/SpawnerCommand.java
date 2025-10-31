package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.spawners.SpawnerActionManager;
import com.playmonumenta.plugins.spawners.types.ProtectorSpawner;
import com.playmonumenta.plugins.spawners.types.RallySpawner;
import com.playmonumenta.plugins.utils.SpawnerUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.SuggestionInfo;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.FunctionArgument;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.arguments.TextArgument;
import dev.jorel.commandapi.wrappers.FunctionWrapper;
import java.util.List;
import java.util.Set;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.GameMode;
import org.bukkit.command.CommandSender;
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

						int shields = args.getUnchecked("shields");
						SpawnerUtils.setShields(item, shields);
					}),
				new CommandAPICommand("type")
					.withSubcommands(
						new CommandAPICommand("guarded")
							.withArguments(new IntegerArgument("guarded"))
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								int guardBlock = args.getUnchecked("guarded");
								SpawnerUtils.setSpawnerType(item, SpawnerUtils.GUARDED_ATTRIBUTE, guardBlock);
							}),
						new CommandAPICommand("ensnared")
							.withArguments(new IntegerArgument("ensnared"))
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}
								int ensnaredBlock = args.getUnchecked("ensnared");
								if (ensnaredBlock < 4) {
									player.sendMessage("The command needs the radius to be greater than 3.");
									return;
								}
								SpawnerUtils.setSpawnerType(item, SpawnerUtils.ENSNARED_ATTRIBUTE, ensnaredBlock);
							}),
						new CommandAPICommand("decaying")
							.withArguments(new IntegerArgument("decaying"))
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								int decayingBlock = args.getUnchecked("decaying");
								SpawnerUtils.setSpawnerType(item, SpawnerUtils.DECAYING_ATTRIBUTE, decayingBlock);
							}),
						new CommandAPICommand("protector")
							.withArguments(new BooleanArgument("protector"))
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								boolean protector = args.getUnchecked("protector");
								ProtectorSpawner.setProtector(item, protector);
							}),
						new CommandAPICommand("rally")
							.withArguments(new IntegerArgument("rally"))
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								int rallyBlock = args.getUnchecked("rally");
								RallySpawner.setRally(item, rallyBlock);
							}),
						new CommandAPICommand("cat")
							.withArguments(new IntegerArgument("cat"))
							.withArguments(new IntegerArgument("catRadius"))
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								int catBlock = args.getUnchecked("cat");
								int catRadius = args.getUnchecked("catRadius");

								SpawnerUtils.setSpawnerType(item, SpawnerUtils.CAT_ATTRIBUTE, catBlock);
								SpawnerUtils.setSpawnerType(item, SpawnerUtils.CAT_ATTRIBUTE_RADIUS, catRadius);
							}),
						new CommandAPICommand("sequential")
							.withArguments(new IntegerArgument("sequence"))
							.withArguments(new IntegerArgument("radius"))
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								int sequenceBlock = args.getUnchecked("sequence");
								int sequenceRadius = args.getUnchecked("radius");

								if (sequenceRadius >= 100) {
									player.sendMessage("The command needs the radius to be under 100.");
									return;
								}

								SpawnerUtils.setSpawnerType(item, SpawnerUtils.SEQUENCE_ATTRIBUTE, sequenceBlock);
								SpawnerUtils.setSpawnerType(item, SpawnerUtils.SEQUENCE_ATTRIBUTE_RADIUS, sequenceRadius);
							})
					),
				new CommandAPICommand("function")
					.withSubcommands(
						new CommandAPICommand("set")
							.withArguments(new FunctionArgument("function"))
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								FunctionWrapper[] functions = args.getUnchecked("function");
								if (functions.length == 0) {
									throw CommandAPI.failWithString("Failed to get function");
								}
								SpawnerUtils.setCustomFunction(item, functions[0]);
							}),
						new CommandAPICommand("unset")
							.executesPlayer((player, args) -> {
								ItemStack item = getHeldItemAndSendErrors(player);
								if (item == null) {
									return;
								}

								SpawnerUtils.unsetCustomFunction(item);
							})
					),
				new CommandAPICommand("lospool")
					.withArguments(new GreedyStringArgument("los pool").includeSuggestions(
						ArgumentSuggestions.stringCollection(info -> LibraryOfSoulsIntegration.getPoolNames())
					))
					.executesPlayer((player, args) -> {
						ItemStack item = getHeldItemAndSendErrors(player);
						if (item == null) {
							return;
						}

						String losPool = args.getUnchecked("los pool");
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

								String action = args.getUnchecked("name");
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

								String action = args.getUnchecked("name");
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

								String actionIdentifier = args.getUnchecked("action identifier");
								String parameterName = args.getUnchecked("parameter name");
								String valueString = args.getUnchecked("value");
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

								String actionIdentifier = args.getUnchecked("action identifier");
								String parameterName = args.getUnchecked("parameter name");
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
		return !LibraryOfSoulsIntegration.getPool(losPool).isEmpty();
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
	private static String[] getActionIdentifierSuggestions(SuggestionInfo<CommandSender> info) {
		if (info.sender() instanceof Player player) {
			ItemStack item = player.getInventory().getItemInMainHand();
			if (SpawnerUtils.isSpawner(item)) {
				List<String> breakActions = SpawnerUtils.getBreakActionIdentifiers(item);
				return !breakActions.isEmpty() ? breakActions.toArray(new String[0]) : new String[0];
			}
		}
		return new String[0];
	}

	// Only returns the break action identifiers that are not already set on the item.
	private static String[] getAvailableActionIdentifiersSuggestions(SuggestionInfo<CommandSender> info) {
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
	private static String[] getParameterNameSuggestions(SuggestionInfo<CommandSender> info) {
		String previousAction = info.previousArgs().getUnchecked(0);
		if (SpawnerActionManager.actionExists(previousAction)) {
			return SpawnerActionManager.getActionParameters(previousAction).keySet().toArray(new String[0]);
		}
		return new String[0];
	}

	// Returns the default value for the specified break action parameter
	private static String[] getDefaultParameterValueSuggestion(SuggestionInfo<CommandSender> info) {
		String actionIdentifier = info.previousArgs().getUnchecked(0);
		String parameterName = info.previousArgs().getUnchecked(1);
		if (SpawnerActionManager.actionExists(actionIdentifier)) {
			Object value = SpawnerActionManager.getActionParameters(actionIdentifier).get(parameterName);
			if (value != null) {
				return new String[]{value.toString()};
			}
		}
		return new String[0];
	}
}
