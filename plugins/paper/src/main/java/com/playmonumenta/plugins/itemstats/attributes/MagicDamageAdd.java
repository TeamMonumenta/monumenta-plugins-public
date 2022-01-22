package com.playmonumenta.plugins.itemstats.attributes;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Attribute;
import com.playmonumenta.plugins.utils.ItemStatUtils.AttributeType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MagicDamageAdd implements Attribute {

	@Override
	public @NotNull String getName() {
		return "Magic Damage Add";
	}

	@Override
	public AttributeType getAttributeType() {
		return AttributeType.MAGIC_DAMAGE_ADD;
	}

	@Override
	public double getPriorityAmount() {
		return 3;
	}

	@Override
	public void onDamage(@NotNull Plugin plugin, @NotNull Player player, double value, @NotNull DamageEvent event, @NotNull LivingEntity enemy) {
		if (event.getType() == DamageType.MAGIC) {
			event.setDamage(event.getDamage() + value);
		}
	}

}
