package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.metadata.FixedMetadataValue;

public class ProjectileSpeed implements Attribute {

	private static final String SPEED_METAKEY = "AttributeArrowSpeedMetakey";

	@Override
	public String getName() {
		return "Projectile Speed";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.PROJECTILE_SPEED;
	}

	@Override
	public void onLaunchProjectile(Plugin plugin, Player player, double value, ProjectileLaunchEvent event, Projectile proj) {
		// If the level is 0, then it's just a vanilla item with no modifiers.
		if (value - 1 != 1) {
			proj.setMetadata(SPEED_METAKEY, new FixedMetadataValue(plugin, value - 1));
			proj.setVelocity(proj.getVelocity().multiply(value - 1));
		}
	}

	public static double getProjectileSpeedModifier(Projectile proj) {
		if (proj.hasMetadata(SPEED_METAKEY)) {
			return proj.getMetadata(ProjectileSpeed.SPEED_METAKEY).get(0).asDouble();
		}
		return 1;
	}
}
