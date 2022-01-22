package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Trident;

public class ProjectileDamageAdd implements Attribute {

	@Override
	public String getName() {
		return "Projectile Damage Add";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.PROJECTILE_DAMAGE_ADD;
	}

	@Override
	public double getPriorityAmount() {
		return 1;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Projectile proj) {
			if (proj instanceof AbstractArrow arrow && !(arrow instanceof Trident) && !arrow.isCritical()) {
				value *= Math.min(1, arrow.getVelocity().length() / Constants.PLAYER_BOW_INITIAL_SPEED / ProjectileSpeed.getProjectileSpeedModifier(arrow));
			}
			event.setDamage(value);
		}
	}

}
