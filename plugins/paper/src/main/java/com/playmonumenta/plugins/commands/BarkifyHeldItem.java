package com.playmonumenta.plugins.commands;

import java.util.ArrayList;
import java.util.List;

import com.playmonumenta.plugins.utils.ItemStatUtils;

import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument.EntitySelector;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;

/*
 * NOTICE!
 * If this enchantment gets changed, make sure someone updates the Python item replacement code to match!
 * Constants and new enchantments included!
 * This most likely means @NickNackGus or @Combustible
 * If this does not happen, your changes will NOT persist across weekly updates!
 */
public class BarkifyHeldItem extends GenericCommand {
	static final String COMMAND = "barkifyhelditem";

	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.barkifyhelditem");
		List<Argument> arguments = new ArrayList<>();

		arguments.add(new MultiLiteralArgument("Barking", "Barking2", "Debarking"));
		arguments.add(new EntitySelectorArgument("player", EntitySelector.ONE_PLAYER));

		new CommandAPICommand(COMMAND)
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				run((Player)args[1], (String)args[0]);
			})
			.register();
	}

	private static void run(Player player, String selection) throws WrapperCommandSyntaxException {
		ItemStack is = player.getItemInHand();
		if (selection.equals("Barking")) {
			player.playSound(player.getLocation(), Sound.ENTITY_WOLF_AMBIENT, 1, 1f);
			ItemStatUtils.addInfusion(is, ItemStatUtils.InfusionType.BARKING, 1, player.getUniqueId());
		} else if (selection.equals("Barking2")) {
			player.playSound(player.getLocation(), Sound.ENTITY_WOLF_WHINE, 1, 1f);
			ItemStatUtils.addInfusion(is, ItemStatUtils.InfusionType.BARKING, 2, player.getUniqueId());
		} else {
			player.playSound(player.getLocation(), Sound.ENTITY_WOLF_GROWL, 1, 1f);
			ItemStatUtils.addInfusion(is, ItemStatUtils.InfusionType.DEBARKING, 1, player.getUniqueId());
		}
		ItemStatUtils.generateItemStats(is);
	}
}
