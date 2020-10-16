package com.playmonumenta.plugins.overrides;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ItemUtils.ItemTier;

public class FirmamentOverride extends BaseOverride {

	private static final String PRISMARINE_ENABLED = ChatColor.AQUA + "Prismarine " + ChatColor.GREEN + "Enabled";
	private static final String PRISMARINE_DISABLED = ChatColor.AQUA + "Prismarine " + ChatColor.RED + "Disabled";
	private static final String ITEM_NAME = "Firmament";

	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		return placeBlock(player, item, event);
	}

	@Override
	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return changeMode(item);
	}

	@Override
	public boolean leftClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return changeMode(item);
	}

	private boolean placeBlock(Player player, ItemStack item, BlockPlaceEvent event) {
		if (!isFirmamentItem(item)) {
			//S omehow triggered when it wasn't the right item - shouldn't prevent the event to be safe - hopefully other shulkers with lore wont get placed
			return true;
		}
		if (!player.hasPermission("monumenta.firmament")) {
			player.sendMessage(ChatColor.RED + "You don't have permission to use this item. Please ask a moderator to fix this.");
			return false;
		}
		BlockStateMeta shulkerMeta = (BlockStateMeta)item.getItemMeta();
		ShulkerBox shulkerBox = (ShulkerBox)shulkerMeta.getBlockState();
		Inventory shulkerInventory = shulkerBox.getInventory();
		for (int i = 0; i < 27; i++) {
			ItemStack currentItem = shulkerInventory.getItem(i);
			if (currentItem == null || currentItem.getType().isAir()) {
				// Air breaks it, skip over it
				continue;
			}
			ItemMeta meta = currentItem.getItemMeta();
			// No known way to preserve BlockStateMeta - so check that it's either null or simple BlockDataMeta
			if (currentItem.getType().isBlock() && (meta == null || meta instanceof BlockDataMeta)) {
				BlockState state = event.getBlockReplacedState();
				if (FastUtils.RANDOM.nextBoolean() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().get(item.getItemMeta().getLore().size() - 1).contains("Enabled")) {
					// Place a prismarine block instead of the block from the shulker
					state.setBlockData(Material.PRISMARINE.createBlockData());

					// Forcibly update the new block state and apply physics
					state.update(true, true);
					state.setType(Material.PRISMARINE);
					// No changes needed to the shulker, exit here and cancel the event
					return false;
				}

				if (meta == null) {
					// If no block data (simple block), create default
					state.setBlockData(currentItem.getType().createBlockData());
				} else if (meta instanceof BlockDataMeta) {
					// If some block data (complex block), use it
					BlockDataMeta blockMeta = (BlockDataMeta)meta;
					if (blockMeta.hasBlockData()) {
						state.setBlockData(blockMeta.getBlockData(currentItem.getType()));
					} else {
						state.setBlockData(currentItem.getType().createBlockData());
					}
				}

				// Forcibly update the new block state and apply physics
				state.update(true, true);
				// Set the type of the block
				state.setType(currentItem.getType());
				// Update the shulker's inventory and abort
				shulkerInventory.setItem(i, currentItem.subtract());
				shulkerMeta.setBlockState(shulkerBox);
				item.setItemMeta(shulkerMeta);
				return false;
			}
		}
		player.sendMessage(ChatColor.RED + "There are no valid blocks to place in the shulker!");
		return false;
	}

	private boolean changeMode(ItemStack item) {
		if (!isFirmamentItem(item)) {
			//Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe
			return true;
		}

		List<String> lore = item.getItemMeta().getLore();
		List<String> newLore = new ArrayList<>();
		boolean foundLine = false;

		for (String loreEntry : lore) {
			if (loreEntry.equals(PRISMARINE_ENABLED) && !foundLine) {
				newLore.add(PRISMARINE_DISABLED);
				foundLine = true;
				continue;
			} else if (loreEntry.equals(PRISMARINE_DISABLED) && !foundLine) {
				newLore.add(PRISMARINE_ENABLED);
				foundLine = true;
				continue;
			}
			newLore.add(loreEntry);
			if (loreEntry.equals(lore.get(lore.size() - 1))) {
				newLore.add(PRISMARINE_ENABLED);
			}
		}
		ItemMeta meta = item.getItemMeta();
		meta.setLore(newLore);
		item.setItemMeta(meta);
		return true;
	}

	private boolean isFirmamentItem(ItemStack item) {
		return item != null &&
		       item.getType() != null &&
		       InventoryUtils.testForItemWithName(item, ITEM_NAME) &&
		       InventoryUtils.testForItemWithLore(item, "City of Shifting Waters") &&
		       ItemUtils.getItemTier(item).equals(ItemTier.EPIC) &&
		       ItemUtils.isShulkerBox(item.getType());
	}
}
