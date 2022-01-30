package com.playmonumenta.plugins.itemstats.infusions;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.Infusion;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.DelveInfusionUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.ItemStatUtils.InfusionType;

public class Reflection implements Infusion {
	private static final double REFLECT_PCT_PER_LEVEL = 0.06;
	private static final int RADIUS = 4;

	@Override
	public String getName() {
		return "Reflection";
	}

	@Override
	public InfusionType getInfusionType() {
		return InfusionType.REFLECTION;
	}

	@Override
	public void onHurtByEntity(Plugin plugin, Player player, double value, DamageEvent event, Entity damager) {
		if (event.getType() == DamageType.MAGIC && !event.isBlocked()) {
			double modifiedLevel = DelveInfusionUtils.getModifiedLevel(plugin, player, (int) value);
			double reflectedDamage = modifiedLevel * REFLECT_PCT_PER_LEVEL * event.getOriginalDamage();
			World world = player.getWorld();
			world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.6f);
			world.playSound(player.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, 0.4f);
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					mTicks++;
					world.spawnParticle(Particle.SOUL_FIRE_FLAME, player.getLocation(), 2, 0.1, 0.1, 0.1, 0.15);
					if (mTicks >= 20) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(player.getLocation(), RADIUS, player)) {
							DamageUtils.damage(player, mob, DamageType.OTHER, reflectedDamage, null, true);
						}
						world.playSound(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 2.0f, 1.6f);
						new BukkitRunnable() {
							double mRadius = 0;
							final Location mLoc = player.getLocation();
							@Override
							public void run() {
								mRadius += 0.5;
								for (double j = 0; j < 360; j += 36) {
									double radian1 = Math.toRadians(j);
									mLoc.add(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
									world.spawnParticle(Particle.SOUL_FIRE_FLAME, mLoc, 1, 0, 0, 0, 0.125);
									world.spawnParticle(Particle.END_ROD, mLoc, 1, 0, 0, 0, 0.15);
									mLoc.subtract(FastUtils.cos(radian1) * mRadius, 0.15, FastUtils.sin(radian1) * mRadius);
								}

								if (mRadius >= RADIUS + 1) {
									this.cancel();
								}
							}

						}.runTaskTimer(plugin, 0, 1);
						this.cancel();
					}
				}
			}.runTaskTimer(plugin, 0, 1);
		}
	}
}
