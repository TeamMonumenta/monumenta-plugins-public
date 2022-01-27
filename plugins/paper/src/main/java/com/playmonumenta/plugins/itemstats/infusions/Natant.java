package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.Player;

public class Natant implements Infusion {

	private static final int DURATION = 10;
	private static final double PERCENT_SPEED_PER_LEVEL = 0.04;
	private static final String PERCENT_SPEED_EFFECT_NAME = "NatantPercentSpeedEffect";

	@Override
	public String getName() {
		return "Natant";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.NATANT;
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
		double percentSpeed = PERCENT_SPEED_PER_LEVEL * modifiedLevel;
		if (player.isInWaterOrBubbleColumn()) {
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, percentSpeed, PERCENT_SPEED_EFFECT_NAME));
		}
	}

}
