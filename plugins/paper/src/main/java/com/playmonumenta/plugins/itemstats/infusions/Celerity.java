package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.itemstats.enums.InfusionType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Celerity implements Infusion {

	public static final double SPEED_BONUS = 0.025;
	private static final int EFFECT_DURATION = 10;
	private static final int DISABLE_DURATION = 100;

	private static final String PERCENT_SPEED_EFFECT_NAME = "CelerityPercentSpeedEffect";
	private static final String DISABLED_SPEED_EFFECT_NAME = "DisabledCelerityPercentSpeedEffect";

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
		if (shouldActivate(player, plugin)) {
			plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME,
				new PercentSpeed(EFFECT_DURATION, level * SPEED_BONUS, PERCENT_SPEED_EFFECT_NAME).displaysTime(false));
		}
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		plugin.mEffectManager.addEffect(player, DISABLED_SPEED_EFFECT_NAME, new OnHitTimerEffect(DISABLE_DURATION));
		plugin.mEffectManager.clearEffects(player, PERCENT_SPEED_EFFECT_NAME);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		plugin.mEffectManager.addEffect(player, DISABLED_SPEED_EFFECT_NAME, new OnHitTimerEffect(DISABLE_DURATION));
		plugin.mEffectManager.clearEffects(player, PERCENT_SPEED_EFFECT_NAME);
	}


	public boolean shouldActivate(Player player, Plugin plugin) {
		return plugin.mEffectManager.getActiveEffect(player, DISABLED_SPEED_EFFECT_NAME) == null;
	}
}
