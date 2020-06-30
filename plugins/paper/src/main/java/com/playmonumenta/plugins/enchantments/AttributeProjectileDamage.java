package com.playmonumenta.plugins.enchantments;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;

public class AttributeProjectileDamage implements BaseAttribute {
	private static final String PROPERTY_NAME = "Projectile Damage";
	public static final String DAMAGE_METAKEY = "AttributeProjectileDamageMetakey";
	// Bow velocity comes out at around 2.95 to 3.05
	private static final double ARROW_VELOCITY_SCALE = 3;

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, Projectile proj, ProjectileLaunchEvent event) {
		/*
		 * Don't heal mobs and don't apply if there's a shootable item in the offhand; this is because you can use mainhand
		 * items for the attribute damage (like a Ghastcaller's with no arrows) and an offhand projectile (like infinite
		 * snowball) to get lots of damage. Since we have no shootables with offhand stats, just ignore the attribute calculation
		 * if the player shoots something and has a shootable in the offhand.
		 */
		if (value > 0 && !ItemUtils.isShootableItem(player.getInventory().getItemInOffHand().getType())) {
			proj.setMetadata(DAMAGE_METAKEY, new FixedMetadataValue(plugin, value));
		}
	}

	public static void onShootAttack(Plugin plugin, Projectile proj, LivingEntity target, EntityDamageByEntityEvent event) {
		if (proj.hasMetadata(DAMAGE_METAKEY)) {
			double damage = proj.getMetadata(DAMAGE_METAKEY).get(0).asDouble();
			// Only scale damage if not fully charged arrow and if it is an arrow being launched
			if (proj instanceof Arrow && !((Arrow) proj).isCritical()) {
				// Arrow speed will be different if arrow speed attribute is active, so scale properly
				damage *= Math.min(1, proj.getVelocity().length() / ARROW_VELOCITY_SCALE / AttributeProjectileSpeed.getProjectileSpeedModifier(proj));
			}

			//Regardless of projectile type, set damage here
			event.setDamage(damage);
		}
	}
}
