package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

public class StrengthApply implements Enchantment {

	private static final double DAMAGE_ADD_PER_LEVEL = 0.10;

	@Override
	public String getName() {
		return "StrengthApply";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.STRENGTH_APPLY;
	}

	@Override
	public double getPriorityAmount() {
		return 1001; // after default item stats to multiply most damage
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (event.getType() != DamageEvent.DamageType.OTHER
			    && event.getType() != DamageEvent.DamageType.WARRIOR_AOE_OTHER
			    && player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE)) {
			int potLevel = player.getPotionEffect(PotionEffectType.INCREASE_DAMAGE).getAmplifier();
			event.setDamage(event.getDamage() * (1 + (potLevel + 1) * DAMAGE_ADD_PER_LEVEL));
		}
	}
}
