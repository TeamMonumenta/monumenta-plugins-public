package com.playmonumenta.plugins.listeners;

import java.util.Set;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LingeringPotion;
import org.bukkit.entity.Player;
import org.bukkit.entity.SplashPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class ThrowablePotionListener implements Listener {

	private static final String INVENTORY_DRINK_TAG = "InventoryDrinkTag"; //Tag to enable this feature (drink from inventory right click)

	@EventHandler(priority = EventPriority.HIGHEST)
	public void InventoryClickEvent(InventoryClickEvent event) {
		if (
		    // Must not be cancelled
		    event.isCancelled() ||
		    // Must be a right click
		    event.getClick() == null ||
		    !event.getClick().equals(ClickType.RIGHT) ||
		    // Must be placing a single block
		    event.getAction() == null ||
		    !event.getAction().equals(InventoryAction.PICKUP_HALF) ||
		    // Must be a player interacting with their main inventory
		    event.getWhoClicked() == null ||
		    !(event.getWhoClicked() instanceof Player) ||
		    event.getClickedInventory() == null ||
		    // Must be a click on a shulker box with an empty hand
		    (event.getCursor() != null && !event.getCursor().getType().equals(Material.AIR)) ||
		    event.getCurrentItem() == null ||
		    !(event.getCurrentItem().getType().equals(Material.SPLASH_POTION) ||
		      event.getCurrentItem().getType().equals(Material.LINGERING_POTION))
		) {

			// Nope!
			return;
		}

		Player player = (Player)event.getWhoClicked();
		ItemStack item = event.getCurrentItem();

		Set<String> tags = player.getScoreboardTags();

		if ( !tags.contains(INVENTORY_DRINK_TAG) || item.getI18NDisplayName().equals("Alchemist's Potion") ) {
			//Needs this tag to work
			return;
		}

		if (item.getType().equals(Material.SPLASH_POTION)) {
			SplashPotion potion = (SplashPotion) player.getWorld().spawnEntity(player.getLocation(), EntityType.SPLASH_POTION);
			potion.setItem(item);
		} else if (item.getType().equals(Material.LINGERING_POTION)) {
			LingeringPotion potion = (LingeringPotion) player.getWorld().spawnEntity(player.getLocation(), EntityType.LINGERING_POTION);
			potion.setItem(item);

		}

		item.setAmount(item.getAmount() - 1);

		event.setCancelled(true);
	}
}
