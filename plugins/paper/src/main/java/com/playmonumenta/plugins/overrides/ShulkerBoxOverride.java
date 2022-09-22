package com.playmonumenta.plugins.overrides;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;
import javax.annotation.Nullable;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class ShulkerBoxOverride extends BaseOverride {
	private static final String CAN_PLACE_SHULKER_PERM = "monumenta.canplaceshulker";

	@Override
	public boolean blockPlaceInteraction(Plugin plugin, Player player, ItemStack item, BlockPlaceEvent event) {
		if (FirmamentOverride.isFirmamentItem(item)) {
			return FirmamentOverride.placeBlock(player, item, event);
		} else {
			return player.hasPermission(CAN_PLACE_SHULKER_PERM);
		}
	}

	@Override
	public boolean rightClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		if (WorldshaperOverride.isWorldshaperItem(item)) {
			// Thinking about it one more time, we want this to return false (to avoid double place)
			return WorldshaperOverride.placeBlock(plugin, player, item);
		}

		// Pretty sure shulker boxes doesn't have a right click functionality so no reason to cancel anything?
		return true;
	}


	@Override
	public boolean leftClickItemInteraction(Plugin plugin, Player player, Action action, ItemStack item, @Nullable Block block) {
		// (It all returns true)
		if (WorldshaperOverride.isWorldshaperItem(item)) {
			return WorldshaperOverride.changeMode(item, player);
		} else {
			return true;
		}
	}

	@Override
	public boolean leftClickBlockInteraction(Plugin plugin, Player player, Action action, @Nullable ItemStack item, Block block) {
		// This also all returns true.
		if (WorldshaperOverride.isWorldshaperItem(item)) {
			return WorldshaperOverride.changeMode(item, player);
		} else {
			return true;
		}
	}

	@Override
	public boolean swapHandsInteraction(Plugin plugin, Player player, ItemStack item) {
		// (It all returns true)
		if (FirmamentOverride.isFirmamentItem(item)) {
			return FirmamentOverride.changeMode(item, player);
		} else {
			return true;
		}
	}

	@Override
	public boolean blockDispenseInteraction(Plugin plugin, Block block, ItemStack dispensed) {
		if (FirmamentOverride.isFirmamentItem(dispensed) || WorldshaperOverride.isWorldshaperItem(dispensed)) {
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
}
