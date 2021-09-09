package com.playmonumenta.plugins.bosses.spells.oldslabsbos;

import java.util.Iterator;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellWhirlwind extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;

	public SpellWhirlwind(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = mBoss.getWorld();
	}

	@Override
	public void run() {
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 1.5f);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 4));
		new BukkitRunnable() {
			int mTicks = 0;
			double mRadius = 4;
			double mBladeDamageRadius = 4;

			@Override
			public void run() {

				Location loc = mBoss.getLocation();

				mWorld.spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.125);
				mWorld.spawnParticle(Particle.CRIT, loc, 8, 0.1, 0.1, 0.1, 0.6);
				if (mTicks % 2 == 0) {

					for (int i = 0; i < 15; i += 1) {
						double radian1 = Math.toRadians(24 * i);
						loc.add(FastUtils.cos(radian1) * mRadius, 0.1, FastUtils.sin(radian1) * mRadius);
						mWorld.spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.025);
						mWorld.spawnParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0.25);
						loc.subtract(FastUtils.cos(radian1) * mRadius, 0.1, FastUtils.sin(radian1) * mRadius);

					}
				}

				mTicks++;
				if (mTicks >= 25) {
					this.cancel();

					List<Player> playersNearby = PlayerUtils.playersInRange(loc, 40, true);

					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.5f, 0.85f);
					new BukkitRunnable() {
						double mRotation = 0;
						double mSin = 0;
						double mY = 0;
						@Override
						public void run() {
							Location loc = mBoss.getLocation();
							mWorld.spawnParticle(Particle.CLOUD, loc, 4, 0.1, 0.1, 0.1, 0.15);
							mWorld.spawnParticle(Particle.CRIT, loc, 12, 0.1, 0.1, 0.1, 0.85);
							mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.85f + ((float)FastUtils.RANDOM.nextDouble() * 0.5f));
							for (int i = 0; i < 2; i++) {
								mRotation += 10;
								mSin += 0.1;
								mY = 1 + FastUtils.sin(mSin);
								double radian1 = Math.toRadians(mRotation);
								loc.add(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);
								mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0.025);
								mWorld.spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.025);
								mWorld.spawnParticle(Particle.CRIT, loc, 4, 0.1, 0.1, 0.1, 0.2);
								loc.subtract(FastUtils.cos(radian1) * mRadius, mY, FastUtils.sin(radian1) * mRadius);

								/*
								 * Check if this hits a player
								 * A player can only be hit once per cast of this attack
								 */
								Iterator<Player> iter = playersNearby.iterator();
								while (iter.hasNext()) {
									Player player = iter.next();

									if (player.getLocation().distance(loc) < mBladeDamageRadius) {
										BossUtils.bossDamage(mBoss, player, 6, mBoss.getLocation(), "Whirlwind");
										MovementUtils.knockAway(mBoss.getLocation(), player, 0.5f, 0.65f);
										iter.remove();
									}
								}
							}

							if (mRotation >= 360) {
								mBoss.removePotionEffect(PotionEffectType.SLOW);
								this.cancel();
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 9;
	}

}
