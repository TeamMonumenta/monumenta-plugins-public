package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.depths.DepthsManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class MagicDamageMultiply implements Attribute {

	@Override
	public String getName() {
		return "Magic Damage Multiply";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.MAGIC_DAMAGE_MULTIPLY;
	}

	@Override
	public double getPriorityAmount() {
		return 22;
	}

	@Override
	public double getDefaultValue() {
		return 1;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (DepthsManager.getInstance().isInSystem(player)) {
			// Handled in DepthsListener
			return;
		}

		if (event.getType() == DamageType.MAGIC) {
			event.updateGearDamageWithMultiplier(value);
		}
	}
}
