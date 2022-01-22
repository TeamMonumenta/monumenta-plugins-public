package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Aura implements Infusion {

	private static final double SLOW_PER_LEVEL = 0.02;
	private static final int DURATION = 15;
	private static final int RADIUS = 3;

	@Override
	public String getName() {
		return "Aura";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.AURA;
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), RADIUS)) {
			EntityUtils.applySlow(plugin, DURATION, SLOW_PER_LEVEL * modifiedLevel, mob);
		}
	}
}
