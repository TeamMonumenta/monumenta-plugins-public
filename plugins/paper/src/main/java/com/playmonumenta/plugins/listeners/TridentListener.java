package com.playmonumenta.plugins.listeners;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;

public class TridentListener implements Listener {

	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void tridentThrowEvent(ProjectileLaunchEvent event) {
		//If a player throws a trident
		if (event.getEntity().getType() != EntityType.TRIDENT
			|| event.getEntity().getShooter() == null
			|| !(event.getEntity().getShooter() instanceof Player)) {
			return;
		}

		Player player = (Player) event.getEntity().getShooter();
		ItemStack mainhand = player.getInventory().getItemInMainHand();
		ItemStack offhand = player.getInventory().getItemInOffHand();
		Trident trident = (Trident) event.getEntity();
		ItemStack item = trident.getItemStack();

		//If neither hand has the same trident as the projectile,
		//or mainhand is changed to a trident while the offhand is being thrown (in case the mainhand has trident enchantments),
		//cancel trident throw
		if ((!mainhand.equals(item) && !offhand.equals(item)) ||
				(offhand.equals(item) && mainhand.getType() == Material.TRIDENT)) {
			event.setCancelled(true);
			return;
		}

		//Infinitely throwable trident with no cooldown if it has "Infinity" (Think creative mode trident throwing)
		if (item.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0) {
			//If Infinity or Loyalty, set projectile to be unpickable and manually do durablity damage
			trident.setPickupStatus(PickupStatus.CREATIVE_ONLY);

			//Puts the item back into inventory slot so that it stays in the slot
			if (mainhand.equals(item)) {
				player.getInventory().setItemInMainHand(item);
			} else if (offhand.equals(item)) {
				player.getInventory().setItemInOffHand(item);
			}
		}
	}
}
