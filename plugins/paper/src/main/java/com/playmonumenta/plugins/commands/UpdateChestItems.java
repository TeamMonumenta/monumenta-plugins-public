package com.playmonumenta.plugins.commands;

import com.playmonumenta.plugins.utils.ItemStatUtils;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class UpdateChestItems extends GenericCommand {
	public static void register() {
		registerPlayerCommand("updatechestitems", "monumenta.command.updatechestitems", UpdateChestItems::run);
	}

	public static void run(CommandSender sender, Player player) {
		if (((Player) sender).getGameMode() != GameMode.CREATIVE) {
			return;
		}
		Block b = player.getTargetBlock(10);
		if (b.getType() == Material.CHEST) {
			Chest chest = (Chest) b.getState();
			for (ItemStack item : chest.getInventory().getContents()) {
				ItemStatUtils.generateItemStats(item);
			}
		}
	}
}
