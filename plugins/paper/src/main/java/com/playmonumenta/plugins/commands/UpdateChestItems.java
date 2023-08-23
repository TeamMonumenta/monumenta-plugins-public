package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MMLog;
import dev.jorel.commandapi.CommandAPICommand;
import dev.jorel.commandapi.arguments.LocationArgument;
import dev.jorel.commandapi.arguments.LocationType;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;

public class UpdateChestItems extends GenericCommand {
	public static void register() {
		new CommandAPICommand("updatechestitems")
			.withPermission("monumenta.command.updatechestitems")
			.withArguments(
				new LocationArgument("pos", LocationType.BLOCK_POSITION)
			)
			.executes((sender, args) -> {
				run(sender, (Location) args[0]);
			})
			.register();

	}

	public static void run(CommandSender sender, Location loc) {
		Block b = loc.getBlock();
		if (b.getType() == Material.CHEST) {
			Chest chest = (Chest) b.getState();
			loc = loc.add(0.5, 1, 0.5); // get center of block above chest
			String pos = "(" + loc.getX() + " " + loc.getY() + " " + loc.getZ() + ")";
			int slot = 0;
			for (ItemStack item : chest.getInventory().getContents()) {
				slot++;
				if (ItemUtils.isNullOrAir(item)) {
					continue;
				}
				try {
					ItemUpdateHelper.fixLegacies(item);
					ItemUpdateHelper.generateItemStats(item);
					String errorFound = ItemUpdateHelper.checkForErrors(item);
					if (errorFound != null) {
						errorFound = "Chest: '" + ItemUtils.toPlainTagText(chest.customName()) + "' Slot: " + slot + " at: " + pos + " " + errorFound;
						MMLog.warning(errorFound);
					}
				} catch (Exception e) {
					MMLog.severe("Item Update Error at Slot: #" + slot + " at: " + pos + " " + ItemUtils.getGiveCommand(item), e);
				}
			}
		}
	}
}
