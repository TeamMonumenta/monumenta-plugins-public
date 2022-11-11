package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.Particle;
import org.bukkit.entity.Player;

public class Soothing implements Infusion {

	private static final double HEAL_PER_LEVEL = 0.0275;

	@Override
	public String getName() {
		return "Soothing";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.SOOTHING;
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (oneHz) {
			double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
			PlayerUtils.healPlayer(plugin, player, modifiedLevel * HEAL_PER_LEVEL);
			new PartialParticle(Particle.VILLAGER_HAPPY, player.getLocation().add(0, 1, 0), 2, 0.5, 0.5, 0.5, 0.1).spawnAsPlayerBuff(player);
		}
	}
}
