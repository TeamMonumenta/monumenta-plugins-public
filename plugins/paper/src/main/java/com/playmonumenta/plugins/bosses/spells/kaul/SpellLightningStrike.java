package com.playmonumenta.plugins.bosses.spells.kaul;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.bosses.Kaul;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PPPillar;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.Nullable;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;



public class SpellLightningStrike extends Spell {
	public static final DustOptions DUST_GRAY_LARGE = new DustOptions(Color.fromRGB(51, 51, 51), 5);
	public static final DustOptions DUST_YELLOW_LARGE = new DustOptions(Color.fromRGB(255, 255, 64), 1.25f);
	public static final DustOptions DUST_YELLOW_SMALL = new DustOptions(DUST_YELLOW_LARGE.getColor(), 0.75f);
	public static final DustOptions DUST_LIGHT_YELLOW_SMALL = new DustOptions(Color.fromRGB(255, 255, 128), 0.75f);

	// Fraction of arena participants to strike
	private static final int TARGETS_DIVISOR = 3;

	// Lightning shock
	private static final double SHOCK_DAMAGE_MULTIPLIER = 0.05;
	// Cylindrical dimensions from centre
	// (total height is twice of vertical range)
	private static final int SHOCK_RADIUS = 3;
	private static final int SHOCK_VERTICAL_RANGE = 10;
	private static final int SHOCK_INTERVAL_TICKS = 2;
	private static final int SHOCK_DELAY_TICKS = (int)(1.75 * Constants.TICKS_PER_SECOND);
	private static final int SHOCK_COUNT = 10;

	// Lingering fire
	private static final double FIRE_DAMAGE_MULTIPLIER = 0.1;
	private static final int FIRE_RADIUS = SHOCK_RADIUS;
	private static final int FIRE_DURATION_TICKS = (int)(5.25 * Constants.TICKS_PER_SECOND);
	private static final int FIRE_INTERVAL_TICKS = (int)(0.5 * Constants.TICKS_PER_SECOND);
	private static final int FIRE_DELAY_TICKS = (int)(0.25 * Constants.TICKS_PER_SECOND);
	private static final int FIRE_ALIGHT_TICKS = 3 * Constants.TICKS_PER_SECOND;

	private Kaul mKaul;
	private int mCooldownTicks;
	private int mRemainingCooldownTicks;
	private Location mCenter;

	public SpellLightningStrike(
		Kaul kaul,
		int cooldownSeconds,
		boolean startCooledDown,
		Location center
	) {
		mKaul = kaul;
		mCooldownTicks = cooldownSeconds * Constants.TICKS_PER_SECOND;
		if (startCooledDown) {
			mRemainingCooldownTicks = 0;
		} else {
			mRemainingCooldownTicks = mCooldownTicks;
		}
		mCenter = center;
	}

	@Override
	public void run() {
		// Cooldown fully pauses if totems are active
		if (SpellPutridPlague.getPlagueActive()) {
			return;
		}

		// Count of the tick this run, first being mCooldownTicks, last being 1.
		// If previous run was the last tick of cooldown (now tick #0),
		// cast and reset as this is now first mCooldownTicks tick
		if (mRemainingCooldownTicks <= 0) {
			mRemainingCooldownTicks = mCooldownTicks;
			Collection<Player> arenaParticipants = mKaul.getArenaParticipants();
			Collection<Player> targetPlayers = Collections.emptyList();

			int targetCount = arenaParticipants.size() / TARGETS_DIVISOR;
			if (targetCount < 1) {
				// Integer division by TARGETS_DIVISOR.
				// < TARGETS_DIVISOR players means everyone targeted
				targetPlayers = arenaParticipants;
			} else {
				ArrayList<Player> shuffledArenaParticipants = new ArrayList<>(arenaParticipants);
				Collections.shuffle(shuffledArenaParticipants);
				targetPlayers = shuffledArenaParticipants.subList(0, targetCount);
			}

			targetPlayers.forEach((Player targetPlayer) -> startStrike(targetPlayer));
		}

		// Count of the tick next run
		mRemainingCooldownTicks -= BossAbilityGroup.PASSIVE_RUN_INTERVAL_DEFAULT;
	}

	@Override
	public int cooldownTicks() {
		// We want to always run,
		// and manage cooldown ourselves (for startCooledDown logic)
		return 0;
	}

