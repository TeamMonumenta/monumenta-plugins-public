package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class ProjectileDamageMultiply implements Attribute {

	@Override
	public String getName() {
		return "Projectile Damage Multiply";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.PROJECTILE_DAMAGE_MULTIPLY;
	}

	@Override
	public double getPriorityAmount() {
		return 20;
	}

	@Override
	public double getDefaultValue() {
		return 1;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		DamageEvent.DamageType type = event.getType();

		if (type == DamageType.PROJECTILE_SKILL && DepthsManager.getInstance().isInSystem(player)) {
			// Handled in DepthsListener
			return;
		}

		if (type.equals(DamageType.PROJECTILE) || type.equals(DamageType.PROJECTILE_SKILL)) {
			event.setDamage(event.getDamage() * value);
		}
	}

}
