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
		return -10000; // very first damage modifier
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (player.hasPotionEffect(PotionEffectType.INCREASE_DAMAGE) && event.getCause() == DamageCause.ENTITY_ATTACK) {
			int potLevel = player.getPotionEffect(PotionEffectType.INCREASE_DAMAGE).getAmplifier();

			float cooldown = player.getCooledAttackStrength(0.5F);
			double strengthDamage = (potLevel + 1) * DAMAGE_ADD_CANCEL_PER_LEVEL * (0.2F + cooldown * cooldown * 0.8F);

			if (PlayerUtils.isCriticalAttack(player)) {
				event.setDamage(event.getDamage() - strengthDamage * 1.5);
			} else {
				event.setDamage(event.getDamage() - strengthDamage);
			}
		}
	}
}
