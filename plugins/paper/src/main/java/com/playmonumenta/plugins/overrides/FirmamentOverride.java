package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.protocollib.FirmamentLagFix;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.Tier;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MessagingUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import de.tr7zw.nbtapi.NBTItem;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
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
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockDataMeta;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class FirmamentOverride extends BaseOverride {
	private static final String CAN_PLACE_SHULKER_PERM = "monumenta.canplaceshulker";

	private static final Component PRISMARINE_ENABLED = Component.text("Prismarine ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		.append(Component.text("Enabled").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
	private static final Component PRISMARINE_DISABLED = Component.text("Prismarine ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		.append(Component.text("Disabled").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
	private static final Component BLACKSTONE_ENABLED = Component.text("Blackstone ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		.append(Component.text("Enabled").color(NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
	private static final Component BLACKSTONE_DISABLED = Component.text("Blackstone ").color(NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false)
		.append(Component.text("Disabled").color(NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
	private static final String PLAIN_PRISMARINE_ENABLED = MessagingUtils.plainText(PRISMARINE_ENABLED);
	private static final String PLAIN_PRISMARINE_DISABLED = MessagingUtils.plainText(PRISMARINE_DISABLED);
	private static final String PLAIN_BLACKSTONE_ENABLED = MessagingUtils.plainText(BLACKSTONE_ENABLED);
	private static final String PLAIN_BLACKSTONE_DISABLED = MessagingUtils.plainText(BLACKSTONE_DISABLED);
	private static final String ITEM_NAME = "Firmament";
	private static final String DELVE_SKIN_NAME = "Doorway from Eternity";

	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		return placeBlock(player, item, event);
	}

	@Override
	public boolean swapHandsInteraction(Plugin plugin, Player player, ItemStack item) {
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
			// Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe - hopefully other shulkers with lore won't get placed
			// Check permission to enable placing shulkers, just so this can be turned off via perms if needed
			return player.hasPermission(CAN_PLACE_SHULKER_PERM);
		}
		if (!player.hasPermission("monumenta.firmament")) {
			player.sendMessage(ChatColor.RED + "You don't have permission to use this item. Please ask a moderator to fix this.");
			return false;
		}
		if (!ZoneUtils.playerCanInteractWithBlock(player, event.getBlock())) {
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
					|| currentItem.getItemMeta().hasLore()) {
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
				StatTrackManager.incrementStat(item, player, ItemStatUtils.InfusionType.STAT_TRACK_BLOCKS, 1);

				BlockData blockData;
				boolean removeItem = true;
				if (FastUtils.RANDOM.nextBoolean()
					    && item.getItemMeta().hasLore()
					    && (InventoryUtils.testForItemWithLore(item, PLAIN_PRISMARINE_ENABLED) || InventoryUtils.testForItemWithLore(item, PLAIN_BLACKSTONE_ENABLED))) {
					removeItem = false;
					// Place a prismarine/blackstone block instead of the block from the shulker
					if (InventoryUtils.testForItemWithName(item, ITEM_NAME)) {
						blockData = Material.PRISMARINE.createBlockData();
					} else {
						blockData = Material.BLACKSTONE.createBlockData();
					}
				} else {
					// Use block data from meta if the meta has some already
					if (meta instanceof BlockDataMeta blockMeta && blockMeta.hasBlockData()) {
						blockData = blockMeta.getBlockData(currentItem.getType());
					} else {
						blockData = currentItem.getType().createBlockData();
						if (blockData instanceof Leaves) {
							((Leaves) blockData).setPersistent(true);
						}
					}
				}

				// Place the chosen block instead of the Firmament
				// This is done by setting the "replaced" block state to the desired block state, and then cancelling the event, which will "revert" the block to this state
				event.getBlockReplacedState().setBlockData(blockData);

				// Log the placement of the block
				CoreProtectIntegration.logPlacement(player, event.getBlock().getLocation(), blockData.getMaterial(), blockData);

				// Update the Shulker's inventory unless it was a free placement
				if (removeItem) {
					shulkerInventory.setItem(i, currentItem.subtract());
					shulkerMeta.setBlockState(shulkerBox);
					item.setItemMeta(shulkerMeta);
				}

				// Prevent sending block update packets for neighbors of the placed block
				FirmamentLagFix.firmamentUsed(event.getBlock());

				// Force update physics on the placed block
				Bukkit.getScheduler().runTask(Plugin.getInstance(), () -> {
					BlockState state = event.getBlock().getState();
					if (state.getBlockData().equals(blockData)) {
						event.getBlock().setType(Material.AIR, false);
						state.update(true, true);
					}
				});

				// Cancel the event
				return false;
			}
		}
		player.sendMessage(ChatColor.RED + "There are no valid blocks to place in the shulker!");
		return false;
	}

	private boolean changeMode(ItemStack item, Player player) {
		if (!isFirmamentItem(item)) {
			//Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe
			return false;
		}

		NBTItem nbt = new NBTItem(item);
		List<String> lore = ItemStatUtils.getPlainLore(nbt);

		boolean foundLine = false;
		if (InventoryUtils.testForItemWithName(item, ITEM_NAME)) {
			for (int i = 0; i < lore.size(); ++i) {
				String line = lore.get(i);
				if (line.equals(PLAIN_PRISMARINE_ENABLED) && !foundLine) {
					ItemStatUtils.removeLore(item, i);
					ItemStatUtils.addLore(item, i, PRISMARINE_DISABLED);
					player.sendMessage(PRISMARINE_DISABLED);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, SoundCategory.BLOCKS, 1, 1);
					foundLine = true;
					break;
				} else if (line.equals(PLAIN_PRISMARINE_DISABLED) && !foundLine) {
					ItemStatUtils.removeLore(item, i);
					ItemStatUtils.addLore(item, i, PRISMARINE_ENABLED);
					player.sendMessage(PRISMARINE_ENABLED);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
					foundLine = true;
					break;
				}
			}
			if (!foundLine) {
				ItemStatUtils.addLore(item, lore.size(), PRISMARINE_ENABLED);
				player.sendMessage(PRISMARINE_ENABLED);
				player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
			}
		} else if (InventoryUtils.testForItemWithName(item, DELVE_SKIN_NAME)) {
			for (int i = 0; i < lore.size(); ++i) {
				String line = lore.get(i);
				if (line.equals(PLAIN_BLACKSTONE_ENABLED) && !foundLine) {
					ItemStatUtils.removeLore(item, i);
					ItemStatUtils.addLore(item, i, BLACKSTONE_DISABLED);
					player.sendMessage(BLACKSTONE_DISABLED);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_CLOSE, SoundCategory.BLOCKS, 1, 1);
					foundLine = true;
					break;
				} else if (line.equals(PLAIN_BLACKSTONE_DISABLED) && !foundLine) {
					ItemStatUtils.removeLore(item, i);
					ItemStatUtils.addLore(item, i, BLACKSTONE_ENABLED);
					player.sendMessage(BLACKSTONE_ENABLED);
					player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
					foundLine = true;
					break;
				}
			}
			if (!foundLine) {
				ItemStatUtils.addLore(item, lore.size(), BLACKSTONE_ENABLED);
				player.sendMessage(BLACKSTONE_ENABLED);
				player.playSound(player.getLocation(), Sound.BLOCK_SHULKER_BOX_OPEN, SoundCategory.BLOCKS, 1, 1);
			}
		}
		ItemStatUtils.generateItemStats(item);
		return true;
	}

	public static boolean isFirmamentItem(ItemStack item) {
		return item != null &&
		       item.getType() != null &&
		       (InventoryUtils.testForItemWithName(item, ITEM_NAME) || InventoryUtils.testForItemWithName(item, DELVE_SKIN_NAME)) &&
		       (InventoryUtils.testForItemWithLore(item, "City of Shifting Waters") || InventoryUtils.testForItemWithLore(item, "Mythic Reliquary")) &&
		       ItemStatUtils.getTier(item).equals(Tier.EPIC) &&
		       ItemUtils.isShulkerBox(item.getType());
	}
}
