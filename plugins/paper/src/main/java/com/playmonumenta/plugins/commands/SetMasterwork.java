package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.Masterwork;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NamespacedKeyUtils;
import de.tr7zw.nbtapi.NBTCompound;
import de.tr7zw.nbtapi.NBTItem;
import dev.jorel.commandapi.CommandAPI;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.Argument;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import dev.jorel.commandapi.arguments.MultiLiteralArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import dev.jorel.commandapi.exceptions.WrapperCommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.CrossbowMeta;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;

public class SetMasterwork extends GenericCommand {
	public static void register() {
		CommandPermission perms = CommandPermission.fromString("monumenta.command.setmasterwork");

		String[] options = new String[2];
		options[0] = "upgrade";
		options[1] = "set";
		Argument selectionArg = new MultiLiteralArgument(options);

		Masterwork[] masterworkRaw = Masterwork.values();
		String[] ms = new String[masterworkRaw.length];
		for (int i = 0; i < ms.length; i++) {
			ms[i] = masterworkRaw[i].getName();
		}

		List<Argument> arguments = new ArrayList<>();
		arguments.add(new EntitySelectorArgument("player", EntitySelectorArgument.EntitySelector.ONE_PLAYER));
		arguments.add(selectionArg);
		arguments.add(new StringArgument("level").overrideSuggestions(ms));

		new CommandAPICommand("setmasterwork")
			.withPermission(perms)
			.withArguments(arguments)
			.executes((sender, args) -> {
				if (args[1].equals("upgrade")) {
					upgrade((Player) args[0]);
				} else if (args[1].equals("set")) {
					Masterwork newLevel = Masterwork.getMasterwork((String) args[2]);
					if (newLevel == null) {
						CommandAPI.fail("Invalid level selection; how did we get here?");
						return;
					}
					run((Player) args[0], newLevel);
				} else {
					CommandAPI.fail("Invalid arg selection; how did we get here?");
				}
			})
			.register();
	}

	public static void run(Player player, Masterwork level) throws WrapperCommandSyntaxException {
		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item == null || item.getAmount() <= 0) {
			CommandAPI.fail("Player must have a valid item in their main hand!");
		}

		Masterwork masterworkLevel = ItemStatUtils.getMasterwork(item);
		if (masterworkLevel == Masterwork.NONE || masterworkLevel == null || masterworkLevel == Masterwork.ERROR) {
			CommandAPI.fail("Player must have a valid item in their main hand!");
		}

		String itemName = ItemUtils.getPlainName(item).toLowerCase();
		itemName = itemName.replaceAll("[\\W]+", "");

		ItemStack newItem = InventoryUtils.getItemFromLootTable(player, NamespacedKeyUtils.fromString("epic:r3/items/masterwork/" + itemName + "/" + itemName + "_" + level.getName()));
		if (newItem == null) {
			CommandAPI.fail("Invalid loot table found! Please submit a bug report.");
		}

		// Modify the result item to carry over player modifications (infusions etc.)
		NBTItem playerItemNbt = new NBTItem(item);
		NBTItem newItemNbt = new NBTItem(newItem);
		NBTCompound playerModified = ItemStatUtils.getPlayerModified(playerItemNbt);

		ItemStatUtils.addPlayerModified(newItemNbt).mergeCompound(playerModified);
		newItem = newItemNbt.getItem();

		// Carry over the durability
		if (newItem.getItemMeta() instanceof Damageable newResultMeta && item.getItemMeta() instanceof Damageable playerItemMeta) {
			newResultMeta.setDamage(playerItemMeta.getDamage());
			newItem.setItemMeta((ItemMeta) newResultMeta);
		}

		// Carry over the current arrow of a crossbow if the player item has an arrow
		if (newItem.getItemMeta() instanceof CrossbowMeta newResultMeta && item.getItemMeta() instanceof CrossbowMeta playerItemMeta
			&& !newResultMeta.hasChargedProjectiles() && playerItemMeta.hasChargedProjectiles()) {
			newResultMeta.setChargedProjectiles(playerItemMeta.getChargedProjectiles());
			newItem.setItemMeta(newResultMeta);
		}

		ItemStatUtils.generateItemStats(newItem);
		player.getInventory().setItemInHand(newItem);
		player.updateInventory();
	}

	public static void upgrade(Player player) throws WrapperCommandSyntaxException {
		ItemStack item = player.getEquipment().getItemInMainHand();
		if (item == null || item.getAmount() <= 0) {
			CommandAPI.fail("Player must have a valid item in their main hand!");
		}

		Masterwork masterworkLevel = ItemStatUtils.getMasterwork(item);
		if (masterworkLevel == Masterwork.NONE || masterworkLevel == null || masterworkLevel == Masterwork.ERROR) {
			CommandAPI.fail("Player must have a valid item in their main hand!");
		} else if (masterworkLevel == Masterwork.II) {
			CommandAPI.fail("The selected item is already at the highest level possible!");
		}

		run(player, Masterwork.getMasterwork(Integer.toString(Integer.parseInt(masterworkLevel.getName()) + 1)));
	}
}
