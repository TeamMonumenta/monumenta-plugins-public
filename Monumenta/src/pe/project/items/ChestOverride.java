package pe.project.items;

import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.GameMode;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Material;

import pe.project.Plugin;
import pe.project.utils.ChestUtils;

public class ChestOverride extends OverrideItem {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (player != null && player.getGameMode() != GameMode.SPECTATOR) {
			ChestUtils.chestScalingLuck(plugin, player, block);
		}

		if (player == null || player.getGameMode() != GameMode.SPECTATOR) {
			return true;
		}

		BlockState state = block.getState();
		if (state instanceof Chest) {
			Chest chest = (Chest)state;
			Inventory inv = chest.getBlockInventory();
			ItemStack[] items = inv.getContents();

			int count = 0;
			for (ItemStack it : items) {
				if (it != null) {
					count++;
				}
			}

			if (count == 0) {
				player.sendMessage(ChatColor.GOLD + "This chest is empty or has a loot table!");
				return false;
			}
		}

		return true;
	}

	/* Chests placed on barriers can not be broken */
	@Override
	public boolean blockBreakInteraction(Plugin plugin, Player player, Block block) {
		if ((player.getGameMode() == GameMode.CREATIVE) || _breakable(block)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		return _breakable(block);
	}

	private boolean _breakable(Block block) {
		Block blockUnder = block.getLocation().add(0, -1, 0).getBlock();
		if (blockUnder != null && blockUnder.getType() == Material.BARRIER) {
			return false;
		}

		return true;
	}
}
