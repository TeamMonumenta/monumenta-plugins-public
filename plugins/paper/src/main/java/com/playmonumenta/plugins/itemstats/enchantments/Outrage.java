package com.playmonumenta.plugins.itemstats.enchantments;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.Enchantment;
import com.playmonumenta.plugins.itemstats.enums.EnchantmentType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class Outrage implements Enchantment {
	private static final double DAMAGE_INCREASE = 0.002;

	@Override
	public EnchantmentType getEnchantmentType() {
		return EnchantmentType.OUTRAGE;
	}

	@Override
	public String getName() {
		return "Outrage";
	}

	@Override
	public void onDamage(Plugin plugin, Player player, double value, DamageEvent event, LivingEntity enemy) {

		DamageEvent.DamageType type = event.getType();
		if (type == DamageEvent.DamageType.AILMENT
			|| type == DamageEvent.DamageType.POISON
			|| type == DamageEvent.DamageType.FALL
			|| type == DamageEvent.DamageType.OTHER
			|| type == DamageEvent.DamageType.TRUE
		) {
			return;
		}

		double healthPercent = Math.round(100 * player.getHealth() / EntityUtils.getMaxHealth(player));

		double missingHealthPercentAboveLimit = Math.min(100 - healthPercent, 50);
		double missingHealthPercentBelowLimit = Math.max(50 - healthPercent, 0);

		double damageMultiplier = missingHealthPercentAboveLimit * value * DAMAGE_INCREASE;
		damageMultiplier += 2 * missingHealthPercentBelowLimit * value * DAMAGE_INCREASE;

		event.updateGearDamageWithMultiplier(1 + damageMultiplier);

	}

	@Override
	public void onHurt(Plugin plugin, Player player, double value, DamageEvent event, @Nullable Entity damager, @Nullable LivingEntity source) {

		if (event.isBlocked() || source == null) {
			return;
		}

		double healthPostAttack = player.getHealth() - event.getFinalDamage(true);
		double maxHealth = EntityUtils.getMaxHealth(player);

		if (healthPostAttack > 0.5 * maxHealth) {
			return;
		}

		float healthPercentPostAttack = Math.round(10 * healthPostAttack / maxHealth) / 10f;

		double widthDelta = PartialParticle.getWidthDelta(player);
		double heightDelta = PartialParticle.getHeightDelta(player);

		float particleSize = Math.max(0.8f - healthPercentPostAttack, 0.5f);

		Particle.DustOptions dustOptions = new Particle.DustOptions(Color.fromBGR(10, 20, 125), particleSize);

		new PartialParticle(
			Particle.REDSTONE,
			LocationUtils.getHeightLocation(player, 0.8),
			7,
			widthDelta,
			heightDelta / 2,
			widthDelta,
			1,
			dustOptions
		).spawnAsEnemy();

		player.getWorld().playSound(
			player.getLocation(),
			Sound.BLOCK_LANTERN_HIT,
			0.2f,
			0.3f
		);

		new BukkitRunnable() {
			@Override
			public void run() {
				player.getWorld().playSound(
					player.getLocation(),
					Sound.BLOCK_LANTERN_HIT,
					0.06f,
					0.1f
				);
			}
		}.runTaskLater(plugin, 4);

	}
}
