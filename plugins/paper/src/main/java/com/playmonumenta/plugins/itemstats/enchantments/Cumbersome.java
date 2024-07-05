package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Cumbersome implements Enchantment {

	@Override
	public String getName() {
		return "Cumbersome";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CUMBERSOME;
	}

	@Override
	// after CritScaling (Priority of 4999) to change it back to non-crit
	// doesn't affect ability triggers
	public double getPriorityAmount() {
		return new CritScaling().getPriorityAmount() + 1;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(player)) {
			event.setIsCrit(false);
		}
	}
}
