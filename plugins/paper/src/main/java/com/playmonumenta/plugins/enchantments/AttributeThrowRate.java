package com.playmonumenta.plugins.enchantments;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;

public class AttributeThrowRate implements BaseAttribute {
	//Trident attribute only
	private static final String PROPERTY_NAME = "Throw Rate";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, Projectile proj, ProjectileLaunchEvent event) {
		if (proj instanceof Trident) {
			Trident trident = (Trident) proj;
			ItemStack item = trident.getItemStack();

			// Off hand projectiles not supported
			if (player.getInventory().getItemInOffHand().equals(item)) {
				return;
			}

			// Only run Throw Rate if the Infinity enchantment is not on the trident
			if (item.getEnchantmentLevel(Enchantment.ARROW_INFINITE) <= 0 && value > 0) {
				// Make trident unpickupable, set cooldown, damage trident based on Unbreaking enchant
				player.setCooldown(item.getType(), (int)(20 / value));
				trident.setPickupStatus(PickupStatus.CREATIVE_ONLY);
				ItemUtils.damageItemWithUnbreaking(item, 1, false);

				// Replace item in hand so that it stays in inventory, delayed or else trident doesn't throw
				new BukkitRunnable() {
					@Override
					public void run() {
						player.getInventory().setItemInMainHand(item);
					}
				}.runTaskLater(plugin, 0);
			}
		} else if (proj instanceof Snowball) {
			if (value > 0) {
				Snowball snowball = (Snowball) player.getWorld().spawnEntity(proj.getLocation(), EntityType.SNOWBALL);
				snowball.setShooter(player);
				snowball.setVelocity(proj.getVelocity());
				// Set projectile attributes; don't need to do speed attribute since that's only used to calculate non-critical arrow damage
				if (proj.hasMetadata(AttributeProjectileDamage.DAMAGE_METAKEY)) {
					snowball.setMetadata(AttributeProjectileDamage.DAMAGE_METAKEY, new FixedMetadataValue(plugin, proj.getMetadata(AttributeProjectileDamage.DAMAGE_METAKEY).get(0).asDouble()));
				}

				player.setCooldown(Material.SNOWBALL, (int)(20 / value));
				event.setCancelled(true);
			}
		}
	}
}
