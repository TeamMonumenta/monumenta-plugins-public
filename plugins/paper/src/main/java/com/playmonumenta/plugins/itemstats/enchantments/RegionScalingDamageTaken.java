package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.Nullable;

public class RegionScalingDamageTaken implements Enchantment {

	private static final String SPEED_EFFECT_NAME = "RegionScalingPercentSpeedEffect";
	public static final double[] SPEED_EFFECT = {0, -0.1, -0.2};
	public static final double[] DAMAGE_TAKEN_MULTIPLIER = {1, 3, 9};

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
		return 4999; // second to last damage taken modifier, just before second wind
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (event.getType() == DamageEvent.DamageType.FALL || event.getType() == DamageEvent.DamageType.TRUE) {
			return;
		}
		event.setDamage(event.getDamage() * DAMAGE_TAKEN_MULTIPLIER[Math.max(0, Math.min((int) value, DAMAGE_TAKEN_MULTIPLIER.length - 1))]);
		if (event.getType() == DamageEvent.DamageType.POISON) {
			event.setDamage(Math.min(event.getDamage(), Math.max(player.getHealth() - 1, 0)));
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		plugin.mEffectManager.addEffect(player, SPEED_EFFECT_NAME, new PercentSpeed(21, SPEED_EFFECT[Math.max(0, Math.min((int) value, SPEED_EFFECT.length - 1))], SPEED_EFFECT_NAME).displaysTime(false));
		plugin.mPotionManager.addPotion(player, PotionManager.PotionID.ITEM,
			new PotionEffect(PotionEffectType.BAD_OMEN, 21, 0, false, false, false));
	}
}
