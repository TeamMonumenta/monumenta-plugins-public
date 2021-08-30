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
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.enchantments.StatTrack.StatTrackOptions;
import com.playmonumenta.plugins.enchantments.StatTrackManager;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ItemUtils.ItemTier;

public class FirmamentOverride extends BaseOverride {
	private static final String CAN_PLACE_SHULKER_PERM = "monumenta.canplaceshulker";

	private static final String PRISMARINE_ENABLED = ChatColor.AQUA + "Prismarine " + ChatColor.GREEN + "Enabled";
	private static final String PRISMARINE_DISABLED = ChatColor.AQUA + "Prismarine " + ChatColor.RED + "Disabled";
	private static final String ITEM_NAME = "Firmament";
	private static final String DELVE_SKIN_NAME = "Doorway from Eternity";
	private static final String BLACKSTONE_ENABLED = ChatColor.GRAY + "Blackstone " + ChatColor.GREEN + "Enabled";
	private static final String BLACKSTONE_DISABLED = ChatColor.GRAY + "Blackstone " + ChatColor.RED + "Disabled";

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

		Player nearbyPlayer = EntityUtils.getNearestPlayer(block.getLocation(), 10);
		if (nearbyPlayer != null) {
			// Check permission to enable placing shulkers, just so this can be turned off via perms if needed
			return nearbyPlayer.hasPermission(CAN_PLACE_SHULKER_PERM);
		}
		return false; // Don't allow shulkers to be placed by dispensers if no player is nearby
	}

	private boolean placeBlock(Player player, ItemStack item, BlockPlaceEvent event) {
		if (!isFirmamentItem(item)) {
			//Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe - hopefully other shulkers with lore wont get placed
			// Check permission to enable placing shulkers, just so this can be turned off via perms if needed
			return player.hasPermission(CAN_PLACE_SHULKER_PERM);
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
			if (currentItem == null
				|| currentItem.getType().isAir()
				|| ItemUtils.notAllowedTreeReplace.contains(currentItem.getType())
				|| (!currentItem.getType().isOccluding() && !ItemUtils.GOOD_OCCLUDERS.contains(currentItem.getType()))
				|| (currentItem.getItemMeta().hasLore() && !currentItem.getItemMeta().lore().isEmpty())) {
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
				//Stat tracking for firmament
				StatTrackManager.incrementStat(item, player, StatTrackOptions.BLOCKS_PLACED, 1);
				BlockState state = event.getBlockReplacedState();
				if (FastUtils.RANDOM.nextBoolean()
						&& item.getItemMeta().hasLore()
						&& (item.getItemMeta().getLore().contains(PRISMARINE_ENABLED) || item.getItemMeta().getLore().contains(BLACKSTONE_ENABLED))) {

					// Place a prismarine block instead of the block from the shulker
					BlockData blockData = null;
					if (InventoryUtils.testForItemWithName(item, ITEM_NAME)) {
						blockData = Material.PRISMARINE.createBlockData();
						state.setBlockData(blockData);

						// Forcibly update the new block state and apply physics
						state.update(true, true);
						state.setType(Material.PRISMARINE);
						//Log the placement
						CoreProtectIntegration.logPlacement(player, event.getBlock().getLocation(), Material.PRISMARINE, blockData);
					} else {
						blockData = Material.BLACKSTONE.createBlockData();
						state.setBlockData(blockData);

						// Forcibly update the new block state and apply physics
						state.update(true, true);
						state.setType(Material.BLACKSTONE);
						//Log the placement
						CoreProtectIntegration.logPlacement(player, event.getBlock().getLocation(), Material.BLACKSTONE, blockData);
					}
					// No changes needed to the shulker, exit here and cancel the event
					return false;
				}

				BlockData blockData = currentItem.getType().createBlockData();
				if (blockData instanceof Leaves) {
					((Leaves)blockData).setPersistent(true);
				}

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
		if (InventoryUtils.testForItemWithName(item, ITEM_NAME)) {
			for (String loreEntry : lore) {
				if (loreEntry.equals(PRISMARINE_ENABLED) && !foundLine) {
					newLore.add(PRISMARINE_DISABLED);
					player.sendMessage(PRISMARINE_DISABLED);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, SoundCategory.BLOCKS, 1, 1);
					foundLine = true;
					continue;
				} else if (loreEntry.equals(PRISMARINE_DISABLED) && !foundLine) {
					newLore.add(PRISMARINE_ENABLED);
					player.sendMessage(PRISMARINE_ENABLED);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
					foundLine = true;
					continue;
				}
				newLore.add(loreEntry);
				if (loreEntry.equals(lore.get(lore.size() - 1)) && !foundLine) {
					newLore.add(PRISMARINE_ENABLED);
					player.sendMessage(PRISMARINE_ENABLED);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
				}
			}
		} else if (InventoryUtils.testForItemWithName(item, DELVE_SKIN_NAME)) {
			for (String loreEntry : lore) {
				if (loreEntry.equals(BLACKSTONE_ENABLED) && !foundLine) {
					newLore.add(BLACKSTONE_DISABLED);
					player.sendMessage(BLACKSTONE_DISABLED);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, SoundCategory.BLOCKS, 1, 1);
					foundLine = true;
					continue;
				} else if (loreEntry.equals(BLACKSTONE_DISABLED) && !foundLine) {
					newLore.add(BLACKSTONE_ENABLED);
					player.sendMessage(BLACKSTONE_ENABLED);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
					foundLine = true;
					continue;
				}
				newLore.add(loreEntry);
				if (loreEntry.equals(lore.get(lore.size() - 1)) && !foundLine) {
					newLore.add(BLACKSTONE_ENABLED);
					player.sendMessage(BLACKSTONE_ENABLED);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
				}
			}
		}
		ItemMeta meta = item.getItemMeta();
		meta.setLore(newLore);
		item.setItemMeta(meta);
		ItemUtils.setPlainLore(item);
		return true;
	}

	public static boolean isFirmamentItem(ItemStack item) {
		return item != null &&
		       item.getType() != null &&
		       (InventoryUtils.testForItemWithName(item, ITEM_NAME) || InventoryUtils.testForItemWithName(item, DELVE_SKIN_NAME)) &&
		       (InventoryUtils.testForItemWithLore(item, "City of Shifting Waters") || InventoryUtils.testForItemWithLore(item, "Mythic Reliquary")) &&
		       ItemUtils.getItemTier(item).equals(ItemTier.EPIC) &&
		       ItemUtils.isShulkerBox(item.getType());
	}
}
