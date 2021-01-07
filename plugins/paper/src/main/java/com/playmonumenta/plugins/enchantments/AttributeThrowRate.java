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

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.InventoryUtils;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.NmsUtils;

public class AttributeThrowRate implements BaseAttribute {
	//Trident attribute only
	private static final String PROPERTY_NAME = "Throw Rate";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, Projectile proj, ProjectileLaunchEvent event) {
		/*
		 * TODO:
		 * Since we have a generic entity cloning method now, we can
		 * get rid of the item type checks. Not going to mess with it
		 * currently since we need to hotfix the trident dupe bug.
		 */
		if (proj instanceof Trident) {
			Trident trident = (Trident) proj;
			ItemStack item = trident.getItemStack();
			ItemStack mainhand = player.getInventory().getItemInMainHand();
			ItemStack offhand = player.getInventory().getItemInOffHand();

			// Off hand projectiles not supported
			if (offhand.equals(item)) {
				return;
			}

			//Check for Two Handed Curse.
			if (InventoryUtils.testForItemWithLore(mainhand, TwoHanded.PROPERTY_NAME) || InventoryUtils.testForItemWithLore(offhand, TwoHanded.PROPERTY_NAME)) {
				if (offhand.getType() != Material.AIR && mainhand.getType() != Material.AIR) {
					return;
				}
			}

			// Only run Throw Rate if the Infinity enchantment is not on the trident
			if (item.getEnchantmentLevel(Enchantment.ARROW_INFINITE) <= 0 && value > 0) {
				// Make trident unpickupable, set cooldown, damage trident based on Unbreaking enchant
				player.setCooldown(item.getType(), (int)(20 / value));
				ItemUtils.damageItemWithUnbreaking(item, 1, false);

				// Duplicate the entity, then cancel the throw event so the trident doesn't leave inventory
				Trident newProj = NmsUtils.duplicateEntity(trident);

				// Set a bunch of stuff that isn't caught by the entity duplication
				newProj.setShooter(player);
				if (proj.hasMetadata(AttributeProjectileDamage.DAMAGE_METAKEY)) {
					newProj.setMetadata(AttributeProjectileDamage.DAMAGE_METAKEY, new FixedMetadataValue(plugin, proj.getMetadata(AttributeProjectileDamage.DAMAGE_METAKEY).get(0).asDouble()));
				}

				newProj.setPickupStatus(PickupStatus.CREATIVE_ONLY);
				trident.setPickupStatus(PickupStatus.CREATIVE_ONLY);
				event.setCancelled(true);
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
