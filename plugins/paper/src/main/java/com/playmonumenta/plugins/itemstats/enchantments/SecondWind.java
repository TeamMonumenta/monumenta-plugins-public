package com.playmonumenta.plugins.itemstats.enchantments;

import org.bukkit.entity.Player;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;



public class SecondWind implements Enchantment {
	private static final double DAMAGE_RESIST = 0.1;
	private static final double HEALTH_LIMIT = 0.5;

	@Override
	public String getName() {
		return "Second Wind";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.SECOND_WIND;
	}

	@Override
	public void onHurt(Plugin plugin, Player player, double level, DamageEvent event) {
		double currHealth = player.getHealth();
		double maxHealth = EntityUtils.getMaxHealth(player);
		double hpAfterHit = currHealth - event.getDamage();
		if (currHealth / maxHealth <= HEALTH_LIMIT) {
			event.setDamage(event.getDamage() * Math.pow(1 - DAMAGE_RESIST, level));
		} else if (hpAfterHit / maxHealth <= HEALTH_LIMIT) {
			double hpLostBelowHalf = maxHealth / 2 - hpAfterHit;
			double proportion = hpLostBelowHalf / event.getDamage();
			event.setDamage(event.getDamage() * (1 - proportion) + event.getDamage() * proportion * Math.pow(1 - DAMAGE_RESIST, level));
		}
	}

}
