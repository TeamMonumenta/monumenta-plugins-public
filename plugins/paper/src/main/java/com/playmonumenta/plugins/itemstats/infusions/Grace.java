package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentAttackSpeed;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.entity.Player;

public class Grace implements Infusion {

	public static final double ATKS_BONUS = 0.015;
	private static final int DURATION = 20;
	private static final String EFFECT_NAME = "GraceAttackSpeedEffect";

	@Override
	public String getName() {
		return "Grace";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.GRACE;
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (twoHz) {
			double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
			plugin.mEffectManager.addEffect(player, EFFECT_NAME, new PercentAttackSpeed(DURATION, ATKS_BONUS * modifiedLevel, EFFECT_NAME).displaysTime(false));
		}
	}
}
