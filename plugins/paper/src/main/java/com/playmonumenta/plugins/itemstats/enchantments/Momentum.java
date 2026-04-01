package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import java.util.EnumSet;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;


public final class Momentum implements Enchantment {
	private static final EnumSet<DamageEvent.DamageType> AFFECTED_DAMAGE_TYPES = DamageEvent.DamageType.getAllMeleeTypes();

	private static final String MOMENTUM_EFFECT_NAME = "MomentumEffect";
	private static final String DISABLED_MOMENTUM_EFFECT_NAME = "DisabledMomentumEffect";
	private static final int SPRINT_TIME_REQUIRED = 20;
	private static final int EFFECT_DURATION = 60;
	private static final double DAMAGE_PER_LEVEL = 0.1;

	@Override
	public String getName() {
		return "Momentum";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.MOMENTUM;
	}

	@Override
	public double getPriorityAmount() {
		return 30;
	}
	/*
	| check if the player is sprinting.
	| If the player is not sprinting, add disable effect for 20 ticks (activation requirement).
	| If the player was sprinting checks, the disable effect will run out, and buff can be applied.
	| This way, the only way for it to "turn-on" is if the player passed all checks. (they were not not sprinting).
	 */

	@Override
	public void tick(Plugin plugin, Player player, double level, boolean twoHertz, boolean oneHertz) {
		if (!player.isSprinting()) {
			plugin.mEffectManager.addEffect(player, DISABLED_MOMENTUM_EFFECT_NAME,
				new OnHitTimerEffect(SPRINT_TIME_REQUIRED));
		}

		if (shouldActivate(player, plugin)) {
			if (shouldPlaySound(player, plugin)) {
				player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.PLAYERS, 0.8f, 1.8f);
			}
			plugin.mEffectManager.addEffect(player, MOMENTUM_EFFECT_NAME,
				new PercentDamageDealt(EFFECT_DURATION, level * DAMAGE_PER_LEVEL).damageTypes(AFFECTED_DAMAGE_TYPES).displaysTime(false));
		}
	}

	public boolean shouldActivate(Player player, Plugin plugin) {
		return plugin.mEffectManager.getActiveEffect(player, DISABLED_MOMENTUM_EFFECT_NAME) == null;
	}

	public boolean shouldPlaySound(Player player, Plugin plugin) {
		return plugin.mEffectManager.getActiveEffect(player, MOMENTUM_EFFECT_NAME) == null;
	}
}
