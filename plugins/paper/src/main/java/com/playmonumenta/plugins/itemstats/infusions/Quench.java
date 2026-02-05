package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.itemstats.EffectType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import java.util.EnumSet;
import org.bukkit.entity.Player;

public class Quench implements Infusion {

	public static final double DURATION_BONUS_PER_LVL = 0.025;
	public static final EnumSet<EffectType> EXCLUDED_EFFECTS = EnumSet.of(
		EffectType.CUSTOM_HEALTH_OVER_TIME
	);

	@Override
	public String getName() {
		return "Quench";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.QUENCH;
	}

	public static double getDurationScaling(Plugin plugin, Player player) {
		double level = plugin.mItemStatManager.getInfusionLevel(player, InfusionType.QUENCH);
		return 1 + DURATION_BONUS_PER_LVL * level;
	}
}
