package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class StatTrackAdd {
	public static void register() {
		EntitySelectorArgument.OnePlayer playerArg = new EntitySelectorArgument.OnePlayer("player");
		MultiLiteralArgument selectionArg = new MultiLiteralArgument("selection", StatTrackItem.OPTIONS.keySet().toArray(String[]::new));
		IntegerArgument amountArg = new IntegerArgument("amount", 1);

		new CommandAPICommand("stattrackadd")
			.withPermission("monumenta.command.stattrackadd")
			.withArguments(playerArg)
			.withArguments(selectionArg)
			.withOptionalArguments(amountArg)
			.executes((sender, args) -> {
				Player player = args.getByArgument(playerArg);
				ItemStack item = player.getInventory().getItemInMainHand();
				InfusionType selection = StatTrackItem.OPTIONS.get(args.getByArgument(selectionArg));
				if (selection == null) {
					throw CommandAPI.failWithString("Invalid stat selection; how did we get here?");
				}
				int amount = args.getByArgumentOrDefault(amountArg, 1);
				StatTrackManager.getInstance().incrementStatImmediately(item, player, selection, amount);
			}).register();
	}
}
