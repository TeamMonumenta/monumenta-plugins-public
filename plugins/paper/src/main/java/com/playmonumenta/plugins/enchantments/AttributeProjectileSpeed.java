package com.playmonumenta.plugins.enchantments;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.ItemUtils;

public class AttributeProjectileSpeed implements BaseAttribute {
	private static final String PROPERTY_NAME = "Projectile Speed";
	private static final String SPEED_METAKEY = "AttributeArrowSpeedMetakey";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, Projectile proj, ProjectileLaunchEvent event) {
		/*
		 * Don't apply if there's a shootable item in the offhand; this is because of a weird interaction
		 * where we don't add the projectile damage metadata if there's a shootable in the offhand, which can
		 * lead to problems if the arrow has super-speed but not any damage setters, because default damage
		 * calculations are uncapped and based on arrow speed.
		 *
		 * Additionally, if the level is 0, then it's just a vanilla item with no modifiers.
		 */
		if (value != 0 && !ItemUtils.isShootableItem(player.getInventory().getItemInOffHand().getType())) {
			proj.setMetadata(SPEED_METAKEY, new FixedMetadataValue(plugin, value));
			proj.setVelocity(proj.getVelocity().multiply(value));
		}
	}

	public static double getProjectileSpeedModifier(Projectile proj) {
		if (proj.hasMetadata(SPEED_METAKEY)) {
			return proj.getMetadata(AttributeProjectileSpeed.SPEED_METAKEY).get(0).asDouble();
		}
		return 1;
	}
}
