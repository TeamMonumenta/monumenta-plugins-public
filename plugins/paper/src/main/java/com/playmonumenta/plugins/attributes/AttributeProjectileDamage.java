package com.playmonumenta.plugins.attributes;

import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;



public class AttributeProjectileDamage implements BaseAttribute {
	public static final String PROPERTY_NAME = "Projectile Damage";
	public static final String DAMAGE_METAKEY = "AttributeProjectileDamageMetakey";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, Projectile proj, ProjectileLaunchEvent event) {
		// Don't heal mobs
		if (value > 0) {
			proj.setMetadata(DAMAGE_METAKEY, new FixedMetadataValue(plugin, value));
		}
	}

	/*
	 * TODO: This needs some kind of better registration than expecting it to be called directly
	 *
	 * IF YOU COPY THIS YOU MUST PUT A CORRESPONDING CALL IN EntityDamageByEntityEvent !
	 *
	 * This works this way because you might have the enchantment when you fire the arrow, but switch to a different item before it hits
	 */
	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(DAMAGE_METAKEY)) {
			double damage = proj.getMetadata(DAMAGE_METAKEY).get(0).asDouble();
			// Only scale damage if not fully charged arrow and if it is an arrow being launched
			if (proj instanceof AbstractArrow && !(proj instanceof Trident) && !((AbstractArrow) proj).isCritical()) {
				//TODO change event in EntityListener, from ProjectileLaunchEvent.
				// Then can check if bow or crossbow,
				// need to account for different initial launch speeds

				// Arrow speed will be different if arrow speed attribute is active, so scale properly
				damage *= Math.min(1, proj.getVelocity().length() / Constants.PLAYER_BOW_INITIAL_SPEED / AttributeProjectileSpeed.getProjectileSpeedModifier(proj));
			}

			//Trident can have mob specific enchantments (Impaling, Smite, Bane of Arthropods)
			if (proj instanceof Trident trident) {
				ItemStack item = trident.getItemStack();

				if (item.containsEnchantment(org.bukkit.enchantments.Enchantment.DAMAGE_UNDEAD) && EntityUtils.isUndead(target)) {
					damage += item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DAMAGE_UNDEAD) * 2.5;
				}
				if (item.containsEnchantment(org.bukkit.enchantments.Enchantment.IMPALING) && EntityUtils.isAquatic(target)) {
					damage += item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.IMPALING) * 2.5;
				}
				if (item.containsEnchantment(org.bukkit.enchantments.Enchantment.DAMAGE_ARTHROPODS) && EntityUtils.isArthropod(target)) {
					damage += item.getEnchantmentLevel(org.bukkit.enchantments.Enchantment.DAMAGE_ARTHROPODS) * 2.5;
				}
			}

			//Regardless of projectile type, set damage here
			event.setDamage(damage);
		}
	}
}
