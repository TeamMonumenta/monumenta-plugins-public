package com.playmonumenta.plugins.itemstats.enchantments;

import java.util.EnumSet;

import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;

public class Decay implements Enchantment {

	public static final int DURATION = 20 * 4;
	public static final String DOT_EFFECT_NAME = "DecayDamageOverTimeEffect";

	@Override
	public String getName() {
		return "Decay";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.DECAY;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND);
	}

	@Override
	public double getPriorityAmount() {
		return 16;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		double level = plugin.mItemStatManager.getEnchantmentLevel(player, EnchantmentType.DECAY);
		if (event.getType() == DamageType.MELEE) {
			apply(plugin, enemy, (int) (DURATION * player.getCooledAttackStrength(0)), (int) level, player);
		} else if (event.getType() == DamageType.PROJECTILE && event.getDamager() instanceof Trident) {
			apply(plugin, enemy, DURATION, (int) level, player);
		}
	}

	public static void apply(Plugin plugin, LivingEntity enemy, int duration, int decayLevel, Player player) {
		plugin.mEffectManager.addEffect(enemy, DOT_EFFECT_NAME, new CustomDamageOverTime((int)(DURATION * player.getCooledAttackStrength(0)), 1, 40 / decayLevel, player, null, Particle.SQUID_INK));
	}
}
