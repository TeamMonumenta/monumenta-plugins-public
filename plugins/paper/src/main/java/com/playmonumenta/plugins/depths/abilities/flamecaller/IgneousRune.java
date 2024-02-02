package com.playmonumenta.plugins.depths.abilities.flamecaller;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.potion.PotionManager;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Color;
import org.bukkit.FluidCollisionMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class IgneousRune extends DepthsAbility {

	public static final String ABILITY_NAME = "Igneous Rune";
	private static final int COOLDOWN = 16 * 20;
	private static final double CAST_RANGE = 30;
	private static final int ARMING_TIME = 40;
	private static final int MAX_RUNE_TIME = 30 * 20;
	private static final double RUNE_RADIUS = 3;
	private static final double DAMAGE_RADIUS = 6;
	private static final double[] DAMAGE = {20, 24, 28, 32, 36, 44};
	private static final int FIRE_DURATION = 6 * 20;
	private static final double RUNE_STRENGTH = 0.1;
	private static final double RUNE_SPEED = 0.1;
	private static final int BUFF_DURATION = 6 * 20;

	public static final String CHARM_COOLDOWN = "Igneous Rune Cooldown";

	public static final DepthsAbilityInfo<IgneousRune> INFO =
		new DepthsAbilityInfo<>(IgneousRune.class, ABILITY_NAME, IgneousRune::new, DepthsTree.FLAMECALLER, DepthsTrigger.SHIFT_LEFT_CLICK)
			.linkedSpell(ClassAbility.IGNEOUS_RUNE)
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", IgneousRune::cast, DepthsTrigger.SHIFT_LEFT_CLICK))
			.displayItem(Material.BLAZE_POWDER)
			.descriptions(IgneousRune::getDescription);

	private final double mCastRange;
	private final int mArmingTime;
	private final double mRuneRadius;
	private final double mDamageRadius;
	private final double mDamage;
	private final int mFireDuration;
	private final double mRuneStrength;
	private final double mRuneSpeed;
	private final int mBuffDuration;

	private static final Color YELLOW_COLOR = Color.fromRGB(250, 180, 0);
	private static final Color ORANGE_COLOR = Color.fromRGB(240, 140, 0);
	private static final Color RED_COLOR = Color.fromRGB(200, 90, 0);
	private static final Color DARK_RED_COLOR = Color.fromRGB(180, 0, 0);
	private boolean mSparkAnimationCompleted;
	private @Nullable BukkitRunnable mRuneRunnable;

	public IgneousRune(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mCastRange = CAST_RANGE;
		mArmingTime = CharmManager.getDuration(player, CharmEffects.IGNEOUS_RUNE_ARMING_TIME.mEffectName, ARMING_TIME);
		mRuneRadius = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.IGNEOUS_RUNE_RADIUS.mEffectName, RUNE_RADIUS);
		mDamageRadius = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.IGNEOUS_RUNE_RADIUS.mEffectName, DAMAGE_RADIUS);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CharmEffects.IGNEOUS_RUNE_DAMAGE.mEffectName, DAMAGE[mRarity - 1]);
		mFireDuration = CharmManager.getDuration(player, CharmEffects.IGNEOUS_RUNE_FIRE_DURATION.mEffectName, FIRE_DURATION);
		mRuneStrength = RUNE_STRENGTH + CharmManager.getLevelPercentDecimal(player, CharmEffects.IGNEOUS_RUNE_BUFF_AMPLIFIER.mEffectName);
		mRuneSpeed = RUNE_SPEED + CharmManager.getLevelPercentDecimal(player, CharmEffects.IGNEOUS_RUNE_BUFF_AMPLIFIER.mEffectName);
		mBuffDuration = CharmManager.getDuration(player, CharmEffects.IGNEOUS_RUNE_BUFF_DURATION.mEffectName, BUFF_DURATION);

		mSparkAnimationCompleted = false;
		mRuneRunnable = null;
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getEyeLocation();
		Vector direction = loc.getDirection();

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		RayTraceResult result = world.rayTrace(loc, direction, mCastRange, FluidCollisionMode.SOURCE_ONLY, true, 0, e -> false);

		Location endLocation;
		if (result == null) {
			endLocation = loc.clone().add(direction.clone().multiply(mCastRange));
		} else {
			endLocation = result.getHitPosition().toLocation(world);
		}
		endLocation.setDirection(direction);

		Location startLocation = loc.clone().add(direction.clone().multiply(2));
		mSparkAnimationCompleted = false;
		launchSpark(startLocation, endLocation);
		placeRune(endLocation, playerItemStats);

		return true;
	}

	public void launchSpark(Location start, Location end) {
		World world = start.getWorld();
		world.playSound(start, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 1f, 1.6f);
		world.playSound(start, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.PLAYERS, 0.5f, 2.0f);
		new BukkitRunnable() {
			final Location mL = start;
			final Location mDestination = end;
			int mT = 0;
			int mIter = 0;
			double mAccelStrength = 0.02;
			boolean mHasBumpedUp = false;
			final Vector mVelocity = VectorUtils.rotateTargetDirection(start.getDirection(), 0, -60);

			double mYawPush = 0;
			double mPitchPush = 0;

			@Override
			public void run() {
				mT++;

				for (int i = 0; i < 10; i++) {
					mIter++;

					mVelocity.add(LocationUtils.getDirectionTo(mDestination, mL).multiply(mAccelStrength));

					// every once in a while, add some random force to the path
					if (mIter % 20 == 0) {
						mYawPush = (FastUtils.RANDOM.nextBoolean() ? 1 : -1) * FastUtils.randomDoubleInRange(30, 80);
						mPitchPush = FastUtils.randomDoubleInRange(-30, 10);
					}
					if (mIter % 20 < 12 && !mHasBumpedUp) {
						mVelocity.add(VectorUtils.rotateTargetDirection(LocationUtils.getDirectionTo(mDestination, mL),
							mYawPush, mPitchPush).multiply(0.22));
					}

					if (mVelocity.length() > 0.27) {
						mVelocity.normalize().multiply(0.27);
					}

					mL.add(mVelocity);

					new PartialParticle(Particle.REDSTONE, mL, 1, 0.05, 0.05, 0.05, 0,
						new Particle.DustOptions(ORANGE_COLOR, 1.5f))
						.spawnAsPlayerActive(mPlayer);
					new PartialParticle(Particle.REDSTONE, mL, 1, 0.05, 0.05, 0.05, 0,
						new Particle.DustOptions(RED_COLOR, 1.5f))
						.spawnAsPlayerActive(mPlayer);
					if (mIter % 2 == 0) {
						new PartialParticle(Particle.SMALL_FLAME, mL, 1, 0, 0, 0, 0.05).spawnAsPlayerActive(mPlayer);
					}

					// stop random motion and increase acceleration strength once we get close enough
					if (mL.distance(mDestination) < 5 && !mHasBumpedUp) {
						mHasBumpedUp = true;
						mVelocity.add(new Vector(0, 3.5, 0));
						mAccelStrength = 0.07;
					}
					if (mHasBumpedUp) {
						mAccelStrength += 0.01;
					}

					if (mL.distance(mDestination) < 0.5) {
						ParticleUtils.drawParticleCircleExplosion(mPlayer, mDestination.clone().add(0, 0.05, 0), 0, 0.5, -mDestination.getYaw(), -mDestination.getPitch(), 30,
							0.15f, true, 0, 0, Particle.FLAME);
						mSparkAnimationCompleted = true;
						this.cancel();
						return;
					}
				}

				if (mT >= mArmingTime) {
					this.cancel();
				}
			}

		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public void placeRune(Location loc, ItemStatManager.PlayerItemStats playerItemStats) {
		// if we already have a rune out, explode it to place the new one
		if (mRuneRunnable != null) {
			mRuneRunnable.cancel();
		}
		mRuneRunnable = new BukkitRunnable() {
			final Location mLoc = loc;
			final World mWorld = loc.getWorld();
			int mTicks = 0;
			@Override
			public void run() {
				// cosmetics
				if (mTicks < mArmingTime) { // arming in progress
					if (mSparkAnimationCompleted) {
						new PPCircle(Particle.SMALL_FLAME, mLoc.clone().add(0, 0.1, 0), mRuneRadius)
							.countPerMeter(2)
							.arcDegree(mLoc.getYaw() + 90, 360.0 * mTicks / mArmingTime + mLoc.getYaw() + 90)
							.includeStart(true)
							.includeEnd(true)
							.spawnAsPlayerActive(mPlayer);

						float pitch = (float) (1.5f + 0.5 * (mTicks + 1) / mArmingTime);
						mWorld.playSound(loc, Sound.BLOCK_DISPENSER_DISPENSE, SoundCategory.PLAYERS, 0.2f, pitch);
					}
				} else if (mTicks == mArmingTime) { // arming complete
					new PPCircle(Particle.FLAME, mLoc.clone().add(0, 0.15, 0), mRuneRadius + 0.05)
						.countPerMeter(1.75)
						.randomizeAngle(true)
						.spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.FLAME, mLoc.clone().add(0, 0.2, 0), mRuneRadius + 0.1)
						.countPerMeter(1.5)
						.randomizeAngle(true)
						.spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.FLAME, mLoc, mRuneRadius)
						.count(60)
						.delta(0, 1, 0).directionalMode(true).extraRange(0.05, 0.15)
						.spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.SMALL_FLAME, mLoc, mRuneRadius)
						.count(40)
						.delta(0, 1, 0).directionalMode(true).extraRange(0.08, 0.18)
						.spawnAsPlayerActive(mPlayer);

					mWorld.playSound(loc, Sound.BLOCK_IRON_DOOR_OPEN, SoundCategory.PLAYERS, 0.8f, 1.5f);
					mWorld.playSound(loc, Sound.BLOCK_CHEST_LOCKED, SoundCategory.PLAYERS, 0.8f, 2.0f);
					mWorld.playSound(loc, Sound.ENTITY_TNT_PRIMED, SoundCategory.PLAYERS, 1.0f, 2.0f);
					mWorld.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 0.8f, 1f);
					mWorld.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 0.8f, 1.1f);
				} else if (mTicks % 10 == 0) { // armed and ready
					new PPCircle(Particle.SMALL_FLAME, mLoc, mRuneRadius + 0.1)
						.countPerMeter(3)
						.innerRadiusFactor(0.93)
						.randomizeAngle(false)
						.spawnAsPlayerActive(mPlayer);
					new PPCircle(Particle.REDSTONE, mLoc.clone().add(0, 0.05, 0), mRuneRadius)
						.data(new Particle.DustOptions(ParticleUtils.getTransition(YELLOW_COLOR, DARK_RED_COLOR, (double) (mTicks - mArmingTime) / MAX_RUNE_TIME), 1f))
						.countPerMeter(2.5)
						.randomizeAngle(true)
						.spawnAsPlayerActive(mPlayer);
					mWorld.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, SoundCategory.PLAYERS, 0.5f, 1.5f);

					if (mTicks % 20 == 0) {
						new PPCircle(Particle.FLAME, mLoc.clone().add(0, 0.1, 0), mRuneRadius)
							.countPerMeter(1)
							.randomizeAngle(true)
							.spawnAsPlayerActive(mPlayer);
						new PPCircle(Particle.SMALL_FLAME, mLoc, mRuneRadius)
							.count(20)
							.delta(0, 1, 0).directionalMode(true).extraRange(0.05, 0.11)
							.spawnAsPlayerActive(mPlayer);
					}
				}

				// actual effect
				Hitbox hitbox = new Hitbox.UprightCylinderHitbox(loc.clone().subtract(0, mRuneRadius, 0), mRuneRadius * 2, mRuneRadius);
				for (Player player : hitbox.getHitPlayers(true)) {
					if (player == mPlayer) {
						mPlugin.mPotionManager.addPotion(player, PotionManager.PotionID.ABILITY_SELF, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 5 * 20, 0, true, true));
					} else {
						mPlugin.mPotionManager.addPotion(player, PotionManager.PotionID.ABILITY_OTHER, new PotionEffect(PotionEffectType.FIRE_RESISTANCE, 5 * 20, 0, true, true));
					}
				}
				if (mTicks > mArmingTime && !hitbox.getHitMobs().isEmpty()) {
					this.cancel();
				}

				// just erupt if we wait too long
				if (mTicks > MAX_RUNE_TIME) {
					this.cancel();
				}
				mTicks++;
			}

			@Override
			public synchronized void cancel() {
				doEruption(mLoc, playerItemStats);
				mRuneRunnable = null;
				super.cancel();
			}
		};

		mRuneRunnable.runTaskTimer(mPlugin, 0, 1);
	}

	public void doEruption(Location loc, ItemStatManager.PlayerItemStats playerItemStats) {
		World world = loc.getWorld();
		world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 1.5f, 1.4f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.5f, 2.0f);
		world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.PLAYERS, 1.5f, 1.5f);
		world.playSound(loc, Sound.ENTITY_CREEPER_DEATH, SoundCategory.PLAYERS, 1.5f, 0.65f);
		world.playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, SoundCategory.PLAYERS, 0.15f, 2.0f);

		new PartialParticle(Particle.FLAME, loc, 75).delta(0.5).extraRange(0.35, 0.6).spawnAsPlayerActive(mPlayer);
		new PartialParticle(Particle.LAVA, loc, 100).delta(mDamageRadius / 2).spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.FLAME, loc, mRuneRadius + 0.1)
			.count(50).randomizeAngle(true)
			.spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.FLAME, loc, mRuneRadius)
			.count(50).randomizeAngle(true)
			.delta(0, 1, 0).directionalMode(true).extraRange(0.0, 0.1)
			.spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.FLAME, loc, mRuneRadius)
			.count(120).randomizeAngle(true)
			.delta(0, 1, 0).directionalMode(true).extraRange(0.1, 0.4)
			.spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.FLAME, loc, mRuneRadius)
			.count(240).randomizeAngle(true)
			.delta(0, 1, 0).directionalMode(true).extraRange(0.3, 0.65)
			.spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.SMALL_FLAME, loc, mRuneRadius)
			.count(150).randomizeAngle(true)
			.delta(0, 1, 0).directionalMode(true).extraRange(0.3, 0.9)
			.spawnAsPlayerActive(mPlayer);
		new PPCircle(Particle.SMALL_FLAME, loc, mRuneRadius)
			.count(60).randomizeAngle(true)
			.delta(0, 1, 0).directionalMode(true).extraRange(0.85, 1.45)
			.spawnAsPlayerActive(mPlayer);

		// flame spirals
		new BukkitRunnable() {
			int mTicks = 0;
			int mIter = 0;
			double mDegree = 0;
			@Override
			public void run() {
				for (int i = 0; i < 8; i++) {
					for (int spiral = 0; spiral < 3; spiral++) {
						double degree = mDegree + spiral * 120;
						Location l = loc.clone().add(FastUtils.cosDeg(degree) * mDamageRadius * 0.8, Math.pow(mIter, 2) / 400, FastUtils.sinDeg(degree) * mDamageRadius * 0.8);
						Vector v = new Vector(-FastUtils.sinDeg(degree), 0.5, FastUtils.cosDeg(degree));
						new PartialParticle(Particle.FLAME, l, 1, v.getX(), v.getY(), v.getZ(), 0.08, null, true, 0.02)
							.spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.SMALL_FLAME, l, 1, v.getX(), v.getY() + 0.3, v.getZ(), 0.12, null, true, 0.04)
							.spawnAsPlayerActive(mPlayer);
					}

					mDegree += 5;
					mIter++;
				}

				mTicks++;
				if (mTicks > 6) {
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);

		// actual effect
		for (LivingEntity mob : new Hitbox.UprightCylinderHitbox(loc.clone().subtract(0, mDamageRadius, 0), mDamageRadius * 2, mDamageRadius).getHitMobs()) {
			DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mDamage, true, false, false);
			EntityUtils.applyFire(mPlugin, mFireDuration, mob, mPlayer, playerItemStats);
			MovementUtils.knockAway(loc, mob, 0.3f, 0.7f, true);
		}

		mPlugin.mEffectManager.addEffect(mPlayer, "IgneousRuneStrength", new PercentDamageDealt(mBuffDuration, mRuneStrength));
		mPlugin.mEffectManager.addEffect(mPlayer, "IgneousRuneSpeed", new PercentSpeed(mBuffDuration, mRuneSpeed, "IgneousRuneSpeedAttr"));
	}

	private static Description<IgneousRune> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<IgneousRune>(color)
			.add("Left click while sneaking to place a rune up to ")
			.add(a -> a.mCastRange, CAST_RANGE)
			.add(" blocks away. The rune has a radius of ")
			.add(a -> a.mRuneRadius, RUNE_RADIUS)
			.add(" blocks, arms after ")
			.addDuration(a -> a.mArmingTime, ARMING_TIME, true)
			.add(" seconds, and grants fire immunity to all players inside. A maximum of 1 rune can be active at a time. When a mob enters the rune once it arms, it erupts and deals ")
			.addDepthsDamage(a -> a.mDamage, DAMAGE[rarity - 1], true)
			.add(" magic damage to all mobs in a ")
			.add(a -> a.mDamageRadius, DAMAGE_RADIUS)
			.add(" block radius and ignites them for ")
			.addDuration(a -> a.mFireDuration, FIRE_DURATION)
			.add(" seconds. After a rune is triggered, you gain ")
			.addPercent(a -> a.mRuneStrength, RUNE_STRENGTH)
			.add(" strength and ")
			.addPercent(a -> a.mRuneSpeed, RUNE_SPEED)
			.add(" speed for ")
			.addDuration(a -> a.mBuffDuration, BUFF_DURATION)
			.add(" seconds. ")
			.addCooldown(COOLDOWN);
	}
}
