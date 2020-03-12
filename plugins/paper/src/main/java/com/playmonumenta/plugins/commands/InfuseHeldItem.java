package com.playmonumenta.plugins.commands;

import java.util.LinkedHashMap;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.playmonumenta.plugins.utils.InfusionUtils;
import com.playmonumenta.plugins.utils.InfusionUtils.InfusionSelection;

import io.github.jorelali.commandapi.api.CommandAPI;
import io.github.jorelali.commandapi.api.CommandPermission;
import io.github.jorelali.commandapi.api.arguments.Argument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument;
import io.github.jorelali.commandapi.api.arguments.EntitySelectorArgument.EntitySelector;
import io.github.jorelali.commandapi.api.arguments.LiteralArgument;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class InfuseHeldItem extends GenericCommand {

	@SuppressWarnings("unchecked")
	private static void registerType(InfusionSelection selection) {
		LinkedHashMap<String, Argument> arguments = new LinkedHashMap<>();
		arguments.put(selection.getLabel(), new LiteralArgument(selection.getLabel()));
		arguments.put("player", new EntitySelectorArgument(EntitySelector.ONE_PLAYER));
		arguments.put("frames", new EntitySelectorArgument(EntitySelector.MANY_ENTITIES));
		CommandAPI.getInstance().register("infusehelditem", CommandPermission.fromString("monumenta.command.infusehelditem"), arguments,
			(sender, args) -> {
				run(sender, (Player)args[0], (List<Entity>)args[1], selection);
			});

	}

	public static void register() {
		registerType(InfusionSelection.ACUMEN);
		registerType(InfusionSelection.FOCUS);
		registerType(InfusionSelection.PERSPICACITY);
		registerType(InfusionSelection.TENACITY);
		registerType(InfusionSelection.VIGOR);
		registerType(InfusionSelection.VITALITY);
	}

	@SuppressWarnings("unchecked")
	private static void run(CommandSender sender, Player player, List<? extends Entity> frames, InfusionSelection selection) throws CommandSyntaxException {
		for (Entity entity : frames) {
			if (!(entity instanceof ItemFrame)) {
				CommandAPI.fail("Got entity '" + entity.getType().toString() + "' that was not an item frame");
			} else {
				player.sendMessage("Got 1 item frame");
			}
		}

		try {
			InfusionUtils.doInfusion(sender, player, player.getInventory().getItemInMainHand(), (List<ItemFrame>)frames, selection);
		} catch (CommandSyntaxException ex) {
			/* Let the player also know why it failed */
			player.sendMessage(ChatColor.RED + ex.getMessage());
			/* Continue to propagate the failure */
			throw ex;
		}
	}
}
