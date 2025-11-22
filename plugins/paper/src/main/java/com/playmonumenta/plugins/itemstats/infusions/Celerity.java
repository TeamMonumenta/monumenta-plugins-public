package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import com.playmonumenta.plugins.utils.EntityUtils;
import java.util.Collection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Celerity implements Infusion {

	public static final double NEARBY_ENEMY_RANGE = 18;

	public static final double SPEED_BONUS = 0.025;

	private static final int EFFECT_DURATION = 10;
	private static final String PERCENT_SPEED_EFFECT_NAME = "CelerityPercentSpeedEffect";

	@Override
	public String getName() {
		return "Celerity";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.CELERITY;
	}

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHertz, boolean oneHertz) {
		if (shouldActivate(player)) {
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(EFFECT_DURATION, level * SPEED_BONUS, PERCENT_SPEED_EFFECT_NAME).displaysTime(false));
		} else {
			plugin.mEffectManager.clearEffects(player, PERCENT_SPEED_EFFECT_NAME);
		}
	}

	public boolean shouldActivate(Player player) {
		Collection<LivingEntity> nearbyEntities = player.getLocation().getNearbyLivingEntities(NEARBY_ENEMY_RANGE);
		for (LivingEntity entity : nearbyEntities) {
			if ((!(entity instanceof Player) && entity.getScoreboardTags().contains("Boss"))
				|| EntityUtils.isHostileMob(entity)) {
				return false;
			}
		}
		return true;
	}
}
