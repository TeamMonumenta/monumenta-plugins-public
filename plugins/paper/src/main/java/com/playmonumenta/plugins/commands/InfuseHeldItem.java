package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
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
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
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
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.infusehelditem");

		Argument selectionArg = new MultiLiteralArgument(InfusionSelection.ACUMEN.getLabel(),
		                                                 InfusionSelection.FOCUS.getLabel(),
		                                                 InfusionSelection.PERSPICACITY.getLabel(),
		                                                 InfusionSelection.TENACITY.getLabel(),
		                                                 InfusionSelection.VIGOR.getLabel(),
		                                                 InfusionSelection.VITALITY.getLabel(),
		                                                 InfusionSelection.REFUND.getLabel(),
		                                                 InfusionSelection.SPEC_REFUND.getLabel());

		List<Argument> arguments = new ArrayList<>();
		arguments.add(selectionArg);
		arguments.add(new IntegerArgument("level", 1));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (sender instanceof Player) {
					InfusionSelection selection = InfusionSelection.getInfusionSelection((String) args[0]);
					if (selection == null) {
						CommandAPI.fail("Invalid infusion selection; how did we get here?");
					}
					InfusionUtils.freeInfusion(sender, (Player)sender, selection, (Integer)args[1]);
				} else {
					CommandAPI.fail("This command can only be run by players");
				}
			})
			.register();

		arguments.clear();
		arguments.add(selectionArg);
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));
		arguments.add(new EntitySelectorArgument("frames", EntitySelector.MANY_ENTITIES));
		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				InfusionSelection selection = InfusionSelection.getInfusionSelection((String) args[0]);
				if (selection == null) {
					CommandAPI.fail("Invalid infusion selection; how did we get here?");
				}
				run(sender, (Player)args[1], (List<Entity>)args[2], selection);
			})
			.register();
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
