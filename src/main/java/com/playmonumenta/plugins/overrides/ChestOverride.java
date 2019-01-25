package com.playmonumenta.plugins.overrides;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.CommandBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ChestUtils;

public class ChestOverride extends BaseOverride {
	@Override
	public boolean rightClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		if (!command_chest(block)) {
			return false;
		}

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
		if (!command_chest(block)) {
			return false;
		}
		if ((player.getGameMode() == GameMode.CREATIVE) || _breakable(block)) {
			return true;
		}
		return false;
	}

	@Override
	public boolean blockExplodeInteraction(Plugin plugin, Block block) {
		if (!command_chest(block)) {
			return false;
		}
		return _breakable(block);
	}

	protected static boolean _breakable(Block block) {
		Block blockUnder = block.getLocation().add(0, -1, 0).getBlock();
		if (blockUnder != null && blockUnder.getType() == Material.BARRIER) {
			return false;
		}
		return true;
	}

	// If this returns false, the caller should also return false and stop processing the chest
	private boolean command_chest(Block block) {
		BlockState state = block.getState();
		if (state != null && state instanceof Chest) {
			Chest chest = (Chest)state;
			String name = chest.getCustomName();
			if (name != null && (name.toLowerCase().equals("trap") || name.toLowerCase().equals("function"))) {
				// This is a function chest - run it!
				// Run the first command block found in the 10 blocks under this block
				Location loc = block.getLocation();
				for (int y = 0; y < 10; y++) {
					loc = loc.subtract(0, 1, 0);
					Block testBlock = loc.getBlock();
					BlockState testState = testBlock.getState();

					if (testBlock.getType().equals(Material.COMMAND_BLOCK)
					    && testState instanceof CommandBlock) {

						// Run the command positioned at the chest block
						String command = "execute positioned " +
						                 Integer.toString((int)chest.getLocation().getX()) + " " +
						                 Integer.toString((int)chest.getLocation().getY()) + " " +
						                 Integer.toString((int)chest.getLocation().getZ()) + " run " +
						                 ((CommandBlock)testState).getCommand();
						Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
						break;
					}
				}

				if (name.toLowerCase().equals("trap")) {
					// This was a trapped chest - clear its name and still let the player open it
					chest.setCustomName(null);
					chest.update();
					return true;
				} else {
					// This was a function chest - don't let the player open it
					return false;
				}
			}
		}
		return true;
	}
}
