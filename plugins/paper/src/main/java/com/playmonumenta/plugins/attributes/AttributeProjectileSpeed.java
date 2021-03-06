package com.playmonumenta.plugins.attributes;

import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

import com.playmonumenta.plugins.Plugin;

public class AttributeProjectileSpeed implements BaseAttribute {
	private static final String PROPERTY_NAME = "Projectile Speed";
	private static final String SPEED_METAKEY = "AttributeArrowSpeedMetakey";

	@Override
	public String getProperty() {
		return PROPERTY_NAME;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, Projectile proj, ProjectileLaunchEvent event) {
		// If the level is 0, then it's just a vanilla item with no modifiers.
		if (value != 0) {
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
