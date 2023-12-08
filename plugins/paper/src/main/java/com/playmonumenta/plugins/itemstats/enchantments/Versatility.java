package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.Effect;
import com.playmonumenta.plugins.effects.OnHitTimerEffect;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.EnumSet;
import java.util.NavigableSet;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Versatility implements Enchantment {
	private static final String VERSATILITY_MELEE_NAME = "MeleeVersatilityEffect";
	private static final String VERSATILITY_PROJ_NAME = "ProjectileVersatilityEffect";
	private static final String VERSATILITY_MAGIC_NAME = "MagicVersatilityEffect";
	private static final int PAST_HIT_DURATION_TIME = 20 * 60;
	private static final double DAMAGE_PER_LEVEL = 0.1;

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.VERSATILITY;
	}

	@Override
	public String getName() {
		return "Versatility";
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		DamageType type = event.getType();
		NavigableSet<Effect> melee = plugin.mEffectManager.getEffects(player, VERSATILITY_MELEE_NAME);
		NavigableSet<Effect> proj = plugin.mEffectManager.getEffects(player, VERSATILITY_PROJ_NAME);
		NavigableSet<Effect> magic = plugin.mEffectManager.getEffects(player, VERSATILITY_MAGIC_NAME);
		if (type == DamageType.MELEE || type == DamageType.MELEE_ENCH || type == DamageType.MELEE_SKILL) {
			if (proj != null || magic != null) {
				double bonus = DAMAGE_PER_LEVEL * level;
				double damage = event.getDamage() * (1 + bonus);
				event.setDamage(damage);

				audioVisuals(player, enemy);
			}

			clearEffects(plugin, player);
			plugin.mEffectManager.addEffect(player, VERSATILITY_MELEE_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 1));
		} else if (type == DamageType.PROJECTILE || type == DamageType.PROJECTILE_SKILL) {
			if (melee != null || magic != null) {
				double bonus = DAMAGE_PER_LEVEL * level;
				double damage = event.getDamage() * (1 + bonus);
				event.setDamage(damage);

				audioVisuals(player, enemy);
			}

			clearEffects(plugin, player);
			plugin.mEffectManager.addEffect(player, VERSATILITY_PROJ_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 1));
		} else if (type == DamageType.MAGIC) {
			if (proj != null || melee != null) {
				double bonus = DAMAGE_PER_LEVEL * level;
				double damage = event.getDamage() * (1 + bonus);
				event.setDamage(damage);

				audioVisuals(player, enemy);
			}

			clearEffects(plugin, player);
			plugin.mEffectManager.addEffect(player, VERSATILITY_MAGIC_NAME, new OnHitTimerEffect(PAST_HIT_DURATION_TIME, 1));
		}
	}

	private void clearEffects(Plugin plugin, Player player) {
		plugin.mEffectManager.clearEffects(player, VERSATILITY_MELEE_NAME);
		plugin.mEffectManager.clearEffects(player, VERSATILITY_PROJ_NAME);
		plugin.mEffectManager.clearEffects(player, VERSATILITY_MAGIC_NAME);
	}

	private void audioVisuals(Player player, LivingEntity enemy) {
		player.playSound(player.getLocation(), Sound.ENTITY_WARDEN_ATTACK_IMPACT, SoundCategory.PLAYERS, 0.5f, 2f);
		player.playSound(player.getLocation(), Sound.ITEM_AXE_SCRAPE, SoundCategory.PLAYERS, 0.9f, 1.5f);

		double widthDelta = PartialParticle.getWidthDelta(enemy);
		double heightDelta = PartialParticle.getHeightDelta(enemy);
		new PartialParticle(Particle.REDSTONE, enemy.getEyeLocation(), 9, widthDelta * 1.5, heightDelta, widthDelta * 1.5, new Particle.DustOptions(Color.fromRGB(200, 200, 120), 1.1f)).spawnAsEnemy();
	}
}
