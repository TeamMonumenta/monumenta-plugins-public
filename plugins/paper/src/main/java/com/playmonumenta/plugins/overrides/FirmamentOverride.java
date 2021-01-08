package com.playmonumenta.plugins.overrides;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.ShulkerBox;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
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
		return changeMode(item, player);
	}

	@Override
	public boolean leftClickBlockInteraction(Plugin plugin, Player player, Action action, ItemStack item, Block block) {
		return changeMode(item, player);
	}

	@Override
	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		if (isFirmamentItem(dispensed)) {
			block.getWorld().playSound(block.getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			return false;
		}
		return true;
	}

	private boolean placeBlock(Player player, ItemStack item, BlockPlaceEvent event) {
		if (!isFirmamentItem(item)) {
			//Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe - hopefully other shulkers with lore wont get placed
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
			if (currentItem == null || currentItem.getType().isAir() || ItemUtils.notAllowedTreeReplace.contains(currentItem.getType()) || (!currentItem.getType().isOccluding() && !ItemUtils.GOOD_OCCLUDERS.contains(currentItem.getType()))) {
				// Air breaks it, skip over it. Also the banned items break it, skip over those.
				continue;
			}

			// Safety
			if (!Plugin.getInstance().mItemOverrides.blockPlaceInteraction(Plugin.getInstance(), player, currentItem, event)) {
				return false;
			}

			ItemMeta meta = currentItem.getItemMeta();
			// No known way to preserve BlockStateMeta - so check that it's either null or simple BlockDataMeta
			if (currentItem.getType().isBlock() && (meta == null || meta instanceof BlockDataMeta)) {
				BlockState state = event.getBlockReplacedState();
				if (FastUtils.RANDOM.nextBoolean() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().contains(ChatColor.AQUA + "Prismarine " + ChatColor.GREEN + "Enabled")) {
					// Place a prismarine block instead of the block from the shulker
					BlockData blockData = Material.PRISMARINE.createBlockData();
					state.setBlockData(blockData);

					// Forcibly update the new block state and apply physics
					state.update(true, true);
					state.setType(Material.PRISMARINE);
					//Log the placement of prismarine
					CoreProtectIntegration.logPlacement(player, event.getBlock().getLocation(), Material.PRISMARINE, blockData);
					// No changes needed to the shulker, exit here and cancel the event
					return false;
				}

				BlockData blockData = currentItem.getType().createBlockData();

				if (meta == null) {
					// If no block data (simple block), create default
					state.setBlockData(blockData);
				} else if (meta instanceof BlockDataMeta) {
					// If some block data (complex block), use it
					BlockDataMeta blockMeta = (BlockDataMeta)meta;
					if (blockMeta.hasBlockData()) {
						//Change blockData if the meta has some already
						blockData = blockMeta.getBlockData(currentItem.getType());
						state.setBlockData(blockData);
					} else {
						state.setBlockData(blockData);
					}
				}
				//Log the placement of the blocks
				CoreProtectIntegration.logPlacement(player, event.getBlock().getLocation(), currentItem.getType(), blockData);
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

	private boolean changeMode(ItemStack item, Player player) {
		if (!isFirmamentItem(item)) {
			//Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe
			return true;
		}

		if (!player.isSneaking()) {
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
			if (loreEntry.equals(lore.get(lore.size() - 1)) && !foundLine) {
				newLore.add(PRISMARINE_ENABLED);
			}
		}
		ItemMeta meta = item.getItemMeta();
		meta.setLore(newLore);
		item.setItemMeta(meta);
		player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, SoundCategory.BLOCKS, 1, 1);
		return true;
	}

	public static boolean isFirmamentItem(ItemStack item) {
		return item != null &&
		       item.getType() != null &&
		       InventoryUtils.testForItemWithName(item, ITEM_NAME) &&
		       InventoryUtils.testForItemWithLore(item, "City of Shifting Waters") &&
		       ItemUtils.getItemTier(item).equals(ItemTier.EPIC) &&
		       ItemUtils.isShulkerBox(item.getType());
	}
}
