package com.playmonumenta.plugins.bosses.spells.spells_oldslabsbos;

import java.util.Iterator;
import java.util.List;
import java.util.Random;

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
import com.playmonumenta.plugins.bosses.utils.DamageUtils;
import com.playmonumenta.plugins.bosses.utils.Utils;

public class SpellWhirlwind extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Random mRand;

	public SpellWhirlwind(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = mBoss.getWorld();
		mRand = new Random();
	}

	@Override
	public void run() {
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.8f);
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1.5f, 1.5f);
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 60, 4));
		new BukkitRunnable() {
			int t = 0;
			double radius = 4;
			double bladeDamageRadius = 4;
			@Override
			public void run() {

				Location loc = mBoss.getLocation();

				mWorld.spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.125);
				mWorld.spawnParticle(Particle.CRIT, loc, 8, 0.1, 0.1, 0.1, 0.6);
				if (t % 2 == 0) {

					for (int i = 0; i < 15; i += 1) {
						double radian1 = Math.toRadians(24 * i);
						loc.add(Math.cos(radian1) * radius, 0.1, Math.sin(radian1) * radius);
						mWorld.spawnParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.025);
						mWorld.spawnParticle(Particle.CRIT, loc, 2, 0.1, 0.1, 0.1, 0.25);
						loc.subtract(Math.cos(radian1) * radius, 0.1, Math.sin(radian1) * radius);

					}
				}

				t++;
				if (t >= 25) {
					this.cancel();

					List<Player> playersNearby = Utils.playersInRange(loc, 40);

					mWorld.playSound(mBoss.getLocation(), Sound.ITEM_TRIDENT_THROW, 1.5f, 0.85f);
					new BukkitRunnable() {
						double rotation = 0;

						double sin = 0;
						double y = 0;
						@Override
						public void run() {
							Location loc = mBoss.getLocation();
							mWorld.spawnParticle(Particle.CLOUD, loc, 4, 0.1, 0.1, 0.1, 0.15);
							mWorld.spawnParticle(Particle.CRIT, loc, 12, 0.1, 0.1, 0.1, 0.85);
							mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.85f + (mRand.nextFloat() * 0.5f));
							for (int i = 0; i < 2; i++) {
								rotation += 10;
								sin += 0.1;
								y = 1 + Math.sin(sin);
								double radian1 = Math.toRadians(rotation);
								loc.add(Math.cos(radian1) * radius, y, Math.sin(radian1) * radius);
								mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 1, 0, 0, 0, 0.025);
								mWorld.spawnParticle(Particle.CLOUD, loc, 2, 0.1, 0.1, 0.1, 0.025);
								mWorld.spawnParticle(Particle.CRIT, loc, 4, 0.1, 0.1, 0.1, 0.2);
								loc.subtract(Math.cos(radian1) * radius, y, Math.sin(radian1) * radius);

								/*
								 * Check if this hits a player
								 * A player can only be hit once per cast of this attack
								 */
								Iterator<Player> iter = playersNearby.iterator();
								while (iter.hasNext()) {
									Player player = iter.next();

									if (player.getLocation().distance(loc) < bladeDamageRadius) {
										DamageUtils.damage(mBoss, player, 6);
										Utils.KnockAway(mBoss.getLocation(), player, 0.5f, 0.65f);
										iter.remove();
									}
								}
							}

							if (rotation >= 360) {
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
	public int duration() {
		return 20 * 9;
	}

}
