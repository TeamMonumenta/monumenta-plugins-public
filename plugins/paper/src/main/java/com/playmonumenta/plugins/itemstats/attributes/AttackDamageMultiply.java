package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
	public double getDefaultValue() {
		return 1;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();

		if (type == DamageType.MELEE_SKILL && DepthsManager.getInstance().isInSystem(player)) {
			// Handled in DepthsListener
			return;
		}

		if (type == DamageType.MELEE || type == DamageType.MELEE_SKILL) {
			event.setDamage(event.getDamage() * value);
		}
	}

}
