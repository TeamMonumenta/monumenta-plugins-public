package com.playmonumenta.plugins.itemstats.infusions;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class Carapace implements Infusion {

	private static final double DAMAGE_REDUCTION_PER_LEVEL = 0.0125;
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
		// Runs one tick later so that it does not affect this attack
		new BukkitRunnable() {
			@Override
			public void run() {
				plugin.mEffectManager.addEffect(player, DAMAGE_REDUCTION_EFFECT_NAME, new PercentDamageReceived(DURATION, -DAMAGE_REDUCTION_PER_LEVEL * modifiedLevel));
			}
		}.runTaskLater(plugin, 1);
	}
}
