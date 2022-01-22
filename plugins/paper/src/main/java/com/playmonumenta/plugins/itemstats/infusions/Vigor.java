package com.playmonumenta.plugins.itemstats.infusions;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class Vigor implements Infusion {

	private static final double DAMAGE_PCT_PER_LEVEL = 0.01;

	@Override
	public String getName() {
		return "Vigor";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.VIGOR;
	}

	@Override
	public double getPriorityAmount() {
		return 25;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageType.MELEE) {
			return;
		}

		double damageBuffPct = value * DAMAGE_PCT_PER_LEVEL;
		event.setDamage(event.getDamage() * (1.0 + damageBuffPct));
	}

}
