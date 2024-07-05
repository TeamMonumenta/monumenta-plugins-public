package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.FractalCooldown;
import com.playmonumenta.plugins.effects.FractalVuln;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.itemstats.enums.Slot;
import com.playmonumenta.plugins.particle.PartialParticle;
import java.util.EnumSet;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class Fractal implements Enchantment {
	private static final int DURATION = 2 * 20;
	private static final double DAMAGE_PER_LEVEL = 0.05;
	private static final String SOURCE = "FractalVuln";
	private static final String SOURCE_DISABLE = "FractalDisable";
	private static final int DISABLE_DURATION = 10000 * 20;

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.FRACTAL;
	}

	@Override
	public EnumSet<Slot> getSlots() {
		return EnumSet.of(Slot.MAINHAND, Slot.OFFHAND, Slot.HEAD, Slot.CHEST, Slot.LEGS, Slot.FEET, Slot.PROJECTILE);
	}

	@Override
	public String getName() {
		return "Fractal";
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double level, DamageEvent event, LivingEntity enemy) {
		if (plugin.mEffectManager.getEffects(enemy, SOURCE_DISABLE + player.getName()) == null) {
			plugin.mEffectManager.addEffect(enemy, SOURCE_DISABLE + player.getName(), new FractalCooldown(DISABLE_DURATION));
			plugin.mEffectManager.addEffect(enemy, SOURCE + player.getName(), new FractalVuln(DURATION));
		} else if (plugin.mEffectManager.getEffects(enemy, SOURCE + player.getName()) != null && event.getType() == DamageEvent.DamageType.MAGIC) {
			double bonus = DAMAGE_PER_LEVEL * level;
			event.updateGearDamageWithMultiplier(1 + bonus);

			player.playSound(player.getLocation(), Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.PLAYERS, 0.3f, 2f);
			player.playSound(player.getLocation(), Sound.ITEM_TRIDENT_HIT, SoundCategory.PLAYERS, 0.5f, 1.2f);
			player.playSound(player.getLocation(), Sound.BLOCK_AMETHYST_BLOCK_CHIME, SoundCategory.PLAYERS, 0.6f, 1.5f);

			double widthDelta = PartialParticle.getWidthDelta(enemy);
			double heightDelta = PartialParticle.getHeightDelta(enemy);
			new PartialParticle(Particle.REDSTONE, enemy.getEyeLocation(), 9, widthDelta * 1.5, heightDelta, widthDelta * 1.5, new Particle.DustOptions(Color.fromRGB(200, 100, 200), 1.1f)).spawnAsEnemy();
		}
	}
}
