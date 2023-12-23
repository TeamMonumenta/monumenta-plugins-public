package com.playmonumenta.plugins.depths.bosses.spells.vesperidys;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.depths.bosses.Vesperidys;
import com.playmonumenta.plugins.effects.PercentDamageReceived;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellVesperidysFeintParticleBeam extends Spell {
	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final Vesperidys mVesperidys;

	private final int mNumStrikes;
	private final int mStrikeDelay;
	private final int mMaxTargets;

	private static final int SHOCK_RADIUS = 3;
	private static final int SHOCK_VERTICAL_RANGE = 10;
	private static final int SHOCK_DELAY_TICKS = (int) (1.75 * Constants.TICKS_PER_SECOND);
	private static final double DAMAGE = 70;

	private boolean mOnCooldown = false;

	public SpellVesperidysFeintParticleBeam(Plugin plugin, LivingEntity boss, Vesperidys vesperidys, int numStrikes, int strikeDelay, int maxTargets) {
		mPlugin = plugin;
		mBoss = boss;
		mVesperidys = vesperidys;
		mNumStrikes = numStrikes;
		mStrikeDelay = strikeDelay;
		mMaxTargets = maxTargets;
	}

	@Override
	public void run() {
		mOnCooldown = true;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			mOnCooldown = false;
		}, cooldownTicks() + 20);

		mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 3f, 1.5f);

		BukkitRunnable runnableB = new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks > mStrikeDelay) {
					this.cancel();

					BukkitRunnable runnableB = new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							if (mT > (mStrikeDelay * (mNumStrikes - 1) + SHOCK_DELAY_TICKS)) {
								this.cancel();
							}
							mT++;
						}
					};
					runnableB.runTaskTimer(mPlugin, 0, 1);
					mActiveRunnables.add(runnableB);
				}

				mTicks += 2;
			}
		};
		runnableB.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnableB);

		List<Player> playerList = PlayerUtils.playersInRange(mVesperidys.mSpawnLoc, Vesperidys.detectionRange, true);
		Collections.shuffle(playerList);

		int count = 0;
		for (Player player : playerList) {
			if (count >= mMaxTargets) {
				break;
			}

			startBeam(player.getLocation(), player, 0, 0, 0);
			count++;
		}

		if (count < mMaxTargets) {
			List<Vesperidys.Platform> platforms = mVesperidys.mPlatformList.getRandomPlatforms(null, mMaxTargets - count);
			double angle = FastUtils.randomDoubleInRange(0, 2*Math.PI);

			for (Vesperidys.Platform platform : platforms) {
				Player player = EntityUtils.getNearestPlayer(platform.getCenter(), Vesperidys.detectionRange);

				if (player != null) {
					double distance = 5;
					double offsetPerStrike = Math.PI / 3;

					startBeam(platform.getCenter(), player, angle, distance, offsetPerStrike);

					angle += 2 * Math.PI / 3;
				}
			}
		}
	}

	public void startBeam(Location startLocation, Player player, double angle, double distance, double angleOffset) {
		BukkitRunnable runnableA = new BukkitRunnable() {
			final Player mTargetPlayer = player;
			int mT = 0;
			int mStrikes = 0;
			double mAngle = angle;
			final Location mLocation = startLocation;

			@Override
			public void run() {
				if (mStrikes >= mNumStrikes) {
					this.cancel();
					return;
				}

				if (mT >= mStrikeDelay) {
					mT = 0;
					mStrikes += 1;

					Vector offset = new Vector(Math.sin(mAngle) * distance, 0, Math.cos(mAngle) * distance);
					mAngle += angleOffset;

					Location newLoc = mTargetPlayer.getLocation().add(offset);
					Vector dir = LocationUtils.getDirectionTo(newLoc, mLocation);
					dir.setY(0);
					dir.normalize().multiply(6);

					if (LocationUtils.xzDistance(newLoc, mLocation) <= 6) {
						mLocation.setX(newLoc.getX());
						mLocation.setZ(newLoc.getZ());
					} else {
						mLocation.add(dir);
					}

					startStrike(mLocation.clone());
				}
				mT++;
			}
		};
		runnableA.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnableA);
	}

	public void startStrike(Location strikeLocation) {
		World world = mBoss.getWorld();
		strikeLocation.setY(mVesperidys.mSpawnLoc.getY());

		BukkitRunnable runnable = new BukkitRunnable() {

			int mT = 0;

			@Override
			public void run() {
				if (mT > SHOCK_DELAY_TICKS) {
					Collection<Player> shockPlayers = PlayerUtils.playersInCylinder(
						strikeLocation,
						SHOCK_RADIUS,
						2 * SHOCK_VERTICAL_RANGE
					);
					shockPlayers.forEach((Player player) -> strikeShock(strikeLocation, player));

					world.playSound(strikeLocation, Sound.ITEM_TRIDENT_THUNDER, 1f, 0.75f);
					world.playSound(strikeLocation, Sound.ENTITY_DRAGON_FIREBALL_EXPLODE, 1f, 0.75f);

					BukkitRunnable runnableEnd = new BukkitRunnable() {
						int mT = 0;

						@Override
						public void run() {
							if (mT > 6 || (mT > 0 && mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 8)) {
								this.cancel();
								return;
							}

							for (int i = 0; i <= 8; i++) {
								double rad = FastUtils.randomDoubleInRange(0, 2 * Math.PI);
								Location l = strikeLocation.clone().add(0.2 * Math.cos(rad), 0.2 + FastUtils.randomDoubleInRange(0, 0.2), 0.2 * Math.sin(rad));
								Location center = strikeLocation.clone().add(0, 0.2, 0);
								Vector vector = l.toVector().subtract(center.toVector()).normalize();
								new PartialParticle(Particle.END_ROD, l).delta(vector.getX(), vector.getY(), vector.getZ())
									.count(1)
									.extra(FastUtils.randomDoubleInRange(0, 0.5))
									.directionalMode(true)
									.spawnAsBoss();
							}
							mT += 3;
						}
					};
					runnableEnd.runTaskTimer(mPlugin, 0, 3);
					mActiveRunnables.add(runnableEnd);

					int particleAmount = 20;
					if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 8) {
						particleAmount = 10;
					}
					ParticleUtils.drawRing(strikeLocation.clone().add(0, 0.1, 0), particleAmount, new Vector(0, 1, 0), 0.2,
						(l, t) -> {
							Location center = strikeLocation.clone().add(0, 0.1, 0);
							Vector vector = l.toVector().subtract(center.toVector()).normalize();
							new PartialParticle(Particle.END_ROD, l).delta(vector.getX(), vector.getY(), vector.getZ())
								.count(1)
								.extra(0.25)
								.directionalMode(true)
								.spawnAsBoss();
						}
					);

					int finalParticleAmount = particleAmount;
					Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
						ParticleUtils.drawRing(strikeLocation.clone().add(0, 0.1, 0), finalParticleAmount / 2, new Vector(0, 1, 0), SHOCK_RADIUS,
							(l, t) -> {
								Location center = strikeLocation.clone().add(0, 0.1, 0);
								Vector vector = l.toVector().subtract(center.toVector()).normalize();
								new PartialParticle(Particle.END_ROD, l).delta(vector.getX(), vector.getY(), vector.getZ())
									.count(1)
									.extra(0.25)
									.directionalMode(true)
									.spawnAsBoss();
							}
						);
					}, 10);

					this.cancel();
					return;
				}

				double progress = (double) mT / SHOCK_DELAY_TICKS;

				if (mT % 10 == 0) {
					new PPPillar(Particle.REDSTONE, strikeLocation, SHOCK_VERTICAL_RANGE)
						.count(2 * SHOCK_VERTICAL_RANGE)
						.data(new Particle.DustOptions(Color.fromRGB(255, 255, 0), 1))
						.spawnAsBoss();
				}

				if (mT % 5 == 0) {
					world.playSound(strikeLocation, Sound.ITEM_FIRECHARGE_USE, 1.0f, (float) (1f + 1f * (1 - progress)));
				}

				if (mT % 4 == 0) {

					ParticleUtils.drawRing(strikeLocation.clone().add(0, 0.1, 0), 25, new Vector(0, 1, 0), SHOCK_RADIUS * (1 - progress),
						(l, t) -> {
							Color color = Color.fromRGB(240 + FastUtils.randomIntInRange(-5, 5), (int) (240 * (1 - progress) + FastUtils.randomIntInRange(0, 5)), FastUtils.randomIntInRange(0, 5));

							new PartialParticle(Particle.REDSTONE, l).delta(0, 0, 0)
								.count(1)
								.extra(0.15)
								.data(new Particle.DustOptions(color, 1f))
								.spawnAsBoss();
						}
					);
				}

				if (mT % 4 == 0) {
					new PPCircle(Particle.REDSTONE, strikeLocation.clone().add(0, 0.1, 0), SHOCK_RADIUS)
						.count(25)
						.extra(0)
						.data(new Particle.DustOptions(Color.fromRGB(240 + FastUtils.randomIntInRange(-5, 5), 0, 240 + FastUtils.randomIntInRange(-5, 5)), 1))
						.spawnAsBoss();
				}

				mT++;
			}

		};

		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	public void strikeShock(
		Location strikeLocation,
		Player player
	) {
		BossUtils.blockableDamage(mBoss, player, DamageEvent.DamageType.MAGIC, DAMAGE, "Feint Particle Beam", strikeLocation);
		MovementUtils.knockAway(strikeLocation, player, 1f, 0.5f);

		if (mVesperidys.mParty != null && mVesperidys.mParty.getAscension() >= 8) {
			mPlugin.mEffectManager.addEffect(player, "VesperidysMagicVuln", new PercentDamageReceived(15 * 20, 0.3, EnumSet.of(DamageEvent.DamageType.MAGIC)));
		}
	}

	@Override public boolean canRun() {
		return !mOnCooldown;
	}

	@Override public int cooldownTicks() {
		return mVesperidys.mSpellCooldowns;
	}
}
