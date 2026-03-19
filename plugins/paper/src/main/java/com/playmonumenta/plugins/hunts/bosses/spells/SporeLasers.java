package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.effects.EffectManager;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.hunts.bosses.SporousAmalgam;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.particle.ParticleCategory;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SporeLasers extends Spell {
	private static final int TELEGRAPH_DURATION = (int) (20 * 1.5f);
	private static final int BEAM_DURATION = 20 * 8;
	private static final int BEAM_FREQUENCY = 20 * 12 + BEAM_DURATION;
	private static final float BEAM_SPORE_AMOUNT = 1.25f;
	private static final double LASER_DAMAGE = 45;

	private static final int HOMING_PROJECTILE_STASIS_DURATION = (int) (20 * 1.5);
	private static final int PLAYERS_PER_HOMING_P = 3;
	private static final double HOMING_SPEED = 4.4 / 20.0;
	private static final double HOMING_TURN_RADIUS = (2 * Math.PI) / 3.0 / 20.0;
	private static final double HOMING_DURATION = 20 * 7 + HOMING_PROJECTILE_STASIS_DURATION;
	private static final int PROJECTILE_SPAWN_DELAY = (int) (20 * 0.75);

	private final Plugin mPlugin;
	private final SporousAmalgam mSporeBeast;
	private final LivingEntity mBoss;

	private Axis mZAxis;
	private Axis mXAxis;

	private int mTicks;

	public SporeLasers(Plugin plugin, SporousAmalgam sporeBeast) {
		mPlugin = plugin;
		mSporeBeast = sporeBeast;
		mBoss = sporeBeast.mBoss;
		mTicks = BEAM_FREQUENCY / 2;
		getArenaEdgeLocations(sporeBeast.mBoss);
	}

	@Override
	public void run() {
		if (mTicks++ % BEAM_FREQUENCY == 0) {
			runBeam();
		}
	}

	private void runHomingProjectile(int projectileAmount) {
		ArrayList<Player> targets = new ArrayList<>();
		int iterator = 0;
		while (iterator < projectileAmount) {
			Player p = FastUtils.getRandomElement(mSporeBeast.getValidPlayersForBanishTargeting());
			if (!targets.contains(p)) {
				targets.add(p);
				iterator++;
			}
			List<Player> checkList = mSporeBeast.getValidPlayersForBanishTargeting();
			checkList.removeAll(targets);
			if (checkList.isEmpty()) {
				mSporeBeast.clearPlayerThatSawBanish();
			}
		}

		for (int i = 0; i < projectileAmount; i++) {
			int targetIndex = i;
			new BukkitRunnable() {
				final Location mSpellLocation = mBoss.getLocation().add(0, 2, 0);
				final Player mTarget = targets.get(targetIndex);
				final Vector mSpeed = LocationUtils.getDirectionTo(mTarget.getLocation(), mSpellLocation);
				int mTicks = -PROJECTILE_SPAWN_DELAY;

				@Override
				public void run() {
					if (mTicks == 0) {
						mTarget.playSound(mTarget, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.95f);

					}
					if (mTicks < HOMING_PROJECTILE_STASIS_DURATION) {
						doParticles();
					} else if (mTicks == HOMING_PROJECTILE_STASIS_DURATION) {
						mTarget.playSound(mTarget, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.85f);
						mTarget.playSound(mTarget, Sound.ENTITY_ELDER_GUARDIAN_CURSE, 1f, 0.85f);
					} else {
						Vector direction = mTarget.getLocation().add(0, 1, 0).subtract(mSpellLocation).toVector().normalize();
						Vector axis = mSpeed.clone().crossProduct(direction);
						double angle = mSpeed.angle(direction);
						mSpeed.rotateAroundAxis(axis, Math.min(angle, HOMING_TURN_RADIUS));
						mSpellLocation.add(mSpeed.clone().multiply(HOMING_SPEED));
						doParticles();

						if (new Hitbox.SphereHitbox(mSpellLocation, 0.45).getHitPlayers(true).contains(mTarget)) {
							mSporeBeast.doBanishSequence(mTarget);

							this.cancel();
						}

						if (this.isCancelled()) {
							extinguishSound();
							return;
						}

						if (mTicks % 8 == 0) {
							mTarget.playSound(mSpellLocation, Sound.BLOCK_CONDUIT_ATTACK_TARGET, 1f, 1f);
							mTarget.playSound(mSpellLocation, Sound.BLOCK_CONDUIT_ATTACK_TARGET, 1f, 1f);
							mTarget.playSound(mSpellLocation, Sound.BLOCK_CONDUIT_ATTACK_TARGET, 1f, 1f);
						}
					}

					if (mTicks >= HOMING_DURATION) {
						extinguishSound();
						this.cancel();
					}

					if (mBoss.isDead() || mTarget.isDead()) {
						this.cancel();
					}

					mTicks++;
				}

				private void extinguishSound() {
					for (int i = 0; i < 5; i++) {
						mTarget.playSound(mSpellLocation, Sound.BLOCK_CANDLE_EXTINGUISH, 1.3f, 0.7f);
					}
				}

				private void doParticles() {
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, mSpellLocation, 3)
						.data(new Particle.DustTransition(Color.fromRGB(82, 50, 89), Color.fromRGB(134, 51, 58), 2f))
						.spawnForPlayer(ParticleCategory.BOSS, mTarget);

					new PartialParticle(Particle.GLOW, mSpellLocation, 3).spawnForPlayer(ParticleCategory.BOSS, mTarget);
				}

			}.runTaskTimer(mPlugin, 0, 1);
		}
	}

	private void runBeam() {
		Axis beamAxis = FastUtils.randomBoolean() ? mZAxis : mXAxis;
		new BukkitRunnable() {
			final BoundingBox mBoundingBox = new BoundingBox(beamAxis.mNegativeLocation.getX(), beamAxis.mNegativeLocation.getY(),
				beamAxis.mNegativeLocation.getZ(), beamAxis.mPositiveLocation.getX(), beamAxis.mPositiveLocation.getY(), beamAxis.mPositiveLocation.getZ());

			final Hitbox.AABBHitbox mHitbox = new Hitbox.AABBHitbox(mBoss.getWorld(), mBoundingBox);

			boolean mDoneTelegraph = false;
			boolean mDoneBeamVisuals = false;

			int mTicks = 0;

			@Override
			public void run() {
				if (!mDoneTelegraph) {
					mDoneTelegraph = true;
					doTelegraph(beamAxis);
				}

				if (mTicks > TELEGRAPH_DURATION && mTicks < BEAM_DURATION + TELEGRAPH_DURATION) {
					if (!mDoneBeamVisuals) {
						mDoneBeamVisuals = true;
						doBeamVisuals(beamAxis);
						mBoss.getWorld().playSound(mBoss, Sound.BLOCK_BEACON_ACTIVATE, 2f, 1f);
					}
					for (Player p : mHitbox.getHitPlayers(true)) {
						p.playSound(p, Sound.UI_STONECUTTER_TAKE_RESULT, 0.8f, 1f);
						p.playSound(p, Sound.UI_STONECUTTER_TAKE_RESULT, 0.8f, 0.8f);
						p.playSound(p, Sound.UI_STONECUTTER_TAKE_RESULT, 0.8f, 0.6f);
						p.addPotionEffect(new PotionEffect(PotionEffectType.POISON, 20 * 2, 1, false, false, false));
						if (MetadataUtils.checkOnceInRecentTicks(mPlugin, p, "HitBySpore", 10)) {
							mSporeBeast.addSpores(p, BEAM_SPORE_AMOUNT);
							EffectManager.getInstance().addEffect(p, "Spore Laser", new PercentSpeed(20 * 5, -0.3, "Spore Slowness"));
							DamageUtils.damage(mBoss, p, DamageEvent.DamageType.MAGIC, LASER_DAMAGE);
						}
					}
				}
				if (mTicks == BEAM_DURATION + TELEGRAPH_DURATION) {
					mBoss.getWorld().playSound(mBoss, Sound.BLOCK_BEACON_DEACTIVATE, 2f, 1f);
					int playerCount = mSporeBeast.getPlayersInOutRange().size();
					if (playerCount > 0) {
						runHomingProjectile(playerCount / PLAYERS_PER_HOMING_P + 1);
					}
					this.cancel();
				}
				if (mBoss.isDead()) {
					this.cancel();
				}
				mTicks += 1;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void doBeamVisuals(Axis axis) {
		new BukkitRunnable() {
			final PPLine mBeamLine = new PPLine(Particle.SNEEZE, axis.mNegativeLocation, axis.mPositiveLocation).countPerMeter(1.5).delta(0, -0.5, 0).extra(0.20).directionalMode(true);
			int mTicks = 0;

			@Override
			public void run() {
				if (mTicks % 12 == 0) {
					mBeamLine.offset(FastUtils.randomDoubleInRange(0, 1)).spawnAsBoss();
				}

				if (mTicks >= BEAM_DURATION - 10 || mBoss.isDead()) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void doTelegraph(Axis axis) {
		new BukkitRunnable() {
			final PartialParticle mTelegraphParticle = new PartialParticle(Particle.ELECTRIC_SPARK, axis.mMidPoint, 2);
			final double mShiftDistance = axis.mMidPoint.distance(axis.mPositiveLocation) / (double) TELEGRAPH_DURATION;
			final Vector mPositiveShift = LocationUtils.getDirectionTo(axis.mMidPoint, axis.mPositiveLocation).multiply(mShiftDistance);
			final Vector mNegativeShift = LocationUtils.getDirectionTo(axis.mMidPoint, axis.mNegativeLocation).multiply(mShiftDistance);
			int mTicks = 4;

			@Override
			public void run() {
				Location negativeLoc = axis.mNegativeLocation.clone();
				Location positiveLoc = axis.mPositiveLocation.clone();

				for (int i = 0; i < mTicks; i++) {
					mTelegraphParticle.location(negativeLoc.add(mNegativeShift)).spawnAsBoss();
					mTelegraphParticle.location(positiveLoc.add(mPositiveShift)).spawnAsBoss();
				}
				if (mTicks >= TELEGRAPH_DURATION || mBoss.isDead()) {
					this.cancel();
				}
				mTicks += 1;
			}
		}.runTaskTimer(mPlugin, 0, 1);

	}

	private void getArenaEdgeLocations(LivingEntity boss) {
		Location baseLocation = boss.getLocation().add(0, 1.5, 0);
		int arenaRadius = SporousAmalgam.SPELL_INNER_RADIUS;
		Location mPositiveXLoc = baseLocation.clone().add(arenaRadius, 0, 0);
		Location mNegativeXLoc = baseLocation.clone().add(-arenaRadius, 0, 0);
		Location mPositiveZLoc = baseLocation.clone().add(0, 0, arenaRadius);
		Location mNegativeZLoc = baseLocation.clone().add(0, 0, -arenaRadius);
		mXAxis = new Axis(mPositiveXLoc, mNegativeXLoc, baseLocation);
		mZAxis = new Axis(mPositiveZLoc, mNegativeZLoc, baseLocation);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	private record Axis(Location mPositiveLocation, Location mNegativeLocation, Location mMidPoint) {
	}
}
