package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;

public class PortableEnderListener implements Listener {
	private static final String LOCK_STRING = "PortableEnder";

	@EventHandler(priority = EventPriority.HIGHEST)
	public void inventoryClickEvent(InventoryClickEvent event) {
		if (!event.isCancelled() &&
		    event.getClick() == ClickType.RIGHT &&
		    event.getAction() == InventoryAction.PICKUP_HALF &&
		    event.getWhoClicked() instanceof Player) {
			// An item was right-clicked
			Player player = (Player) event.getWhoClicked();
			ItemStack item = event.getCurrentItem();
			if (isPortableEnder(item) &&
			    !ItemUtils.isItemShattered(item)) {
				// The clicked item is a portable ender chest, and is not shattered
				event.setCancelled(true);
				if (ZoneUtils.hasZoneProperty(player, ZoneUtils.ZoneProperty.NO_PORTABLE_STORAGE)) {
					player.sendMessage(ChatColor.RED + "The void here is too thick to part");
					player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.PLAYERS, 1.0f, 0.6f);
				} else if (ScoreboardUtils.getScoreboardValue(player, "RushDown") < 40) {
					player.sendMessage(ChatColor.RED + "You must conquer Wave 40 of Rush of Dissonance before you can part the void.");
					player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, SoundCategory.PLAYERS, 1.0f, 0.6f);
				} else {
					player.closeInventory(InventoryCloseEvent.Reason.OPEN_NEW);
					player.openInventory(player.getEnderChest());
					player.playSound(player.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.PLAYERS, 1.0f, 1.0f);
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		if (!event.isCancelled() && isPortableEnder(event.getItem())) {
			event.getBlock().getWorld().playSound(event.getBlock().getLocation(), Sound.ENTITY_SHULKER_HURT, SoundCategory.PLAYERS, 1.0f, 1.0f);
			event.setCancelled(true);
		}
	}

	public static boolean isPortableEnder(ItemStack item) {
		if (item != null && ItemUtils.isShulkerBox(item.getType()) && item.hasItemMeta()) {
			if (item.getItemMeta() instanceof BlockStateMeta) {
				BlockStateMeta blockStateMeta = (BlockStateMeta) item.getItemMeta();
				if (blockStateMeta.getBlockState() instanceof ShulkerBox) {
					ShulkerBox shulkerBox = (ShulkerBox) blockStateMeta.getBlockState();
					return shulkerBox.isLocked() && shulkerBox.getLock().equals(LOCK_STRING);
				}
			}
		}
		return false;
	}

}
