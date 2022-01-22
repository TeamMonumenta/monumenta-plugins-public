package com.playmonumenta.plugins.itemstats.enchantments;

import java.util.NavigableSet;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;

public class CurseOfCrippling implements Enchantment {
	private static final int DURATION = 3 * 20;
	private static final double PERCENT_SPEED = -0.3;
	private static final String PERCENT_SPEED_EFFECT_NAME = "CripplingPercentSpeedEffect";

	@Override
	public @NotNull String getName() {
		return "Curse of Crippling";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_CRIPPLING;
	}


	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event) {
		if (event.getSource() != null) {
			NavigableSet<Effect> speedEffects = plugin.mEffectManager.getEffects(player, PERCENT_SPEED_EFFECT_NAME);
			if (speedEffects != null) {
				for (Effect effect : speedEffects) {
					effect.setDuration(DURATION);
				}
			} else {
				plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED, PERCENT_SPEED_EFFECT_NAME));
			}
			player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 16, 0.4, 0.5, 0.4);
			player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.HOSTILE, 0.25f, 0.8f);
		}
	}
}