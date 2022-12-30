package com.playmonumenta.plugins.infinitytower.mobs.abilities;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.infinitytower.TowerGame;
import com.playmonumenta.plugins.infinitytower.TowerMob;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class EarthquakeTowerAbility extends TowerAbility {
	private static final int RADIUS = 3;
	private static final int DURATION = 40;
	private static final int COOLDOWN = 20 * 5;
	private static final double KNOCK_UP_SPEED = 1.5f;
	private static final int DAMAGE = 16;



	public EarthquakeTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);


		Spell spell = new Spell() {
			@Override
			public void run() {

				List<LivingEntity> list = (mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs());
				Collections.shuffle(list);

				for (LivingEntity target : list) {

					new BukkitRunnable() {
						int mTicks = 0;
						final World mWorld = mBoss.getWorld();
						final Location mTargetLoc = target.getLocation();

						@Override
						public void run() {
							if (isCancelled()) {
								return;
							}

							if (!mBoss.isValid() || mBoss.isDead() || mGame.isTurnEnded()) {
								cancel();
								return;
							}


							if (mTicks <= (DURATION - 5)) {
								mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, SoundCategory.HOSTILE, 1f, 0.25f + ((float) mTicks / 100));
							}


							if (mTicks % 2 == 0) {
								new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, mTargetLoc, 1, ((double) RADIUS * 2) / 2, ((double) RADIUS * 2) / 2, ((double) RADIUS * 2) / 2, 0.05).spawnAsEntityActive(mBoss);
							}
							new PartialParticle(Particle.BLOCK_CRACK, mTargetLoc, 2, ((double) RADIUS) / 2, 0.1, ((double) RADIUS) / 2, Bukkit.createBlockData(Material.STONE)).spawnAsEntityActive(mBoss);


							if (mTicks % 20 == 0 && mTicks > 0) {
								mWorld.playSound(mTargetLoc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1f, 0.5f);
								mWorld.playSound(mTargetLoc, Sound.BLOCK_GRAVEL_BREAK, 1f, 0.5f);
								for (int i = 0; i < 360; i += 18) {
									new PartialParticle(Particle.SMOKE_NORMAL, mTargetLoc.clone().add(FastUtils.cos(Math.toRadians(i)) * ((double) RADIUS), 0.2, FastUtils.sin(Math.toRadians(i)) * ((double) RADIUS)), 1, 0.1, 0.1, 0.1, 0).spawnAsEntityActive(mBoss);
								}
								new PartialParticle(Particle.BLOCK_CRACK, mTargetLoc, 80, ((double) RADIUS) / 2, 0.1, ((double) RADIUS) / 2, Bukkit.createBlockData(Material.DIRT)).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, mTargetLoc, 8, ((double) RADIUS) / 2, 0.1, ((double) RADIUS) / 2, 0).spawnAsEntityActive(mBoss);
							}


							if (mTicks % 10 == 0) {
								new PartialParticle(Particle.LAVA, mTargetLoc, 1, 0.25, 0.25, 0.25, 0.1).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.DRIP_LAVA, mBoss.getEyeLocation(), 1, 0.25, 0.45, 0.25, 1).spawnAsEntityActive(mBoss);
							}

							if (mTicks >= DURATION) {
								new BukkitRunnable() {

									int mTimer2 = 0;
									@Override
									public void run() {
										if (mTimer2 >= 2) {
											cancel();
										}

										if (isCancelled()) {
											return;
										}

										if (!mBoss.isValid() || mBoss.isDead() || mGame.isTurnEnded()) {
											cancel();
											return;
										}

										mWorld.playSound(mTargetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.35f);
										mWorld.playSound(mTargetLoc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
										mWorld.playSound(mTargetLoc, Sound.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR, 1.5f, 0.5f);

										new PartialParticle(Particle.CLOUD, mTargetLoc, 150, 0, 0, 0, 0.5).spawnAsEntityActive(mBoss);
										new PartialParticle(Particle.LAVA, mTargetLoc, 35, ((double) RADIUS) / 2, 0.1, ((double) RADIUS) / 2, 0).spawnAsEntityActive(mBoss);
										new PartialParticle(Particle.BLOCK_CRACK, mTargetLoc, 200, ((double) RADIUS) / 2, 0.1, ((double) RADIUS) / 2, Bukkit.createBlockData(Material.DIRT)).spawnAsEntityActive(mBoss);
										new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, mTargetLoc, 35, ((double) RADIUS) / 2, 0.1, ((double) RADIUS) / 2, 0.1).spawnAsEntityActive(mBoss);

										for (int i = 0; i < 100; i++) {
											new PartialParticle(Particle.SMOKE_LARGE, mTargetLoc.clone().add(-3 + FastUtils.RANDOM.nextDouble() * 6, 0.1, -3 + FastUtils.RANDOM.nextDouble() * 6), 0, 0, 1, 0, 0.2 + FastUtils.RANDOM.nextDouble() * 0.4).spawnAsEntityActive(mBoss);
										}

										if (mTimer2 == 0) {
											for (LivingEntity target : new ArrayList<>(mIsPlayerMob ? mGame.mFloorMobs : mGame.mPlayerMobs)) {
												if (target.isValid() && !target.isDead() && !mGame.isTurnEnded() && target.getLocation().distance(mTargetLoc) <= RADIUS) {
													DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, DAMAGE);
													target.setVelocity(target.getVelocity().add(new Vector(0.0, KNOCK_UP_SPEED, 0.0)));
												}
											}
										}

										mTimer2++;
									}
								}.runTaskTimer(mPlugin, 0, 1);

								cancel();
								return;
							}

							mTicks++;
						}
					}.runTaskTimer(mPlugin, 0, 1);
					break;
				}

			}

			@Override
			public int cooldownTicks() {
				return COOLDOWN;
			}
		};

		SpellManager manager = new SpellManager(List.of(spell));

		super.constructBoss(manager, Collections.emptyList(), -1, null, (int) (FastUtils.RANDOM.nextDouble() * 30) + 20);
	}
}
