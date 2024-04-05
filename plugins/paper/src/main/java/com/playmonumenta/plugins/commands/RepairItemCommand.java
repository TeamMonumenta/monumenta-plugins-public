package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.itemstats.infusions.Shattered;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.CommandPermission;
import dev.jorel.commandapi.arguments.EntitySelectorArgument;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

/**
 * RepairItemCommand (Last written by PikaLegend_ 2.04.2024)
 * /repairitem is a command for repairing an item entity or held item by one shattered level or durability
 * (Utilized in Item Repair Station mechs)
 * <p>
 * Syntax: /repairitem [entity]
 * <p>
 * By default, it assumes a player is holding an item, and thus would attempt to repair that item.
 * If entity is specified and is an Item Entity, it will repair the item data within that entity.
 */
public class RepairItemCommand {
	private static final String COMMAND = "repairitem";
	private static final String PERMISSION = "monumenta.command.repairitem";

	public static void register() {
		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString(PERMISSION))
			.withArguments(
				new EntitySelectorArgument.OneEntity("entity")
			)
			.executes((sender, args) -> {
				try {
					if (args[0] instanceof Item itemEntity) {
						ItemStack itemStack = itemEntity.getItemStack();
						repairItem(itemStack);
					} else if (args[0] instanceof Player player) {
						ItemStack itemStack = player.getInventory().getItemInMainHand();
						repairItem(itemStack);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			})
			.register();

		new CommandAPICommand(COMMAND)
			.withPermission(CommandPermission.fromString(PERMISSION))
			.executes((sender, args) -> {
				try {
					if (sender instanceof Player player) {
						ItemStack itemStack = player.getInventory().getItemInMainHand();
						repairItem(itemStack);
					}
				} catch (Exception e) {
					e.printStackTrace();
				}
			})
			.register();
	}

	public static void repairItem(ItemStack item) {
		if (item != null) {
			boolean unshattered = Shattered.unshatterOneLevel(item);

			if (!unshattered
				&& item.getItemMeta() instanceof Damageable damageable
				&& damageable.getDamage() > 0) {
				damageable.setDamage(0);
				item.setItemMeta(damageable);
			}
		}
	}
}
