package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class AttackDamageMultiply implements Attribute {

	@Override
	public String getName() {
		return "Attack Damage Multiply";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.ATTACK_DAMAGE_MULTIPLY;
	}

	@Override
	public double getPriorityAmount() {
		return 21;
	}

	@Override
	public void onDamage(@NotNull Plugin plugin, @NotNull Player player, double value, @NotNull DamageEvent event, @NotNull LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE || event.getType() == DamageType.MELEE_SKILL) {
			event.setDamage(event.getDamage() * value);
		}
	}

}
