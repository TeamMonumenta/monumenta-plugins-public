package com.playmonumenta.plugins.bosses.spells.xenotopsis;

import com.playmonumenta.plugins.bosses.bosses.Xenotopsis;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class FearfulSouls extends Spell {
	// the duration of the attack in ticks (> 10)
	private static final int DURATION = 20 * 11;

	// the base cooldown of the attack, which is modified by the cooldown ticks provided by the boss
	private static final int BASE_COOLDOWN = 20 * 2;

	// the speed and bidirectional variance in speed that each skull moves, in blocks per tick
	private static final double SKULL_SPEED = 0.16;
	private static final double SKULL_SPEED_VARIANCE = 0.05;

	// the variance in the height from the normal path that each skull moves on
	private static final double SKULL_HEIGHT_VARIANCE = 0.5;

	// the attack and death damage of each skull
	private static final int ATTACK_DAMAGE = 120;
	private static final int DEATH_DAMAGE = 8;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Xenotopsis mXenotopsis;
	private final int mCooldownTicks;

	private final int mSkullsPerPlayer;

	public FearfulSouls(Plugin plugin, LivingEntity boss, Xenotopsis xenotopsis, int cooldownTicks, int skullsPerPlayer) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mCooldownTicks = cooldownTicks;
		mXenotopsis = xenotopsis;

		mSkullsPerPlayer = skullsPerPlayer;
	}

	@Override
	public boolean canRun() {
		return mXenotopsis.canRunSpell(this);
	}

	@Override
	public void run() {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			//final Map<Player, Pair<ArrayList<ArmorStand>, ArrayList<Pair<Double, Double>>>> mSkullsAndTargets = new HashMap<>();
			// double pair right = height
			// double pair left = speed
			final Map<Player, List<FearfulSoulData>> mSkullsAndTargets = new HashMap<>();

			@Override
			public void run() {
				// create skulls
				if (mTicks == 20) {
					for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), Xenotopsis.DETECTION_RANGE, true)) {
						List<FearfulSoulData> skulls = new ArrayList<>();
						for (int i = 0; i < mSkullsPerPlayer; i++) {
							Location skullLocation = mBoss.getLocation().clone().add(FastUtils.randomDoubleInRange(-1.5, 1.5), FastUtils.randomDoubleInRange(0.2, 2) - 0.9, FastUtils.randomDoubleInRange(-1.5, 1.5));
							ArmorStand skull = (ArmorStand) LibraryOfSoulsIntegration.summon(skullLocation, "XenotopsisFearfulSoul"); // create a skull with a random position, accounting for the height of the armor stand

							if (skull == null) {
								continue;
							}

							// particle effect for each skull summon
							new PartialParticle(Particle.SPELL_MOB, skull.getLocation().clone().add(0, 0.9, 0))
								.count(10)
								.delta(0.12)
								.spawnAsBoss();
							new PartialParticle(Particle.SPELL, skull.getLocation().clone().add(0, 0.9, 0))
								.count(10)
								.delta(0.15)
								.spawnAsBoss();

							// set the rotation of each skull to be the same as the boss
							skull.setRotation(mBoss.getLocation().getYaw(), 0);

							double randomSkullSpeed = SKULL_SPEED + FastUtils.randomDoubleInRange((2.0 / mSkullsPerPlayer) * skulls.size() * SKULL_SPEED_VARIANCE, (2.0 / mSkullsPerPlayer) * (skulls.size() + 1) * SKULL_SPEED_VARIANCE) - SKULL_SPEED_VARIANCE;
							double randomSkullHeightDifference = FastUtils.randomDoubleInRange(-SKULL_HEIGHT_VARIANCE, SKULL_HEIGHT_VARIANCE);
							skulls.add(new FearfulSoulData(skull, randomSkullSpeed, randomSkullHeightDifference));
						}
						mSkullsAndTargets.put(player, skulls);
					}

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PARROT_IMITATE_PHANTOM, SoundCategory.HOSTILE, 3f, 0.8f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PARROT_IMITATE_PHANTOM, SoundCategory.HOSTILE, 3f, 0.65f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PARROT_IMITATE_PHANTOM, SoundCategory.HOSTILE, 3f, 0.5f);
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 2f, 0.62f);
				}

				if (mTicks == 0) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ALLAY_DEATH, SoundCategory.HOSTILE, 3f, 0.7f);
				}

				if (mTicks < 20) {
					new PPCircle(Particle.SPELL, mBoss.getLocation().clone().add(0, 0.2, 0), 3 * ((double)(20 - mTicks) / 20))
						.count(30)
						.ringMode(true)
						.spawnAsBoss();
				}

				// move the skulls toward their respective targets
				if (mTicks >= 40 && mTicks < DURATION) {
					mSkullsAndTargets.keySet().forEach(target -> {
						if (mSkullsAndTargets.get(target) != null) {
							mSkullsAndTargets.get(target).forEach(skullData -> {
								if (mSkullsAndTargets.get(target) != null) {
									ArmorStand skull = skullData.skull;
									double skullSpeed = skullData.speed;
									double skullHeight = skullData.height;

									Vector direction = target.getEyeLocation().clone().add(0, -0.9, 0).toVector().subtract(skull.getLocation().clone().add(0, -skullHeight, 0).toVector()).normalize();

									if (skull.getLocation().distance(target.getLocation()) > 8) {
										skullSpeed *= 2.5;
									}
									skull.teleport(skull.getLocation().clone().add(direction.multiply(skullSpeed)));

									skull.setRotation((float) Math.toDegrees(Math.atan2(direction.getZ(), direction.getX()) - Math.PI / 2), (float) Math.toDegrees(Math.cos(direction.getY())));
								}
							});
						}
					});
				}

				// play a sound at each skull
				if (mTicks % 8 == 0) {
					mSkullsAndTargets.values().forEach(skullList -> skullList.forEach(skullData -> {
						ArmorStand skull = skullData.skull;
						mWorld.playSound(skull.getLocation(), Sound.ENTITY_VEX_AMBIENT, SoundCategory.HOSTILE, 0.95f, 0.5f);
					}));
				}

				// check if target is too far away
				if (mTicks % 10 == 0) {
					mSkullsAndTargets.keySet().forEach(player -> {
						if (mBoss.getLocation().distance(player.getLocation()) > 50) {
							if (mSkullsAndTargets.get(player) != null) {
								mSkullsAndTargets.get(player).forEach(skullData -> skullData.skull.remove());
							}
						}
					});
				}

				// remove the skulls
				if (mTicks == DURATION) {
					mSkullsAndTargets.values().forEach(skullList -> skullList.forEach(skullData -> {
						ArmorStand skull = skullData.skull;

						skull.remove();

						// particle effect for each skull summon
						new PartialParticle(Particle.SPELL_MOB, skull.getLocation().clone().add(0, 0.9, 0))
							.count(10)
							.delta(0.12)
							.spawnAsBoss();
						new PartialParticle(Particle.SPELL, skull.getLocation().clone().add(0, 0.9, 0))
							.count(10)
							.delta(0.15)
							.spawnAsBoss();

						mWorld.playSound(skull.getLocation(), Sound.ENTITY_PHANTOM_HURT, SoundCategory.HOSTILE, 0.7f, 1.7f);
					}));
				}

				// particle effect
				mSkullsAndTargets.values().forEach(skullList -> skullList.forEach(skullData -> {
					ArmorStand skull = skullData.skull;
					new PartialParticle(Particle.REDSTONE, skull.getLocation().clone().add(0, 0.8, 0).subtract(skull.getLocation().getDirection().clone().multiply(0.3)))
						.count(1)
						.data(new Particle.DustOptions(Color.fromRGB(224, 224, 224), 1.9f))
						.spawnAsBoss();
				}));

				// check for collisions
				Map<Player, ArmorStand> toRemove = new HashMap<>(); // this sucks, but is needed to avoid concurrent modification exception
				mSkullsAndTargets.keySet().forEach(target -> {
					if (mSkullsAndTargets.get(target) != null) {
						mSkullsAndTargets.get(target).forEach(skullData -> {
							ArmorStand skull = skullData.skull;
							Hitbox hitbox = new Hitbox.AABBHitbox(mWorld, BoundingBox.of(skull.getLocation().clone().add(0, 0.9, 0), 0.5, 0.5, 0.5));
							for (Player player : hitbox.getHitPlayers(true)) {
								skull.remove();
								toRemove.put(target, skull);
								new PartialParticle(Particle.SPELL, target.getLocation().clone().add(0, 0.4, 0))
									.count(20)
									.delta(0.2, 0.5, 0.2)
									.spawnAsBoss();
								new PartialParticle(Particle.SPELL_MOB, target.getLocation().clone().add(0, 0.4, 0))
									.count(20)
									.delta(0.2, 0.5, 0.2)
									.spawnAsBoss();

								BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, mXenotopsis.scaleDamage(ATTACK_DAMAGE), "Fearful Souls", skull.getLocation(), Xenotopsis.SHIELD_STUN_TIME);
								mXenotopsis.changePlayerDeathValue(player, DEATH_DAMAGE, false);
								//MovementUtils.knockAwayRealistic(skull.getLocation(), player, 0.3f, 0.35f, true);

								mWorld.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_BITE, SoundCategory.HOSTILE, 1.5f, 0.9f);
								mWorld.playSound(player.getLocation(), Sound.ENTITY_PHANTOM_HURT, SoundCategory.HOSTILE, 1.2f, 1.2f);
							}
						});
					}
				});
				toRemove.keySet().forEach(target -> {
					if (mSkullsAndTargets.get(target) != null) {
						mSkullsAndTargets.get(target).removeIf(skullData -> skullData.skull.equals(toRemove.get(target)));
					}
				});

				mTicks++;
				if (mTicks > DURATION || mBoss.isDead()) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				mSkullsAndTargets.values().forEach(skullList -> skullList.forEach(skullData -> skullData.skull.remove()));

				super.cancel();
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return BASE_COOLDOWN + mCooldownTicks;
	}

	// record class usage without constructor creates unused variable warnings despite not having issues
	@SuppressWarnings("UnusedVariable")
	private record FearfulSoulData(ArmorStand skull, double speed, double height) {}
}
