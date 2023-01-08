package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.Masterwork;
import com.playmonumenta.plugins.utils.MasterworkUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class SetMasterwork extends GenericCommand {
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.setmasterwork");

		String[] options = new String[2];
		options[0] = "upgrade";
		options[1] = "set";
		Argument<?> selectionArg = new MultiLiteralArgument(options);

		Masterwork[] masterworkRaw = Masterwork.values();
		String[] ms = new String[masterworkRaw.length];
		for (int i = 0; i < ms.length; i++) {
			ms[i] = masterworkRaw[i].getName();
		}

		List<Argument<?>> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument.OnePlayer("player"));
		arguments.add(selectionArg);
		arguments.add(new StringArgument("level").replaceSuggestions(ArgumentSuggestions.strings(ms)));

		new CommandAPICommand("setmasterwork")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (args[1].equals("upgrade")) {
					upgrade((Player) args[0]);
				} else if (args[1].equals("set")) {
					Masterwork newLevel = Masterwork.getMasterwork((String) args[2]);
					if (newLevel == null) {
						throw CommandAPI.failWithString("Invalid level selection; how did we get here?");
					}
					run((Player) args[0], newLevel);
				} else {
					throw CommandAPI.failWithString("Invalid arg selection; how did we get here?");
				}
			})
			.register();
	}

	public static void run(Player player, Masterwork level) throws WrapperCommandSyntaxException {
		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item == null || item.getAmount() <= 0) {
			throw CommandAPI.failWithString("Player must have a valid item in their main hand!");
		}

		Masterwork masterworkLevel = ItemStatUtils.getMasterwork(item);
		if (masterworkLevel == Masterwork.NONE || masterworkLevel == null || masterworkLevel == Masterwork.ERROR) {
			throw CommandAPI.failWithString("Player must have a valid item in their main hand!");
		}

		MasterworkUtils.getItemPath(item, level);
		ItemStack newItem = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString(MasterworkUtils.getItemPath(item, level)));
		if (newItem == null) {
			throw CommandAPI.failWithString("Invalid loot table found! Please submit a bug report.");
		}

		newItem = MasterworkUtils.preserveModified(item, newItem);
		item.setItemMeta(newItem.getItemMeta());

		player.updateInventory();
	}

	public static void upgrade(Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item == null || item.getAmount() <= 0) {
			throw CommandAPI.failWithString("Player must have a valid item in their main hand!");
		}

		Masterwork masterworkLevel = ItemStatUtils.getMasterwork(item);
		if (masterworkLevel == Masterwork.NONE || masterworkLevel == null || masterworkLevel == Masterwork.ERROR) {
			throw CommandAPI.failWithString("Player must have a valid item in their main hand!");
		} else if (masterworkLevel == Masterwork.II) {
			throw CommandAPI.failWithString("The selected item is already at the highest level possible!");
		}

		run(player, Masterwork.getMasterwork(Integer.toString(Integer.parseInt(masterworkLevel.getName()) + 1)));
	}
}
