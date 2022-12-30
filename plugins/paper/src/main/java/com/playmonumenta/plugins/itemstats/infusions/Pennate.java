package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Pennate implements Infusion {

	private static final double REDUCT_PCT_PER_LEVEL = 0.05;

	@Override
	public String getName() {
		return "Pennate";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.PENNATE;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageType.FALL) {
			double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
			event.setDamage(event.getDamage() * getFallDamageResistance(modifiedLevel));
		}
	}

	public static double getFallDamageResistance(double level) {
		return 1.0 - REDUCT_PCT_PER_LEVEL * level;
	}

}
