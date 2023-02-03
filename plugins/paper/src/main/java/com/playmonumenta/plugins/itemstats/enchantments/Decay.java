package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.CustomDamageOverTime;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import com.playmonumenta.plugins.utils.ItemStatUtils.Slot;
import java.util.EnumSet;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Decay implements Enchantment {

	public static final int DURATION = 20 * 4;
	public static final String DOT_EFFECT_NAME = "DecayDamageOverTimeEffect";
	public static final String CHARM_DURATION = "Decay Duration";
	public static final String CHARM_DAMAGE = "Decay Damage";

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
		return EnumSet.of(Slot.MAINHAND, Slot.PROJECTILE);
	}

	@Override
	public double getPriorityAmount() {
		return 16;
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		if (AbilityUtils.isAspectTriggeringEvent(event, player)) {
			int duration = (int) (DURATION * (type == DamageType.MELEE ? player.getCooledAttackStrength(0) : 1));
			apply(plugin, enemy, duration, value, player);
		}
	}

	public static void apply(Plugin plugin, LivingEntity enemy, int duration, double decayLevel, Player player) {
		int finalDuration = CharmManager.getDuration(player, CHARM_DURATION, duration);
		double desiredPeriod = 40 / decayLevel;
		if (desiredPeriod > finalDuration) { // Can happen with enchantment reductions from region scaling
			return;
		}
		// The DoT effect only runs every 5 ticks, so select the period as a multiple of 5 ticks and adjust damage instead to match expected DPS
		int adjustedPeriod = (int) Math.ceil(desiredPeriod / 5) * 5;
		double damage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, 1) * adjustedPeriod / desiredPeriod;
		plugin.mEffectManager.addEffect(enemy, DOT_EFFECT_NAME, new CustomDamageOverTime(finalDuration, damage, adjustedPeriod, player, null));
	}
}
