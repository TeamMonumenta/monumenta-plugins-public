package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;
import java.util.List;

import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.InfusionUtils.InfusionSelection;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.LiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class InfuseHeldItem extends GenericCommand {
	static final String COMMAND = "infusehelditem";

	@SuppressWarnings("unchecked")
	private static void registerType(InfusionSelection selection) {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.infusehelditem");

		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put(selection.getLabel(), new LiteralArgument(selection.getLabel()));
		arguments.put("level", new IntegerArgument(1));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (sender instanceof Player) {
					InfusionUtils.freeInfusion(sender, (Player)sender, selection, (Integer)args[0]);
				} else {
					CommandAPI.fail("This command can only be run by players");
				}
			})
			.register();

		arguments.clear();
		arguments.put(selection.getLabel(), new LiteralArgument(selection.getLabel()));
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("frames", new EntitySelectorArgument(EntitySelector.MANY_ENTITIES));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run(sender, (Player)args[0], (List<Entity>)args[1], selection);
			})
			.register();
	}

	public static void register() {
		registerType(InfusionSelection.ACUMEN);
		registerType(InfusionSelection.FOCUS);
		registerType(InfusionSelection.PERSPICACITY);
		registerType(InfusionSelection.TENACITY);
		registerType(InfusionSelection.VIGOR);
		registerType(InfusionSelection.VITALITY);
		registerType(InfusionSelection.REFUND);
		registerType(InfusionSelection.SPEC_REFUND);
	}

	@SuppressWarnings("unchecked")
	private static void run(CommandSender sender, Player player, List<? extends Entity> frames, InfusionSelection selection) throws WrapperCommandSyntaxException {
		for (Entity entity : frames) {
			if (!(entity instanceof ItemFrame)) {
				CommandAPI.fail("Got entity '" + entity.getType().toString() + "' that was not an item frame");
			}
		}

		try {
			InfusionUtils.doInfusion(sender, player, player.getInventory().getItemInMainHand(), (List<ItemFrame>)frames, selection);
		} catch (WrapperCommandSyntaxException ex) {
			/* Let the player also know why it failed */
			player.sendMessage(ChatColor.GOLD + "[Infusion Altar] " + ChatColor.RED + ex.getException().getMessage());
			/* Continue to propagate the failure */
			throw ex;
		}
	}
}
