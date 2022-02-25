package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.utils.ItemStatUtils.EnchantmentType;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class CurseOfCrippling implements Enchantment {
	private static final int DURATION = 3 * 20;
	private static final double PERCENT_SPEED = -0.3;
	private static final String PERCENT_SPEED_EFFECT_NAME = "CripplingPercentSpeedEffect";

	@Override
	public String getName() {
		return "Curse of Crippling";
	}

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.CURSE_OF_CRIPPLING;
	}


	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {
		if (source == null || event.getType() != DamageEvent.DamageType.MELEE || event.getDamage() <= 0) {
			return;
		}
		plugin.mEffectManager.addEffect(player, PERCENT_SPEED_EFFECT_NAME, new PercentSpeed(DURATION, PERCENT_SPEED, PERCENT_SPEED_EFFECT_NAME));
		player.getWorld().spawnParticle(Particle.CRIT, player.getLocation().add(0, 1, 0), 16, 0.4, 0.5, 0.4);
		player.playSound(player.getLocation(), Sound.ENTITY_EVOKER_FANGS_ATTACK, SoundCategory.HOSTILE, 0.25f, 0.8f);
	}
}
