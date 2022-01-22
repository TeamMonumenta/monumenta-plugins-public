package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Player;

public class RegionScalingDamageTaken implements Enchantment {

	private static final String SPEED_EFFECT_NAME = "RegionScalingPercentSpeedEffect";
	private static final double SPEED_EFFECT = -0.1;
	private static final double DAMAGE_TAKEN_MULTIPLIER = 3;

	@Override
	public String getName() {
		return "RegionScalingDamageTaken";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.REGION_SCALING_DAMAGE_TAKEN;
	}

	@Override
	public double getPriorityAmount() {
		// Set priority to ABSOLUTE FINAL ItemStat event (even after crit scaling)
		return 10000;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event) {
		if (!ServerProperties.getClassSpecializationsEnabled()) {
			event.setDamage(event.getDamage() * DAMAGE_TAKEN_MULTIPLIER);
			if (event.getType() == DamageEvent.DamageType.AILMENT) {
				event.setDamage(Math.min(event.getDamage(), Math.max(player.getHealth() - 1, 0)));
			}
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (!ServerProperties.getClassSpecializationsEnabled()) {
			plugin.mEffectManager.addEffect(player, SPEED_EFFECT_NAME, new PercentSpeed(20, SPEED_EFFECT, SPEED_EFFECT_NAME));
		}
	}
}
