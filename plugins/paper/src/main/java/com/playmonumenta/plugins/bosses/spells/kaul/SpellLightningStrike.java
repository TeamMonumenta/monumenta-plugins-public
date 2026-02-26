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
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Particle.DustOptions;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

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
	private static final int SHOCK_DELAY_TICKS = (int) (1.75 * Constants.TICKS_PER_SECOND);
	private static final int SHOCK_COUNT = 10;
	private static final int ELECTRIC_RING_MARKER_COUNT = 8;

	// Lingering fire
	private static final double FIRE_DAMAGE_MULTIPLIER = 0.1;
	private static final int FIRE_RADIUS = SHOCK_RADIUS;
	private static final int FIRE_DURATION_TICKS = (int) (5.25 * Constants.TICKS_PER_SECOND);
	private static final int FIRE_INTERVAL_TICKS = (int) (0.5 * Constants.TICKS_PER_SECOND);
	private static final int FIRE_DELAY_TICKS = (int) (0.25 * Constants.TICKS_PER_SECOND);
	private static final int FIRE_ALIGHT_TICKS = 3 * Constants.TICKS_PER_SECOND;
	private static final int RISING_FLAMES_COUNT = 5;
	private static final int FIRE_SOUND_LAST_THRESHOLD = 3 * Constants.TICKS_PER_SECOND;

	private final LivingEntity mBoss;
	private final Location mCenter;
	private final int mCooldownTicks;

	private int mRemainingCooldownTicks;

	public SpellLightningStrike(LivingEntity boss, int cooldownSeconds, boolean startCooledDown, Location center) {
		mBoss = boss;
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

		if (mRemainingCooldownTicks <= 0) {
			mRemainingCooldownTicks = mCooldownTicks;
			Collection<Player> arenaParticipants = Kaul.getArenaParticipants(mCenter);
			Collection<Player> targetPlayers;

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

			targetPlayers.forEach(this::startStrike);
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
		new PPPillar(Particle.REDSTONE, strikeLocation, SHOCK_VERTICAL_RANGE).count(2 * SHOCK_VERTICAL_RANGE)
			.data(DUST_YELLOW_SMALL).spawnAsBoss();
		new PPPillar(Particle.FIREWORKS_SPARK, strikeLocation.clone().subtract(0, SHOCK_VERTICAL_RANGE, 0), SHOCK_VERTICAL_RANGE)
			.count(2 * SHOCK_VERTICAL_RANGE).spawnAsBoss();

		// S: Thunder & distant sparks
		world.playSound(strikeLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 1, 1.25f);
		world.playSound(strikeLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 0.75f, 1.5f);
		world.playSound(strikeLocation, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.HOSTILE, 1, 1.75f);
		world.playSound(strikeLocation, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.HOSTILE, 0.75f, 1.75f);

		PartialParticle stormClouds = new PartialParticle(Particle.REDSTONE, strikeLocation.clone().add(0, SHOCK_VERTICAL_RANGE, 0),
			5, 1.5, 0.25, 1.5, 0, DUST_GRAY_LARGE);

		PPCircle electricRingMarker = new PPCircle(Particle.REDSTONE, strikeLocation, SHOCK_RADIUS)
			.count(5 * ELECTRIC_RING_MARKER_COUNT).delta(0, 0.25, 0)
			.data(DUST_YELLOW_LARGE);
		electricRingMarker.spawnAsBoss();
		electricRingMarker.mCount = ELECTRIC_RING_MARKER_COUNT;

		BukkitRunnable lightningRunnable = new BukkitRunnable() {
			int mCountdownTicks = SHOCK_DELAY_TICKS;

			@Nullable BukkitRunnable mInternalParticleRunnable;

			@Override
			public void run() {
				// P: Dark storm clouds gather
				stormClouds.spawnAsBoss();

				// P: Danger, yellow electric ring marker
				electricRingMarker.spawnAsBoss();

				if (mCountdownTicks == Constants.TICKS_PER_SECOND) {
					// P: Lightning starts
					PPLightning lightning = new PPLightning(Particle.END_ROD, strikeLocation).count(8)
						.init(SHOCK_VERTICAL_RANGE, 2.5, 0.3, 0.15)
						.spawnAsBoss();

					mInternalParticleRunnable = lightning.runnable();
					if (mInternalParticleRunnable != null) {
						mActiveRunnables.add(mInternalParticleRunnable);
					}

					// S: Electricity courses
					world.playSound(strikeLocation, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE, SoundCategory.PLAYERS, 1, 1.25f);
					world.playSound(strikeLocation, Sound.ENTITY_FIREWORK_ROCKET_TWINKLE_FAR, SoundCategory.PLAYERS, 1, 1.5f);
				}

				mCountdownTicks--;
				if (mCountdownTicks <= 0) {
					this.cancel();

					mActiveRunnables.remove(this);

					Collection<Player> shockPlayers = PlayerUtils.playersInCylinder(strikeLocation, SHOCK_RADIUS, 2 * SHOCK_VERTICAL_RANGE);
					shockPlayers.forEach((Player player) -> strikeShock(strikeLocation, player));
					startFire(strikeLocation);

					// P: Lightning hits & sparks
					PartialParticle sparks = new PartialParticle(Particle.FIREWORKS_SPARK, strikeLocation, 20,
						0.9, 1.8, 0.9, 0.25, null, true, 0.05).deltaVariance(true, false, true);
					sparks.mVaryPositiveY = true;
					sparks.spawnAsBoss();
					new PartialParticle(Particle.LAVA, strikeLocation, 20, 0, 0).spawnAsBoss();
					new PartialParticle(Particle.REDSTONE, strikeLocation, 100, 1.5, 1.5, 1.5, DUST_LIGHT_YELLOW_SMALL).spawnAsBoss();

					// S: Booms & fire ignites
					world.playSound(strikeLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 0.75f, 0.5f);
					world.playSound(strikeLocation, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.HOSTILE, 0.75f, 0.75f);
					world.playSound(strikeLocation, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 0.75f);
					world.playSound(strikeLocation, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 1, 1);
				}
			}
		};
		mActiveRunnables.add(lightningRunnable);
		lightningRunnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void strikeShock(Location strikeLocation, Player player) {
		double widerWidthDelta = PartialParticle.getWidthDelta(player) * 1.5;
		PartialParticle shockLightning = new PartialParticle(Particle.REDSTONE, strikeLocation, 5,
			widerWidthDelta, PartialParticle.getHeightDelta(player), widerWidthDelta, 0, DUST_LIGHT_YELLOW_SMALL);
		// Initial location already calculated as part of making object

		BukkitRunnable shockRunnable = new BukkitRunnable() {
			int mShocks = 0;

			@Override
			public void run() {
				// Stop damaging players who just teleported into Judgement
				if (player.getScoreboardTags().contains(SpellKaulsJudgement.KAULS_JUDGEMENT_TAG)) {
					this.cancel();
					return;
				}

				BossUtils.bossDamagePercent(mBoss, player, SHOCK_DAMAGE_MULTIPLIER, strikeLocation, "Lightning Strike");

				shockLightning.location(LocationUtils.getHalfHeightLocation(player)).spawnAsBoss();

				mShocks++;
				if (mShocks >= SHOCK_COUNT) {
					this.cancel();
				}
			}
		};
		mActiveRunnables.add(shockRunnable);
		shockRunnable.runTaskTimer(Plugin.getInstance(), 0, SHOCK_INTERVAL_TICKS);
	}

	// Starts in the next tick
	public void startFire(Location fireLocation) {
		World world = fireLocation.getWorld();

		PPCircle fireRingMarker = new PPCircle(Particle.FLAME, fireLocation, FIRE_RADIUS).count(5).delta(0, 0.25, 0);

		PPCircle risingFlames = new PPCircle(Particle.FLAME, fireLocation, FIRE_RADIUS)
			.ringMode(false)
			.count(3 * RISING_FLAMES_COUNT)
			.delta(0, 1, 0)
			.extraRange(0.05, 0.1)
			.directionalMode(true);
		risingFlames.spawnAsBoss();

		PPCircle smoke = new PPCircle(Particle.SMOKE_NORMAL, fireLocation, FIRE_RADIUS)
			.ringMode(false)
			.extraRange(0, 0.01);

		PPCircle largeSmoke = new PPCircle(Particle.SMOKE_NORMAL, fireLocation, FIRE_RADIUS)
			.ringMode(false)
			.extraRange(0, 0.01);

		world.playSound(fireLocation, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 1, 0);
		world.playSound(fireLocation, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 1, 1);
		world.playSound(fireLocation, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 1, 1.5f);

		BukkitRunnable fireRunnable = new BukkitRunnable() {
			int mRemainingTicks = FIRE_DURATION_TICKS;

			@Override
			public void run() {
				double diminishingCountFactor = (double) mRemainingTicks / FIRE_DURATION_TICKS;

				// P: Danger, fire ring marker
				fireRingMarker.spawnAsBoss();

				risingFlames.count((int) Math.ceil(RISING_FLAMES_COUNT * diminishingCountFactor)).spawnAsBoss();

				// P: Large smoke transitions to small
				smoke.count((int) Math.ceil(4 * diminishingCountFactor)).spawnAsBoss();
				if (mRemainingTicks > FIRE_DURATION_TICKS / 2) {
					largeSmoke.count((int) Math.ceil(2 * diminishingCountFactor / 2)).spawnAsBoss();
				}

				// S: Flames burn
				if (mRemainingTicks >= FIRE_SOUND_LAST_THRESHOLD && mRemainingTicks % 3 == 0) {
					world.playSound(fireLocation, Sound.ENTITY_BLAZE_BURN, SoundCategory.HOSTILE, 1, (float) FastUtils.randomDoubleInRange(0, 1.5));
				}

				if (mRemainingTicks <= FIRE_DURATION_TICKS - FIRE_DELAY_TICKS && mRemainingTicks % FIRE_INTERVAL_TICKS == 0) {
					Collection<Player> burnPlayers = PlayerUtils.playersInSphere(fireLocation, FIRE_RADIUS);
					burnPlayers.forEach((Player player) -> {
						LivingEntity boss = mBoss;
						EntityUtils.applyFire(Plugin.getInstance(), FIRE_ALIGHT_TICKS, player, boss);
						BossUtils.bossDamagePercent(boss, player, FIRE_DAMAGE_MULTIPLIER, fireLocation, "Lightning Strike");
					});
				}

				// Count of the tick next run, if next run would be tick #0
				mRemainingTicks--;
				if (mRemainingTicks <= 0) {
					this.cancel();
					mActiveRunnables.remove(this);
				}
			}

		};
		mActiveRunnables.add(fireRunnable);
		fireRunnable.runTaskTimer(Plugin.getInstance(), 1, 1);
	}
}
