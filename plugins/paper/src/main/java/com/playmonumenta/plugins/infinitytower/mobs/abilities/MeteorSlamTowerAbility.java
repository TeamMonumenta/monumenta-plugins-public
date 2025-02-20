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
import com.playmonumenta.plugins.utils.ParticleUtils;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class MeteorSlamTowerAbility extends TowerAbility {
	public MeteorSlamTowerAbility(Plugin plugin, String identityTag, LivingEntity boss, TowerGame game, TowerMob mob, boolean isPlayerMob) {
		super(plugin, identityTag, boss, game, mob, isPlayerMob);

		Spell spell = new Spell() {
			private final World mWorld = mBoss.getWorld();

			public @Nullable LivingEntity getTarget() {
				List<LivingEntity> targets = mIsPlayerMob ? mGame.getFloorMobs() : mGame.getPlayerMobs();
				Collections.shuffle(targets);
				if (targets.size() > 0) {
					return targets.get(0);
				}
				return null;

			}

			@Override
			public void run() {
				final LivingEntity target = getTarget();
				if (target == null || mGame.isGameEnded()) {
					return;
				}

				final Location targetLocation = target.getLocation();
				final Location loc = mBoss.getEyeLocation();
				mWorld.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.PLAYERS, 1, 1);
				new PartialParticle(Particle.LAVA, loc, 15, 1, 0f, 1, 0).spawnAsEntityActive(mBoss);

				Vector offset = targetLocation.clone().subtract(loc).toVector().normalize();
				Location moveTo = loc.clone().add(offset);

				//double distance = moveTo.distance(targetLocation);
				Vector velocity = targetLocation.subtract(moveTo).toVector().multiply(0.05);
				velocity.setY(1);

				BukkitRunnable leap = new BukkitRunnable() {
					final Vector mFinalVelocity = velocity;
					boolean mLeaping = false;
					boolean mHasBeenOneTick = false;
					int mTime = 0;

					@Override
					public void run() {
						mTime++;

						if (mTime >= 160) {
							cancel();
							return;
						}

						if (!mBoss.isValid() || mBoss.isDead() || mGame.isTurnEnded()) {
							cancel();
							return;
						}

						Location loc = mBoss.getLocation();
						if (!mLeaping) {
							mWorld.playSound(loc, Sound.ENTITY_HORSE_JUMP, SoundCategory.PLAYERS, 1, 1);
							new PartialParticle(Particle.LAVA, loc, 15, 1, 0f, 1, 0).spawnAsEntityActive(mBoss);
							mBoss.setVelocity(mFinalVelocity);
							mLeaping = true;
						} else {
							new PartialParticle(Particle.REDSTONE, loc, 4, 0.5, 0.5, 0.5, 1, new Particle.DustOptions(Color.fromRGB(255, 255, 255), 1.0f)).spawnAsEntityActive(mBoss);
							mBoss.setFallDistance(0);
							if (mBoss.isOnGround() && mHasBeenOneTick) {
								List<LivingEntity> targets = new ArrayList<>(mIsPlayerMob ? mGame.mFloorMobs : mGame.mPlayerMobs);
								for (LivingEntity target : targets) {
									if (target.getLocation().distance(loc) <= 4 && target.isValid() && !target.isDead() && !mGame.isTurnEnded()) {
										DamageUtils.damage(mBoss, target, DamageEvent.DamageType.MAGIC, 14, null, false, true);
									}
								}

								ParticleUtils.explodingRingEffect(plugin, loc, 4, 1, 4,
									List.of(
										new AbstractMap.SimpleEntry<Double, ParticleUtils.SpawnParticleAction>(0.5, (Location location) -> {
											new PartialParticle(Particle.FLAME, loc, 1, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(mBoss);
											new PartialParticle(Particle.CLOUD, loc, 1, 0.1, 0.1, 0.1, 0.1).spawnAsEntityActive(mBoss);
										})
									));
								mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.3F, 0);
								mWorld.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2, 1.25F);
								new PartialParticle(Particle.FLAME, loc, 60, 0F, 0F, 0F, 0.2F).spawnAsEntityActive(mBoss);
								new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 20, 0F, 0F, 0F, 0.3F).spawnAsEntityActive(mBoss);
								this.cancel();
								return;
							}

							Vector towardsPlayer = target.getLocation().subtract(mBoss.getLocation()).toVector().setY(0).normalize();
							Vector originalVelocity = mBoss.getVelocity();
							double scale = 0.5;
							Vector newVelocity = new Vector();
							newVelocity.setX((originalVelocity.getX() * 20 + towardsPlayer.getX() * scale) / 20);
							// Use the original mob's vertical velocity, so it doesn't somehow fall faster than gravity
							newVelocity.setY(originalVelocity.getY());
							newVelocity.setZ((originalVelocity.getZ() * 20 + towardsPlayer.getZ() * scale) / 20);
							mBoss.setVelocity(newVelocity);

							// At least one tick has passed to avoid instant smacking a nearby player
							mHasBeenOneTick = true;
						}
					}
				};

				if (!mGame.isTurnEnded()) {
					leap.runTaskTimer(mPlugin, 0, 1);
				}

			}

			@Override
			public int cooldownTicks() {
				return 160;
			}
		};



		SpellManager activeSpells = new SpellManager(List.of(spell));

		super.constructBoss(activeSpells, Collections.emptyList(), -1, null, (int) (FastUtils.RANDOM.nextDouble() * 100) + 20);

	}
}
