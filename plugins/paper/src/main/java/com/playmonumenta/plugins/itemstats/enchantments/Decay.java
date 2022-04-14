package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

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
		DamageType type = event.getType();
		if ((type == DamageType.MELEE && ItemStatUtils.isNotExlusivelyRanged(player.getInventory().getItemInMainHand())) || type == DamageType.PROJECTILE) {
			int duration = (int) (DURATION * (type == DamageType.MELEE ? player.getCooledAttackStrength(0) : 1));
			apply(plugin, enemy, duration, (int) value, player);
		}
	}

	public static void apply(Plugin plugin, LivingEntity enemy, int duration, int decayLevel, Player player) {
		plugin.mEffectManager.addEffect(enemy, DOT_EFFECT_NAME, new CustomDamageOverTime(duration, 1, 40 / decayLevel, player, null, Particle.SQUID_INK));
	}
}
