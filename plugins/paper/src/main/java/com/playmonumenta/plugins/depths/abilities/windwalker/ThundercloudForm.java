package com.playmonumenta.plugins.depths.abilities.windwalker;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.depths.DepthsTree;
import com.playmonumenta.plugins.depths.abilities.DepthsAbility;
import com.playmonumenta.plugins.depths.abilities.DepthsAbilityInfo;
import com.playmonumenta.plugins.depths.abilities.DepthsTrigger;
import com.playmonumenta.plugins.depths.charmfactory.CharmEffects;
import com.playmonumenta.plugins.effects.DamageImmunity;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPLightning;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.ParticleUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Particle.DustTransition;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class ThundercloudForm extends DepthsAbility implements AbilityWithDuration {

	public static final String ABILITY_NAME = "Thundercloud Form";
	public static final int COOLDOWN = 30 * 20;
	public static final int LAUNCH_RADIUS = 4;
	public static final double[] LAUNCH_DAMAGE = {8, 11, 14, 17, 20, 26};
	public static final float LAUNCH_KNOCKBACK = 0.8f;
	public static final int FLIGHT_DURATION = 4 * 20;
	public static final double FLIGHT_SPEED = 0.075;
	public static final double[] LIGHTNING_DAMAGE = {60, 70, 80, 90, 100, 120};
	public static final double[] AOE_DAMAGE = {20, 25, 30, 35, 40, 50};
	public static final double AOE_RADIUS = 4;
	private static final String FALL_IMMUNITY_EFFECT = "ThundercloudFormFallImmunity";

	public static final String CHARM_COOLDOWN = "Thundercloud Form Cooldown";

	public static final DepthsAbilityInfo<ThundercloudForm> INFO =
		new DepthsAbilityInfo<>(ThundercloudForm.class, ABILITY_NAME, ThundercloudForm::new, DepthsTree.WINDWALKER, DepthsTrigger.SWAP)
			.linkedSpell(ClassAbility.THUNDERCLOUD_FORM)
			.cooldown(CHARM_COOLDOWN, COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", ThundercloudForm::cast, DepthsTrigger.SWAP))
			.displayItem(Material.WHITE_GLAZED_TERRACOTTA)
			.descriptions(ThundercloudForm::getDescription);

	private final double mLaunchRadius;
	private final double mLaunchDamage;
	private final int mFlightDuration;
	private final double mFlightSpeed;
	private final double mLightningDamage;
	private final double mAOEDamage;
	private final double mAOERadius;

	private @Nullable BukkitRunnable mRunnable = null;
	private boolean mHasThrownLightning = false;
	private int mCurrDuration = -1;

	public ThundercloudForm(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mLaunchRadius = CharmManager.getRadius(mPlayer, CharmEffects.THUNDERCLOUD_FORM_RADIUS.mEffectName, LAUNCH_RADIUS);
		mLaunchDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.THUNDERCLOUD_FORM_DAMAGE.mEffectName, LAUNCH_DAMAGE[mRarity - 1]);
		mFlightDuration = CharmManager.getDuration(mPlayer, CharmEffects.THUNDERCLOUD_FORM_FLIGHT_DURATION.mEffectName, FLIGHT_DURATION);
		mFlightSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.THUNDERCLOUD_FORM_FLIGHT_SPEED.mEffectName, FLIGHT_SPEED);
		mLightningDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.THUNDERCLOUD_FORM_DAMAGE.mEffectName, LIGHTNING_DAMAGE[mRarity - 1]);
		mAOEDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CharmEffects.THUNDERCLOUD_FORM_DAMAGE.mEffectName, AOE_DAMAGE[mRarity - 1]);
		mAOERadius = CharmManager.getRadius(mPlayer, CharmEffects.THUNDERCLOUD_FORM_RADIUS.mEffectName, AOE_RADIUS);
	}

	public boolean cast() {
		if (isOnCooldown()) {
			if (mRunnable != null && !mHasThrownLightning) {

				throwLightning();

				return true;
			}
			return false;
		}
		if (mRunnable != null) {
			mRunnable.cancel();
		}

		putOnCooldown();

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();

		world.playSound(loc, Sound.ENTITY_BAT_TAKEOFF, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, SoundCategory.PLAYERS, 1f, 1f);
		world.playSound(loc, Sound.ENTITY_ZOMBIE_INFECT, SoundCategory.PLAYERS, 1f, 2f);
		ParticleUtils.drawParticleCircleExplosion(mPlayer, loc, 0, mLaunchRadius / 2, -loc.getYaw(), -loc.getPitch(),
			40, 0.5f, false, 0, 0, Particle.EXPLOSION_NORMAL);
		new PPLightning(Particle.DUST_COLOR_TRANSITION, loc).init(10, 10, 1.0, 0.35).hopsPerBlock(1.33).data(new DustTransition(Color.YELLOW, Color.WHITE, 1f)).count(10).delta(0.005).duration(2).spawnAsPlayerActive(mPlayer);
		for (int i = 0; i < 8; i++) {
			sparkParticle(loc, VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 1.5)));
		}
		new PartialParticle(Particle.EXPLOSION_NORMAL, loc, 40, 0, 0.5).spawnAsPlayerActive(mPlayer);
		for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mLaunchRadius)) {
			DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mLaunchDamage, mInfo.getLinkedSpell(), true, false);
			MovementUtils.knockAway(loc, mob, LAUNCH_KNOCKBACK, 0.4f);

			new PartialParticle(Particle.SPIT, LocationUtils.getEntityCenter(mob), 10).delta(0.5).spawnAsPlayerActive(mPlayer);
		}

		mPlayer.setAllowFlight(true);
		mPlayer.setFlySpeed((float) mFlightSpeed);
		mPlayer.setVelocity(mPlayer.getEyeLocation().getDirection().multiply(0.5).add(new Vector(0, 0.9, 0)).normalize().multiply(1.2));
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> mPlayer.setFlying(true), 6);

		mHasThrownLightning = false;

		mPlugin.mEffectManager.addEffect(mPlayer, FALL_IMMUNITY_EFFECT, new DamageImmunity(mFlightDuration + 4 * 20, EnumSet.of(DamageType.FALL)));

		mCurrDuration = 0;
		mRunnable = new BukkitRunnable() {
			@Override
			public void run() {
				if (!mPlayer.isOnline() || mPlayer.isDead()) {
					this.cancel();
					return;
				}

				if (!mHasThrownLightning) {
					if (mCurrDuration % 10 == 0) {
						world.playSound(mPlayer.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 0.5f, 0.9f);
					}
					new PartialParticle(Particle.ELECTRIC_SPARK, mPlayer.getEyeLocation(), 1).delta(0.5).spawnAsPlayerActive(mPlayer);
					if (FastUtils.RANDOM.nextDouble() < 0.15) {
						sparkParticle(LocationUtils.varyInCircle(mPlayer.getLocation(), 0.75), new Vector(FastUtils.randomDoubleInRange(-0.75, 0.75), -1, FastUtils.randomDoubleInRange(-0.75, 0.75)));
					}
					new PartialParticle(Particle.DUST_COLOR_TRANSITION, mPlayer.getLocation(), 5).delta(0.5, 0.2, 0.5)
						.data(new DustTransition(Color.WHITE, Color.BLACK, 1.5f)).spawnAsPlayerActive(mPlayer);
				}
				new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 1).delta(0.2, 0.1, 0.2).extraRange(0.05, 0.1).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.CLOUD, mPlayer.getLocation(), 2).delta(0.5, 0.2, 0.5).extraRange(0, 0.1).spawnAsPlayerActive(mPlayer);
				if (mFlightDuration - mCurrDuration < 30 && mCurrDuration % 5 == 0) { // last 1.5s
					float pitch = 1 + (mCurrDuration - mFlightDuration + 20) / 20f;
					mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, SoundCategory.PLAYERS, 0.7f, pitch);
				}

				if (mCurrDuration > mFlightDuration) {
					this.cancel();
				}
				mCurrDuration++;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();

				mRunnable = null;
				mCurrDuration = -1;
				mHasThrownLightning = false;
				mPlayer.setFlying(false);
				mPlayer.setAllowFlight(false);
			}
		};
		cancelOnDeath(mRunnable.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	public void throwLightning() {
		mHasThrownLightning = true;

		mPlayer.getWorld().playSound(mPlayer.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 1.0f, 2f);
		new PartialParticle(Particle.ELECTRIC_SPARK, mPlayer.getEyeLocation().add(mPlayer.getEyeLocation().getDirection()), 30).extra(1.5).spawnAsPlayerActive(mPlayer);
		new BukkitRunnable() {
			int mTicks = 0;
			final Location mLoc = mPlayer.getEyeLocation();
			final Vector mDir = mPlayer.getEyeLocation().getDirection();

			@Override
			public void run() {
				for (int i = 0; i < 4; i++) {
					new PPLine(Particle.ELECTRIC_SPARK, mLoc, mDir, 2).shiftStart(-2)
						.countPerMeter(1.5).delta(0.12).spawnAsPlayerActive(mPlayer);
					new PPLine(Particle.DUST_COLOR_TRANSITION, mLoc, mDir, 2).shiftStart(1).data(new DustTransition(Color.YELLOW, Color.WHITE, 1.1f))
						.countPerMeter(4).delta(0.1).spawnAsPlayerActive(mPlayer);

					if (i % 4 == 0) {
						sparkParticle(mLoc, VectorUtils.rotateTargetDirection(mDir.clone().multiply(-0.5), FastUtils.randomDoubleInRange(-15, 15), FastUtils.randomDoubleInRange(-15, 15)));
						sparkParticle(mLoc, VectorUtils.rotateTargetDirection(mDir.clone().multiply(-0.5), FastUtils.randomDoubleInRange(-30, 30), FastUtils.randomDoubleInRange(-30, 30)));
					}

					mLoc.add(mDir.clone().multiply(0.5));

					Hitbox hitbox = new Hitbox.SphereHitbox(mLoc, 1);
					LivingEntity primaryTarget;
					if (!hitbox.getHitMobs().isEmpty()) {
						List<LivingEntity> hitMobs = hitbox.getHitMobs();
						hitMobs.sort(Comparator.comparingDouble(a -> a.getLocation().distance(mLoc))); // get the closest mob to the center
						primaryTarget = hitMobs.get(0);

						DamageUtils.damage(mPlayer, primaryTarget, DamageType.MAGIC, mLightningDamage, ClassAbility.THUNDERCLOUD_FORM, true, true);
						MovementUtils.knockAway(mLoc.clone().subtract(0, 0.5, 0), primaryTarget, 0f, 0.7f);

						lightningStrike(primaryTarget.getLocation(), primaryTarget);

						this.cancel();
						break;
					} else if (!mLoc.isChunkLoaded() || LocationUtils.collidesWithBlocks(BoundingBox.of(mLoc.clone().add(0.5, 0.5, 0.5), mLoc.clone().add(-0.5, -0.5, -0.5)), mLoc.getWorld(), false)) {
						lightningStrike(mLoc, null);

						this.cancel();
						break;
					}
				}

				if (mTicks > 200) {
					this.cancel();
				}
				mTicks++;
			}

			private void lightningStrike(Location loc, @Nullable LivingEntity primaryTarget) {
				for (LivingEntity mob : new Hitbox.SphereHitbox(loc, mAOERadius).getHitMobs(primaryTarget)) {
					DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mAOEDamage, ClassAbility.THUNDERCLOUD_FORM, true, true);
					MovementUtils.knockAway(loc, mob, 0.7f, 0.7f);

					new PartialParticle(Particle.DUST_COLOR_TRANSITION, LocationUtils.getEntityCenter(mob), 20).delta(0.5).data(new DustTransition(Color.YELLOW, Color.YELLOW, 1.2f)).spawnAsPlayerActive(mPlayer);
				}

				World world = loc.getWorld();
				loc.add(0, 0.5, 0);
				new PPLightning(Particle.DUST_COLOR_TRANSITION, loc).init(20, 10, 1.2, 0.4).hopsPerBlock(1.33).data(new DustTransition(Color.YELLOW, Color.YELLOW, 1.2f)).count(15).delta(0.01).duration(3).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FIREWORKS_SPARK, loc, 120, 0, 0, 0, 0.4).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.END_ROD, loc, 60, 0, 0, 0, 0.4).spawnAsPlayerActive(mPlayer);
				new PartialParticle(Particle.FLASH, loc, 1).spawnAsPlayerActive(mPlayer);
				world.playSound(loc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.PLAYERS, 0.6f, 2.0f);
				world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.PLAYERS, 2.0f, 0.5f);
				world.playSound(loc, Sound.ENTITY_EVOKER_CAST_SPELL, SoundCategory.PLAYERS, 2.0f, 0.5f);
				world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_IMPACT, SoundCategory.PLAYERS, 2.0f, 0.8f);
				world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 2.0f, 1.2f);
				world.playSound(loc, Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.PLAYERS, 2.0f, 2.0f);

				Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
					for (int i = 0; i < 10; i++) {
						sparkParticle(loc, VectorUtils.randomUnitVector().multiply(FastUtils.randomDoubleInRange(1, 2)));
					}
				}, 3);
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private static Description<ThundercloudForm> getDescription(int rarity, TextColor color) {
		return new DescriptionBuilder<>(() -> INFO, color)
			.addTrigger()
			.add(" to launch upwards, dealing ")
			.addDepthsDamage(a -> a.mLaunchDamage, LAUNCH_DAMAGE[rarity - 1], true)
			.add(" magic damage to mobs within ")
			.add(a -> a.mLaunchRadius, LAUNCH_RADIUS)
			.add(" blocks and knocking them away, and enter flight for ")
			.addDuration(a -> a.mFlightDuration, FLIGHT_DURATION)
			.add(" seconds. While in flight, trigger again to throw a bolt of lightning that deals ")
			.addDepthsDamage(a -> a.mLightningDamage, LIGHTNING_DAMAGE[rarity - 1], true)
			.add(" magic damage to the first enemy it strikes and ")
			.addDepthsDamage(a -> a.mAOEDamage, AOE_DAMAGE[rarity - 1], true)
			.add(" magic damage to all other enemies in a ")
			.add(a -> a.mAOERadius, AOE_RADIUS)
			.add(" block radius.")
			.addCooldown(COOLDOWN);
	}

	private void sparkParticle(Location loc, Vector dir) {
		Location location = loc.clone();
		Vector direction = dir.clone();

		for (int i = 0; i < 4; i++) {
			Location oldLocation = location.clone();
			location.add(direction.multiply(0.9)).add(FastUtils.randomDoubleInRange(-0.2, 0.25), FastUtils.randomDoubleInRange(-0.25, 0.25), FastUtils.randomDoubleInRange(-0.25, 0.25));

			new PPLine(Particle.DUST_COLOR_TRANSITION, oldLocation, location).data(new DustTransition(Color.YELLOW, Color.YELLOW, 0.75f))
				.countPerMeter(12).groupingDistance(0).spawnAsPlayerActive(mPlayer);
		}
	}

	@Override
	public int getInitialAbilityDuration() {
		return mFlightDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return mCurrDuration >= 0 ? getInitialAbilityDuration() - mCurrDuration : 0;
	}
}

