package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import javax.annotation.Nullable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;



public class SecondWind implements Enchantment {
	private static final double DAMAGE_RESIST = 0.1;
	private static final double HEALTH_LIMIT = 0.5;
	public static final String CHARM_RESISTANCE = "Second Wind Resistance";
	public static final String CHARM_THRESHOLD = "Second Wind Threshold";

	@Override
	public String getName() {
		return "Second Wind";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SECOND_WIND;
	}

	@Override
	public double getPriorityAmount() {
		return 5000; // should be the final damage taken modifier
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double level, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		double healthThreshold = HEALTH_LIMIT + (CharmManager.getLevelPercent(player, CHARM_THRESHOLD) / 100.0);
		double currHealth = player.getHealth();
		double maxHealth = EntityUtils.getMaxHealth(player);
		double hpAfterHit = currHealth - event.getFinalDamage(true);
		if (currHealth / maxHealth <= healthThreshold) {
			event.setDamage(event.getDamage() * getCharmDamageMultiplier(player, level));
		} else if (hpAfterHit / maxHealth <= healthThreshold) {
			double hpLostBelowHalf = maxHealth * healthThreshold - hpAfterHit;
			double proportion = hpLostBelowHalf / event.getFinalDamage(false);
			event.setDamage(event.getDamage() * (1 - proportion) + event.getDamage() * proportion * getCharmDamageMultiplier(player, level));
		}
	}

	public static double getDamageMultiplier(double level) {
		return Math.pow(1 - DAMAGE_RESIST, level);
	}

	public static double getCharmDamageMultiplier(Player player, double level) {
		return Math.pow(1 - CharmManager.calculateFlatAndPercentValue(player, CHARM_RESISTANCE, DAMAGE_RESIST), level);
	}

}
