package com.playmonumenta.plugins.itemstats.infusions;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class Choler implements Infusion {

	private static final double DAMAGE_MLT_PER_LVL = 0.01;

	@Override
	public String getName() {
		return "Choler";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.CHOLER;
	}

	@Override
	public double getPriorityAmount() {
		return 23;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (EntityUtils.isStunned(enemy) || EntityUtils.isSlowed(plugin, enemy) || enemy.getFireTicks() > 0) {
			event.setDamage(event.getDamage() * (1 + (DAMAGE_MLT_PER_LVL * DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value))));
		}
	}
}
