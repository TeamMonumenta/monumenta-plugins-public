package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public class Carapace implements Infusion {

	public static final double DAMAGE_REDUCTION_PER_LEVEL = 0.0125;
	private static final String DAMAGE_REDUCTION_EFFECT_NAME = "OrangeInfusionDamageReductionEffect";
	private static final int DURATION = 5 * 20;

	@Override
	public String getName() {
		return "Carapace";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.CARAPACE;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
		if (source != null) {
			// Runs one tick later so that it does not affect this attack
			Bukkit.getScheduler().runTaskLater(plugin, () -> plugin.mEffectManager.addEffect(player, DAMAGE_REDUCTION_EFFECT_NAME, new PercentDamageReceived(DURATION, getDamageTakenMultiplier(modifiedLevel) - 1)), 1);
		}
	}

	public static double getDamageTakenMultiplier(double level) {
		return 1 - DAMAGE_REDUCTION_PER_LEVEL * level;
	}

}
