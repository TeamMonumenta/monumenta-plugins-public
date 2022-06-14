package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Perspicacity implements Infusion {

	public static final double DAMAGE_MOD_PER_LEVEL = 0.01;

	@Override
	public String getName() {
		return "Perspicacity";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.PERSPICACITY;
	}

	@Override
	public double getPriorityAmount() {
		return 26;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageType.MAGIC) {
			return;
		}

		double abilityDmgBuffPct = value * DAMAGE_MOD_PER_LEVEL;
		event.setDamage(event.getDamage() * (1.0 + abilityDmgBuffPct));
	}
}
