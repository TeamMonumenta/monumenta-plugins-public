package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.server.properties.ServerProperties;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class RegionScalingDamageTaken implements Enchantment {

	private static final String SPEED_EFFECT_NAME = "RegionScalingPercentSpeedEffect";
	public static final double SPEED_EFFECT = -0.1;
	public static final double DAMAGE_TAKEN_MULTIPLIER = 3;

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
		if (!ServerProperties.getClassSpecializationsEnabled()) {
			if (event.getType() == DamageEvent.DamageType.FALL) {
				return;
			}
			event.setDamage(event.getDamage() * DAMAGE_TAKEN_MULTIPLIER);
			if (event.getType() == DamageEvent.DamageType.POISON) {
				event.setDamage(Math.min(event.getDamage(), Math.max(player.getHealth() - 1, 0)));
			}
		}
	}

	@Override
	public void tick(Plugin plugin, Player player, double value, boolean twoHz, boolean oneHz) {
		if (!ServerProperties.getClassSpecializationsEnabled()) {
			plugin.mEffectManager.addEffect(player, SPEED_EFFECT_NAME, new PercentSpeed(20, SPEED_EFFECT, SPEED_EFFECT_NAME));
			plugin.mPotionManager.addPotion(player, PotionManager.PotionID.ITEM,
				new PotionEffect(PotionEffectType.BAD_OMEN, 20, 0, false, false, false));
		}
	}
}
