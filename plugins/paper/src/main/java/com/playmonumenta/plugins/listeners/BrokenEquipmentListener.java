package com.playmonumenta.plugins.listeners;

import com.playmonumenta.plugins.utils.MessagingUtils;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;

public class BrokenEquipmentListener implements Listener {

	private static final String ERROR_MESSAGE = "Broken items must be repaired before use";

	private static boolean isBroken(ItemStack item) {
		return item != null
			       && item.getType().getMaxDurability() > 0
			       && item.getItemMeta() instanceof Damageable damageable
			       && damageable.getDamage() >= item.getType().getMaxDurability();
	}

	// Player interacts with a block in the world
	// via left-click, right-click, or stepping on a pressure plate
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = false)
	public void playerInteractEvent(PlayerInteractEvent event) {
		if (event.useItemInHand() != Event.Result.DENY) {
			Player player = event.getPlayer();
			ItemStack item = event.getItem();
			if (item != null && isBroken(item) && !item.containsEnchantment(Enchantment.RIPTIDE)) {
				MessagingUtils.sendActionBarMessage(player, ERROR_MESSAGE);
				event.setUseItemInHand(Event.Result.DENY);
				event.setCancelled(true);
			}
		}
	}

	// Player right-clicks an entity
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void playerInteractEntityEvent(PlayerInteractEntityEvent event) {
		Player player = event.getPlayer();
		ItemStack item;
		if (event.getHand() == EquipmentSlot.HAND) {
			item = player.getInventory().getItemInMainHand();
		} else {
			item = player.getInventory().getItemInOffHand();
		}
		if (isBroken(item)) {
			MessagingUtils.sendActionBarMessage(player, ERROR_MESSAGE);
			event.setCancelled(true);
		}
	}

	// One entity attacks another
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void entityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Player player) {
			if (isBroken(player.getInventory().getItemInMainHand())) {
				MessagingUtils.sendActionBarMessage(player, ERROR_MESSAGE);
				event.setCancelled(true);
			}
		}
	}

	// Block Dispense Event
	// Cancel dispensers/droppers dropping specific items
	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockDispenseEvent(BlockDispenseEvent event) {
		if (isBroken(event.getItem())) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void blockBreakEvent(BlockBreakEvent event) {
		Player player = event.getPlayer();
		ItemStack item = player.getInventory().getItemInMainHand();
		if (isBroken(item)) {
			event.setCancelled(true);
		}
	}
}