	public void startStrike(Player targetPlayer) {
		World world = targetPlayer.getWorld();
		Location strikeLocation = targetPlayer.getLocation();
		strikeLocation.setY(mCenter.getY());

		// P: Danger, tall markers
		new PPPillar(Particle.REDSTONE, strikeLocation, SHOCK_VERTICAL_RANGE)
			.count(2 * SHOCK_VERTICAL_RANGE)
			.data(DUST_YELLOW_SMALL).spawnAsBoss();
		new PPPillar(Particle.FIREWORKS_SPARK, strikeLocation.clone().subtract(0, SHOCK_VERTICAL_RANGE, 0), SHOCK_VERTICAL_RANGE)
			.count(2 * SHOCK_VERTICAL_RANGE).spawnAsBoss();


		// S: Thunder & distant sparks
		// /playsound entity.lightning_bolt.thunder master @p ~ ~ ~ 1 1.25
		world.playSound(
			strikeLocation,
			Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
			SoundCategory.HOSTILE,
			1,
			1.25f
		);
		// /playsound entity.lightning_bolt.thunder master @p ~ ~ ~ 0.75 1.5
		world.playSound(
			strikeLocation,
			Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
			SoundCategory.HOSTILE,
			0.75f,
			1.5f
		);
		// /playsound entity.lightning_bolt.thunder master @p ~ ~ ~ 1 1.75
		world.playSound(
			strikeLocation,
			Sound.ENTITY_LIGHTNING_BOLT_THUNDER,
			SoundCategory.HOSTILE,
			1,
			1.75f
		);
		// /playsound entity.firework_rocket.twinkle_far master @p ~ ~ ~ 0.75 1.75
		world.playSound(
			strikeLocation,
			Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
			SoundCategory.HOSTILE,
			0.75f,
			1.75f
		);

		// /particle dust 0.2 0.2 0.2 5 ~ ~10 ~ 1.5 0.25 1.5 0 5
		PartialParticle stormClouds = new PartialParticle(
			Particle.REDSTONE,
			strikeLocation.clone().add(0, SHOCK_VERTICAL_RANGE, 0),
			5,
			1.5,
			0.25,
			1.5,
			0,
			DUST_GRAY_LARGE
		);

		// /particle dust 1 1 0.25 1 ~ ~ ~ 0.75 0.25 0.75 0 5
		int electricRingMarkerCount = 8;
		PPCircle electricRingMarker = new PPCircle(Particle.REDSTONE, strikeLocation, SHOCK_RADIUS)
			.ringMode(true)
			.count(5 * electricRingMarkerCount)
			.delta(0, 0.25, 0)
			.data(DUST_YELLOW_LARGE);

		BukkitRunnable lightningRunnable = new BukkitRunnable() {
			int mCountdownTicks = SHOCK_DELAY_TICKS;

			@Nullable BukkitRunnable mInternalParticleRunnable;

			@Override
			public void run() {
				// P: Dark storm clouds gather
				stormClouds.spawnAsBoss();

				// P: Danger, yellow electric ring marker
				if (mCountdownTicks != SHOCK_DELAY_TICKS) {
					electricRingMarker.mCount = electricRingMarkerCount;
				}
				electricRingMarker.spawnAsBoss();

				// Count of the tick this run, last being 1
				if (mCountdownTicks == PPLightning.ANIMATION_TICKS) {
					// P: Lightning starts
					PPLightning lightning = new PPLightning(Particle.END_ROD, strikeLocation)
						.count(8);
					lightning.init(SHOCK_VERTICAL_RANGE, 2.5, 0.3, 0.15);
					lightning.spawnAsBoss();
					mInternalParticleRunnable = lightning.runnable();
					if (mInternalParticleRunnable != null) {
						mActiveRunnables.add(mInternalParticleRunnable);
					}

					// S: Electricity courses
					// /playsound entity.firework_rocket.twinkle master @p ~ ~ ~ 1 1.25
					world.playSound(
						strikeLocation,
						Sound.ENTITY_FIREWORK_ROCKET_TWINKLE,
						SoundCategory.PLAYERS,
						1,
						1.25f
					);
					// /playsound entity.firework_rocket.twinkle_far master @p ~ ~ ~ 1 1.5
					world.playSound(
						strikeLocation,
						Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR,
						SoundCategory.PLAYERS,
						1,
						1.5f
					);
				}

				// Count of the tick next run.
				// If next run would be tick #0
				if (--mCountdownTicks < 1) {
					cancel();
					if (mInternalParticleRunnable != null) {
						mActiveRunnables.remove(mInternalParticleRunnable);
					}
					mActiveRunnables.remove(this);

					Collection<Player> shockPlayers = PlayerUtils.playersInCylinder(
						strikeLocation,
						SHOCK_RADIUS,
						2 * SHOCK_VERTICAL_RANGE
					);
					shockPlayers.forEach((Player player) -> strikeShock(strikeLocation, player));

					startFire(strikeLocation);

					// P: Lightning hits & sparks
					// /particle firework ~ ~ ~ 0.9 1.8 0.9 0.3 0
					PartialParticle sparks = new PartialParticle(
						Particle.FIREWORKS_SPARK,
						strikeLocation,
						20,
						0.9,
						1.8,
						0.9,
						0.25,
						null,
						true,
						0.05
					);
					sparks.deltaVariance(true, false, true);
					sparks.mVaryPositiveY = true;
					sparks.spawnAsBoss();
					// /particle lava ~ ~ ~ 0 0 0 0 10
					new PartialParticle(
						Particle.LAVA,
						strikeLocation,
						20,
						0,
						0
					).spawnAsBoss();
					// /particle dust 1 1 0.5 0.75 ~ ~ ~ 1.5 1.5 1.5 0 100
					new PartialParticle(
						Particle.REDSTONE,
						strikeLocation,
						100,
						1.5,
						1.5,
						1.5,
						DUST_LIGHT_YELLOW_SMALL
					).spawnAsBoss();

					// S: Booms & fire ignites
					// /playsound entity.lightning_bolt.impact master @p ~ ~ ~ 0.75 0.5
					world.playSound(
						strikeLocation,
						Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
						SoundCategory.HOSTILE,
						0.75f,
						0.5f
					);
					// /playsound entity.lightning_bolt.impact master @p ~ ~ ~ 0.75 0.75
					world.playSound(
						strikeLocation,
						Sound.ENTITY_LIGHTNING_BOLT_IMPACT,
						SoundCategory.HOSTILE,
						0.75f,
						0.75f
					);
					// /playsound entity.blaze.shoot master @p ~ ~ ~ 1 0.75
					world.playSound(
						strikeLocation,
						Sound.ENTITY_BLAZE_SHOOT,
						SoundCategory.HOSTILE,
						1,
						0.75f
					);
					// /playsound entity.blaze.shoot master @p ~ ~ ~ 1 1
					world.playSound(
						strikeLocation,
						Sound.ENTITY_BLAZE_SHOOT,
						SoundCategory.HOSTILE,
						1,
						1
					);
				}
			}
		};
		mActiveRunnables.add(lightningRunnable);
		lightningRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void strikeShock(
		Location strikeLocation,
		Player player
	) {
		// /particle dust 1 1 0.5 0.75 ~ ~ ~ 0.225 0.45 0.225 0 5
		double widerWidthDelta = PartialParticle.getWidthDelta(player) * 1.5;
		PartialParticle shockLightning = new PartialParticle(
			Particle.REDSTONE,
			LocationUtils.getHalfHeightLocation(player),
			5,
			widerWidthDelta,
			PartialParticle.getHeightDelta(player),
			widerWidthDelta,
			0,
			DUST_LIGHT_YELLOW_SMALL
		);
		// Initial location already calculated as part of making object

		BukkitRunnable shockRunnable = new BukkitRunnable() {
			boolean mInitialLocationUsed = false;
			int mShockTracker = 0;

			@Override
			public void run() {
				BossUtils.bossDamagePercent(mKaul.getBoss(), player, SHOCK_DAMAGE_MULTIPLIER, strikeLocation, "Lightning Strike");

				if (mInitialLocationUsed) {
					shockLightning.mLocation = LocationUtils.getHalfHeightLocation(player);
				}
				shockLightning.spawnAsBoss();

				// Count of the shock this run.
				// Once shocked SHOCK_COUNT times
				if (++mShockTracker >= SHOCK_COUNT) {
					cancel();
					mActiveRunnables.remove(this);
				} else {
					mInitialLocationUsed = true;
				}
			}
		};
		mActiveRunnables.add(shockRunnable);
		shockRunnable.runTaskTimer(Plugin.getInstance(), 0, SHOCK_INTERVAL_TICKS);
	}

	// Starts in the next tick
	public void startFire(Location fireLocation) {
		World world = fireLocation.getWorld();

		// /particle dust 1 1 0.25 1 ~ ~ ~ 0.75 0.25 0.75 0 5
		PPCircle fireRingMarker = new PPCircle(Particle.FLAME, fireLocation, FIRE_RADIUS)
			.ringMode(true)
			.count(5)
			.delta(0, 0.25, 0);

		// /particle flame ~ ~ ~ 0.1 1 0.1 0.1 0
		int risingFlamesCount = 5;
		PPCircle risingFlames = new PPCircle(Particle.FLAME, fireLocation, FIRE_RADIUS)
			.count(3 * risingFlamesCount)
			.delta(0.1, 1, 0.1)
			.extraRange(0.05, 0.1)
			.directionalMode(true);
		risingFlames.deltaVariance(true, false, true);
		risingFlames.mVaryPositiveY = true;

		// /particle smoke ~ ~ ~ 0.75 0 0.75 0.01 5
		// /particle large_smoke ~ ~ ~ 0.75 0 0.75 0.01 2
		int smallSmokeCount = 4;
		int largeSmokeCount = 2;
		PPCircle smoke = new PPCircle(Particle.SMOKE_NORMAL, fireLocation, FIRE_RADIUS)
			.count(5)
			.extraRange(0, 0.01);

		int fireSoundLastThreshold = 3 * Constants.TICKS_PER_SECOND;
		BukkitRunnable fireRunnable = new BukkitRunnable() {
			int mRemainingTicks = FIRE_DURATION_TICKS;

			@Override
			public void run() {
				double diminishingCountFactor = mRemainingTicks / (double) FIRE_DURATION_TICKS;

				// P: Danger, fire ring marker
				fireRingMarker.spawnAsBoss();

				// P: Rising flames
				if (mRemainingTicks != FIRE_DURATION_TICKS) {
					risingFlames.mCount = (int)Math.ceil(risingFlamesCount * diminishingCountFactor);
				}
				risingFlames.spawnAsBoss();

				// P: Large smoke transitions to small
				smoke.mCount = (int)Math.ceil(smallSmokeCount * diminishingCountFactor);
				smoke.spawnAsBoss();
				if (mRemainingTicks > FIRE_DURATION_TICKS / 2) {
					smoke.mParticle = Particle.SMOKE_LARGE;
					smoke.mCount = (int)Math.ceil(largeSmokeCount * diminishingCountFactor / 2);
					smoke.spawnAsBoss();
					smoke.mParticle = Particle.SMOKE_NORMAL;
				}

				// S: Flames burn
				if (mRemainingTicks == FIRE_DURATION_TICKS) {
					// /playsound entity.blaze.burn master @p ~ ~ ~ 1 0
					world.playSound(
						fireLocation,
						Sound.ENTITY_BLAZE_BURN,
						SoundCategory.HOSTILE,
						1,
						0
					);
					// /playsound entity.blaze.burn master @p ~ ~ ~ 1 1
					world.playSound(
						fireLocation,
						Sound.ENTITY_BLAZE_BURN,
						SoundCategory.HOSTILE,
						1,
						1
					);
					// /playsound entity.blaze.burn master @p ~ ~ ~ 1 1.5f
					world.playSound(
						fireLocation,
						Sound.ENTITY_BLAZE_BURN,
						SoundCategory.HOSTILE,
						1,
						1.5f
					);
				} else if (
					mRemainingTicks >= fireSoundLastThreshold
					&& mRemainingTicks % 3 == 0
				) {
					world.playSound(
						fireLocation,
						Sound.ENTITY_BLAZE_BURN,
						SoundCategory.HOSTILE,
						1,
						(float)FastUtils.randomDoubleInRange(0, 1.5)
					);
				}

				if (
					mRemainingTicks <= FIRE_DURATION_TICKS - FIRE_DELAY_TICKS
					&& mRemainingTicks % FIRE_INTERVAL_TICKS == 0
				) {
					Collection<Player> burnPlayers = PlayerUtils.playersInSphere(
						fireLocation,
						FIRE_RADIUS
					);
					burnPlayers.forEach((Player player) -> {
						player.setFireTicks(FIRE_ALIGHT_TICKS);
						BossUtils.bossDamagePercent(mKaul.getBoss(), player, FIRE_DAMAGE_MULTIPLIER, fireLocation, "Lightning Strike");
					});
				}

				// Count of the tick next run.
				// If next run would be tick #0
				if (--mRemainingTicks < 1) {
					cancel();
					mActiveRunnables.remove(this);
				}
			}

		};
		mActiveRunnables.add(fireRunnable);
		fireRunnable.runTaskTimer(Plugin.getInstance(), 1, 1);
	}
}
