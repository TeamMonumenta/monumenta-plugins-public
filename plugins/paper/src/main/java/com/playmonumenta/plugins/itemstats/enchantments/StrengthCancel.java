package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffectType;

public class StrengthCancel implements Enchantment {

	private static final double DAMAGE_ADD_CANCEL_PER_LEVEL = 3;

	@Override
	public String getName() {
		return "StrengthCancel";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.STRENGTH_CANCEL;
	}

	@Override
	public double getPriorityAmount() {
		// Set priority to before any
		return -1000;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE) && (event.getCause() == DamageCause.ENTITY_ATTACK || event.getCause() == DamageCause.ENTITY_SWEEP_ATTACK)) {
			int potLevel = player.getPotionEffect(PotionEffectType.INCREASE_DAMAGE).getAmplifier();
			if (PlayerUtils.isCriticalAttack(player)) {
				event.setDamage(event.getDamage() - (potLevel + 1) * DAMAGE_ADD_CANCEL_PER_LEVEL * 1.5);
			} else {
				event.setDamage(event.getDamage() - (potLevel + 1) * DAMAGE_ADD_CANCEL_PER_LEVEL);
			}
		}
	}
}
