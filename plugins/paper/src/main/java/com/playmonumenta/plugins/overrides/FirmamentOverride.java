package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.integrations.CoreProtectIntegration;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.itemstats.enums.Tier;
import com.playmonumenta.plugins.itemstats.infusions.StatTrackManager;
import com.playmonumenta.plugins.itemupdater.ItemUpdateHelper;
import com.playmonumenta.plugins.listeners.RepairExplosionsListener;
import com.playmonumenta.plugins.protocollib.FirmamentLagFix;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import de.tr7zw.nbtapi.NBT;
import de.tr7zw.nbtapi.iface.ReadWriteNBT;
import de.tr7zw.nbtapi.iface.ReadableNBT;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
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
import org.jetbrains.annotations.Nullable;

public class FirmamentOverride {
	public enum FirmamentType {
		PRISMARINE("Prismarine", ITEM_NAME, Material.PRISMARINE),
		BLACKSTONE("Blackstone", DELVE_SKIN_NAME, Material.BLACKSTONE);

		public final String mMaterialName;
		public final String mItemName;
		public final Material mMaterial;

		FirmamentType(String materialName, String itemName, Material material) {
			mMaterialName = materialName;
			mItemName = itemName;
			mMaterial = material;
		}

		public Component getMessage(boolean disabled) {
			Component line = Component.text(mMaterialName + " ", NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false);
			if (disabled) {
				return line.append(Component.text("Disabled", NamedTextColor.RED).decoration(TextDecoration.ITALIC, false));
			} else {
				return line.append(Component.text("Enabled", NamedTextColor.GREEN).decoration(TextDecoration.ITALIC, false));
			}
		}
	}

	private static final String ITEM_NAME = "Firmament";
	private static final String DELVE_SKIN_NAME = "Doorway from Eternity";
	private static final String DISABLED_KEY = "FirmamentPrismarineDisabled";

	public static boolean placeBlock(Player player, ItemStack item, BlockPlaceEvent event) {
		FirmamentType firmamentType = getFirmamentType(item);
		if (firmamentType == null) {
			// Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe - hopefully other shulkers with lore won't get placed
			// Check permission to enable placing shulkers, just so this can be turned off via perms if needed
			return player.hasPermission(Constants.Permissions.CAN_PLACE_SHULKER);
		}
		if (!player.hasPermission("monumenta.firmament")) {
			player.sendMessage(Component.text("You don't have permission to use this item. Please ask a moderator to fix this.", NamedTextColor.RED));
			return false;
		}
		if (!ZoneUtils.playerCanMineBlock(player, event.getBlock())) {
			return false;
		}

		BlockStateMeta shulkerMeta = (BlockStateMeta) item.getItemMeta();
		ShulkerBox shulkerBox = (ShulkerBox) shulkerMeta.getBlockState();
		Inventory shulkerInventory = shulkerBox.getInventory();
		for (int i = 0; i < 27; i++) {
			ItemStack currentItem = shulkerInventory.getItem(i);
			if (currentItem == null
				|| currentItem.getType().isAir()
				|| ItemUtils.notAllowedTreeReplace.contains(currentItem.getType())
				|| (!currentItem.getType().isOccluding() && !ItemUtils.GOOD_OCCLUDERS.contains(currentItem.getType()))
				|| currentItem.getItemMeta().hasLore()) {
				// Air breaks it, skip over it. Also, the banned items break it, skip over those.
				continue;
			}

			// Safety
			if (!Plugin.getInstance().mItemOverrides.blockPlaceInteraction(Plugin.getInstance(), player, currentItem, event)) {
				return false;
			}

			ItemMeta meta = currentItem.getItemMeta();
			// No known way to preserve BlockStateMeta - so check that it's either null or simple BlockDataMeta
			if (currentItem.getType().isBlock() && (meta == null || meta instanceof BlockDataMeta)) {

				BlockData blockData;
				boolean removeItem = true;
				if (FastUtils.RANDOM.nextBoolean() && !isDisabled(item)) {
					removeItem = false;
					// Place a prismarine/blackstone block instead of the block from the shulker
					blockData = firmamentType.mMaterial.createBlockData();
				} else {
					// Use block data from meta if the meta has some already
					if (meta instanceof BlockDataMeta blockMeta && blockMeta.hasBlockData()) {
						blockData = blockMeta.getBlockData(currentItem.getType());
					} else {
						blockData = currentItem.getType().createBlockData();
						if (blockData instanceof Leaves leaves) {
							leaves.setPersistent(true);
						}
					}
				}

				// Log for overworld replacements
				BlockPlaceEvent placeEvent = new BlockPlaceEvent(event.getBlock(), event.getBlockReplacedState(), event.getBlockAgainst(), currentItem, event.getPlayer(), event.canBuild(), event.getHand());
				Bukkit.getPluginManager().callEvent(placeEvent);
				if (!placeEvent.isCancelled()) {
					RepairExplosionsListener.getInstance().playerReplacedBlockViaPlugin(player, event.getBlock());
				}
				placeEvent.getBlockReplacedState().setBlockData(blockData);
				if (!event.isCancelled()) {
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

					//Stat tracking for firmament
					StatTrackManager.getInstance().incrementStatImmediately(item, player, InfusionType.STAT_TRACK_BLOCKS, 1);

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
		}
		player.sendMessage(Component.text("There are no valid blocks to place in the shulker!", NamedTextColor.RED));
		return false;
	}

	public static boolean changeMode(ItemStack item, Player player) {
		FirmamentType type = getFirmamentType(item);
		if (type == null) {
			//Somehow triggered when it wasn't the right item - shouldn't prevent the event to be safe
			return false;
		}
		if (!player.isSneaking()) {
			return false;
		}

		NBT.modify(item, nbt -> {
			ReadWriteNBT playerModified = ItemStatUtils.addPlayerModified(nbt);
			boolean previouslyDisabled = isDisabled(playerModified);
			playerModified.setBoolean(DISABLED_KEY, !previouslyDisabled);
			player.sendMessage(type.getMessage(!previouslyDisabled));
			player.playSound(player.getLocation(), previouslyDisabled ? Sound.BLOCK_SHULKER_BOX_OPEN : Sound.BLOCK_SHULKER_BOX_CLOSE, SoundCategory.BLOCKS, 1, 1);
		});
		ItemUpdateHelper.generateItemStats(item);
		return true;
	}

	public static @Nullable FirmamentType getFirmamentType(@Nullable ItemStack item) {
		if (item == null || !ItemStatUtils.getTier(item).equals(Tier.EPIC) || !ItemUtils.isShulkerBox(item.getType())) {
			return null;
		}
		for (FirmamentType type : FirmamentType.values()) {
			if (InventoryUtils.testForItemWithName(item, type.mItemName, true)) {
				return type;
			}
		}
		return null;
	}

	public static boolean isFirmamentItem(@Nullable ItemStack item) {
		return getFirmamentType(item) != null;
	}

	public static boolean isDisabled(ItemStack itemStack) {
		return NBT.get(itemStack, nbt -> {
			return isDisabled(ItemStatUtils.getPlayerModified(nbt));
		});
	}

	public static boolean isDisabled(@Nullable ReadableNBT playerModified) {
		return playerModified != null && playerModified.getOrDefault(DISABLED_KEY, false);
	}
}
