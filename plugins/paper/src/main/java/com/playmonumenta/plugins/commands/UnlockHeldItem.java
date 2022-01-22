package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class UnlockHeldItem extends GenericCommand {
	public static void register() {
		registerPlayerCommand("unlockhelditem", "monumenta.command.unlockhelditem", UnlockHeldItem::run);
	}

	public static void run(CommandSender sender, Player player) throws WrapperCommandSyntaxException {
		ItemStatUtils.removeInfusion(player.getItemInHand(), ItemStatUtils.InfusionType.LOCKED);
	}
}
