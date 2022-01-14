package com.playmonumenta.plugins.commands.experiencinator;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

import org.bukkit.ChatColor;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.custominventories.ExperiencinatorMainGui;
import com.playmonumenta.plugins.custominventories.ExperiencinatorSettingsGui;
import com.playmonumenta.plugins.utils.CommandUtils;
import com.playmonumenta.scriptedquests.managers.InteractableManager;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.BooleanArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

public class ExperiencinatorCommand {

	private static final String COMMAND = "experiencinator";
	private static final String PERMISSION_SELF = "monumenta.command.experiencinator.self";
	private static final String PERMISSION_OTHERS = "monumenta.command.experiencinator.others";
	private static final String PERMISSION_ITEMS = "monumenta.command.experiencinator.items";

	@SuppressWarnings("unchecked")
	public static void register() {

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_SELF)
			.withArguments(new LiteralArgument("menu"))
			.executes((sender, args) -> {
				Player player = CommandUtils.getPlayerFromSender(sender);
				useExperiencinator(player, (experiencinator, item) -> ExperiencinatorMainGui.show(player, Plugin.getInstance(), experiencinator, item));
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_OTHERS)
			.withArguments(new LiteralArgument("menu"), new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>) args[0]) {
					useExperiencinator(player, (experiencinator, item) -> ExperiencinatorMainGui.show(player, Plugin.getInstance(), experiencinator, item));
				}
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_SELF)
			.withArguments(new LiteralArgument("convert"))
			.executes((sender, args) -> {
				Player player = CommandUtils.getPlayerFromSender(sender);
				useExperiencinator(player, (experiencinator, item) -> ExperiencinatorUtils.useExperiencinator(experiencinator, item, player));
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_OTHERS)
			.withArguments(new LiteralArgument("convert"), new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>) args[0]) {
					useExperiencinator(player, (experiencinator, item) -> ExperiencinatorUtils.useExperiencinator(experiencinator, item, player));
				}
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_SELF)
			.withArguments(new LiteralArgument("configure"))
			.executes((sender, args) -> {
				Player player = CommandUtils.getPlayerFromSender(sender);
				useExperiencinator(player, (experiencinator, item) -> ExperiencinatorSettingsGui.showConfig(player, Plugin.getInstance(), experiencinator, item));
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_OTHERS)
			.withArguments(new LiteralArgument("configure"), new EntitySelectorArgument("players", EntitySelectorArgument.EntitySelector.MANY_PLAYERS))
			.executes((sender, args) -> {
				for (Player player : (Collection<Player>) args[0]) {
					useExperiencinator(player, (experiencinator, item) -> ExperiencinatorSettingsGui.showConfig(player, Plugin.getInstance(), experiencinator, item));
				}
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(PERMISSION_ITEMS)
			.withArguments(new LiteralArgument("convert_items"),
			               new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER),
			               new EntitySelectorArgument("items", EntitySelectorArgument.EntitySelector.MANY_ENTITIES),
			               new StringArgument("conversionName")
				               .replaceSuggestions(info -> {
					               ExperiencinatorConfig config = ExperiencinatorUtils.getConfig(info.sender() instanceof Player ? ((Player) info.sender()).getLocation() : null);
					               return config != null ? config.getConversionNames().toArray(new String[0]) : new String[0];
				               }),
			               new StringArgument("conversionResultName")
				               .replaceSuggestions(info -> {
					               String conversionName = info.previousArgs() != null ? (String) info.previousArgs()[2] : null;
					               if (conversionName == null) {
						               return new String[0];
					               }
					               ExperiencinatorConfig config = ExperiencinatorUtils.getConfig(info.sender() instanceof Player ? ((Player) info.sender()).getLocation() : null);
					               if (config == null) {
						               return new String[0];
					               }
					               ExperiencinatorConfig.Conversion conversion = config.getConversion(conversionName);
					               return conversion != null ? conversion.getConversionRateNames().toArray(new String[0]) : new String[0];
				               }),
			               new BooleanArgument("giveToPlayerOnFail"))
			.executes((sender, args) -> {
				for (Entity entity : (Collection<Entity>) args[1]) {
					ExperiencinatorUtils.convertItemEntity((Player) args[0], entity, (String) args[2], (String) args[3], (Boolean) args[4]);
				}
			})
			.register();

	}

	private static void useExperiencinator(Player player, BiConsumer<ExperiencinatorConfig.Experiencinator, ItemStack> func) throws WrapperCommandSyntaxException {
		ExperiencinatorConfig experiencinatorConfig = ExperiencinatorUtils.getConfig(player.getLocation());
		if (experiencinatorConfig == null) {
			player.sendRawMessage(ChatColor.RED + "There's a problem with the server's Experiencinator configuration. Please contact a moderator.");
			return;
		}

		// Try SQ's "used item" first
		ItemStack item = InteractableManager.getUsedItem();
		ExperiencinatorConfig.Experiencinator experiencinator = item != null ? experiencinatorConfig.getExperiencinator(item) : null;

		// Try mainhand second
		if (experiencinator == null) {
			item = player.getInventory().getItemInMainHand();
			experiencinator = experiencinatorConfig.getExperiencinator(item);
		}

		// If still no Experiencinator was found, use the best one from the inventory
		if (experiencinator == null) {
			int bestExperiencinatorIndex = -1;
			List<ExperiencinatorConfig.Experiencinator> experiencinators = experiencinatorConfig.getExperiencinators();
			for (ItemStack currentItem : player.getInventory().getContents()) {
				if (currentItem == null) {
					continue;
				}
				ExperiencinatorConfig.Experiencinator currentExperiencinator = experiencinatorConfig.getExperiencinator(currentItem);
				if (currentExperiencinator == null) {
					continue;
				}
				int currentIndex = experiencinators.indexOf(currentExperiencinator);
				if (currentIndex > bestExperiencinatorIndex) {
					experiencinator = currentExperiencinator;
					bestExperiencinatorIndex = currentIndex;
					item = currentItem;
				}
			}
		}

		if (experiencinator != null) {
			func.accept(experiencinator, item);
		} else {
			CommandAPI.fail("You don't have an Experiencinator or one of its upgrades in your inventory!");
		}
	}

}
