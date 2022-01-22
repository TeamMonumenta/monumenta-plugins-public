package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

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
	public void onDamage(@NotNull Plugin plugin, @NotNull Player player, double value, @NotNull DamageEvent event, @NotNull LivingEntity enemy) {
		if (event.getType() == DamageType.PROJECTILE) {
			event.setDamage(event.getDamage() * value);
		}
	}

}
