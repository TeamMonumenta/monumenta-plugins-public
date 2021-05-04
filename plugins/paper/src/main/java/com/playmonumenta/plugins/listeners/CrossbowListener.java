package com.playmonumenta.plugins.listeners;

import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;

import com.playmonumenta.plugins.Plugin;

public class CrossbowListener implements Listener {

	private Plugin mPlugin;

	public CrossbowListener(Plugin plugin) {
		mPlugin = plugin;
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void projectileLaunchEvent(ProjectileLaunchEvent event) {
		//Has to be an arrow
		if (event.isCancelled()
				|| !(event.getEntity() instanceof AbstractArrow)
				|| event.getEntity() instanceof Trident
				|| !(event.getEntity().getShooter() instanceof LivingEntity)) {
			return;
		}

		LivingEntity shooter = (LivingEntity)event.getEntity().getShooter();
		ItemStack item = shooter.getEquipment().getItemInMainHand();
		AbstractArrow arrow = (AbstractArrow)event.getEntity();

		//For non player entities that shoot a crossbow
		if (item.getType().equals(Material.CROSSBOW) && !(arrow.getShooter() instanceof Player)) {
			if (item.getEnchantmentLevel(Enchantment.ARROW_FIRE) > 0) {
				arrow.setFireTicks(100);
			}
			if (item.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK) > 0) {
				arrow.setKnockbackStrength(item.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK));
			}
		}

		//Has to be a player-shot normal arrow
		if (arrow.getShooter() instanceof Player) {
			Player player = (Player)arrow.getShooter();
			ItemStack itemInMainHand = player.getEquipment().getItemInMainHand();
			ItemStack itemInOffHand = player.getEquipment().getItemInOffHand();

			// Check if the player has an crossbow in main or offhand
			if (itemInMainHand.getType().equals(Material.CROSSBOW) ||
			    itemInOffHand.getType().equals(Material.CROSSBOW)) {

				//Infinity gives arrow to player if the arrow shot had no custom effects
				if ((itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0
						|| itemInOffHand.getEnchantmentLevel(Enchantment.ARROW_INFINITE) > 0)
						&& arrow instanceof Arrow
						&& !((Arrow)event.getEntity()).hasCustomEffects()
						&& ((Arrow)event.getEntity()).getPickupStatus() == AbstractArrow.PickupStatus.ALLOWED) {
					arrow.setPickupStatus(AbstractArrow.PickupStatus.CREATIVE_ONLY);
					if (player.getGameMode() != GameMode.CREATIVE) {
						player.getInventory().addItem(new ItemStack(Material.ARROW));
					}
				}

				//Flame level metadata given, based off of mainhand if both have enchants
				if (itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_FIRE) > 0
						|| itemInOffHand.getEnchantmentLevel(Enchantment.ARROW_FIRE) > 0) {
					arrow.setFireTicks(100);
				}

				//Sets Punch manually to the arrow
				if (itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK) > 0
						|| itemInOffHand.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK) > 0) {
					int level = itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK) > 0 ?
							itemInMainHand.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK) : itemInOffHand.getEnchantmentLevel(Enchantment.ARROW_KNOCKBACK);
					arrow.setKnockbackStrength(level);
				}
				//Has to manually call projectile launch event because of arrow changes
				mPlugin.mTrackingManager.mPlayers.onLaunchProjectile(mPlugin, player, event.getEntity(), event);
			}
		}
	}
}
