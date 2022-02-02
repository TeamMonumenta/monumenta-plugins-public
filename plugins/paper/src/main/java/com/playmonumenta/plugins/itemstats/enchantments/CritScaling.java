package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class CritScaling implements Enchantment {

	public static final double CRIT_BONUS = 1.5;

	@Override
	public String getName() {
		return "CritScaling";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CRIT_SCALING;
	}

	@Override
	public double getPriorityAmount() {
		return 4999; // second to last damage dealt modifier, just before region scaling
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE) {
			event.setDamage(event.getDamage() * player.getCooledAttackStrength(0));
		}
		if (event.getType() == DamageType.MELEE && PlayerUtils.isFallingAttack(player)) {
			event.setDamage(event.getDamage() * CRIT_BONUS);
		}
	}
}
