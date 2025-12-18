package com.playmonumenta.plugins.abilities.scout.ranger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.scout.ranger.RendingRazorCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.Constants.TICKS_PER_SECOND;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class RendingRazor extends Ability {
	private static final int EMBED_DAMAGE_1 = 14;
	private static final int EMBED_DAMAGE_2 = 16;
	private static final int REND_DAMAGE_1 = 20;
	private static final int REND_DAMAGE_2 = 24;
	private static final int COOLDOWN_1 = TICKS_PER_SECOND * 6;
	private static final int COOLDOWN_2 = TICKS_PER_SECOND * 5;
	private static final int RAZOR_TRAVEL_TIME = TICKS_PER_SECOND * 2;
	private static final double MAXIMUM_BLOCK_DISTANCE = 14.0;
	private static final double MAXIMUM_REND_DISTANCE = 60.0;
	private static final float PULL_FORCE = 0.5f;
	private static final double REND_SPEED = 1.0; // blocks per tick
	private static final double SLOW_EFFECT_2 = 0.2;
	private static final int SLOW_DURATION_2 = TICKS_PER_SECOND * 4;

	public static final String CHARM_EMBED_DAMAGE = "Rending Razor Embed Damage";
	public static final String CHARM_REND_DAMAGE = "Rending Razor Rend Damage";
	public static final String CHARM_COOLDOWN = "Rending Razor Cooldown";
	public static final String CHARM_SPEED = "Rending Razor Travel Speed";
	public static final String CHARM_REND_SPEED = "Rending Razor Rend Speed";
	public static final String CHARM_SLOWNESS = "Rending Razor Slowness Amplifier";
	public static final String CHARM_SLOWNESS_DURATION = "Rending Razor Slowness Duration";
	public static final String CHARM_RAZOR_RANGE = "Rending Razor Range";
	public static final String CHARM_RAZOR_SIZE = "Rending Razor Size";

	public static final AbilityInfo<RendingRazor> INFO =
		new AbilityInfo<>(RendingRazor.class, "Rending Razor", RendingRazor::new)
			.linkedSpell(ClassAbility.RENDING_RAZOR)
			.scoreboardId("RendingRazor")
			.shorthandName("RR")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Throw a razor that embeds itself into a hit enemy. Recasting the skill returns the " +
				"razor and damages enemies in its path.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", RendingRazor::cast,
				new AbilityTrigger(AbilityTrigger.Key.RIGHT_CLICK)
					.keyOptions(AbilityTrigger.KeyOptions.NO_USABLE_ITEMS)
					.lookDirections(AbilityTrigger.LookDirection.LEVEL)))
			.displayItem(Material.SHEARS);

	private final RendingRazorCS mCosmetic;
	private final double mRazorSpeedMultiplier;
	private final double mRendSpeed;
	private final double mEmbedDamage;
	private final double mRendDamage;
	private final int mSlownessDuration;
	private final double mSlownessPotency;
	private final double mRazorRange;
	private final double mRadius;
	private @Nullable LivingEntity mEmbeddedRazorTarget;

	public RendingRazor(final Plugin plugin, final Player player) {
		super(plugin, player, INFO);
		mRazorSpeedMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SPEED, 1.0);
		mRendSpeed = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_REND_SPEED, REND_SPEED);
		mEmbedDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_EMBED_DAMAGE,
			(isLevelTwo() ? EMBED_DAMAGE_2 : EMBED_DAMAGE_1));
		mRendDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_REND_DAMAGE,
			(isLevelTwo() ? REND_DAMAGE_2 : REND_DAMAGE_1));
		mSlownessDuration = CharmManager.getDuration(mPlayer, CHARM_SLOWNESS_DURATION, SLOW_DURATION_2);
		mSlownessPotency = SLOW_EFFECT_2 + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		mRazorRange = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RAZOR_RANGE, MAXIMUM_BLOCK_DISTANCE);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RAZOR_SIZE, 1);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(mPlayer, new RendingRazorCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			if (mEmbeddedRazorTarget != null) {
				rendFromTarget(mEmbeddedRazorTarget);
				mEmbeddedRazorTarget = null;
			}
			return false;
		}

		mCosmetic.razorCast(mPlayer);
		ClientModHandler.updateAbility(mPlayer, this);
		putOnCooldown();

		double razorDuration = RAZOR_TRAVEL_TIME / mRazorSpeedMultiplier;
		cancelOnDeath(new BukkitRunnable() {
			Location mRazorLoc = mPlayer.getEyeLocation();
			int mTicks = 0;

			@Override
			public void run() {
				if (!mPlayer.getWorld().equals(mRazorLoc.getWorld()) ||
					mTicks >= razorDuration) {
					cancel();
					mEmbeddedRazorTarget = null;
					return;
				}

				Vector inc = tearDropCalc(mTicks, mRazorRange, razorDuration);
				inc = VectorUtils.rotateTargetDirection(inc, mPlayer.getYaw() - 90, mPlayer.getPitch());
				mRazorLoc = mPlayer.getEyeLocation().add(inc);

				mCosmetic.razorProjectileEffects(mPlayer, mRazorLoc);
				if (mTicks % 3 == 0) {
					mCosmetic.razorTravelSound(mPlayer, mRazorLoc);
				}
				final List<LivingEntity> hitEnemies = new Hitbox.SphereHitbox(mRazorLoc, mRadius).getHitMobs();
				hitEnemies.removeIf(e -> e.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG));
				if (!hitEnemies.isEmpty()) {
					final LivingEntity target = hitEnemies.get(0);
					mCosmetic.razorHit(mPlayer, target.getLocation());
					DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.PROJECTILE_SKILL, mEmbedDamage,
						mInfo.getLinkedSpell(), true, true);

					if (target.isValid() && !target.isDead()) {
						applySlowness(target);
						mEmbeddedRazorTarget = target;
						this.cancel();
					}
				}

				mTicks++;
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				ClientModHandler.updateAbility(mPlayer, RendingRazor.this);
			}
		}.runTaskTimer(mPlugin, 0, 1));

		return true;
	}

	/**
	 * Note: The below is now a lie since straight mode was more fun during testing<br>
	 * Implementation of a <a href="https://mathworld.wolfram.com/TeardropCurve.html">teardrop curve</a> as a
	 * parameterized function in the XZ plane
	 *
	 * @param t Current time the razor has been active in ticks
	 * @return Vector describing the razor's current position on the curve
	 */
	private Vector tearDropCalc(final int t, final double range, final double razorDuration) {
		final double initLoc = 0 * Math.PI;
		final double radians = -2 * Math.PI * t / razorDuration;

		final double x = (FastUtils.cos(initLoc + radians) - 1) * range / 2;
		// final double tempZCalc = FastUtils.sin(0.5 * radians);
		// z = FastUtils.sin(radians) * Math.pow(tempZCalc, 2) * range / 2;

		return new Vector(x, 0, 0);
	}

	private void rendFromTarget(final LivingEntity target) {
		if (target.isDead()) {
			return;
		}

		mCosmetic.razorRetrieveSound(mPlayer, target.getLocation());
		DamageUtils.damage(mPlayer, target, DamageEvent.DamageType.PROJECTILE_SKILL, mRendDamage, mInfo.getLinkedSpell(),
			true, false);
		MovementUtils.pullTowardsNormalized(mPlayer.getLocation(), target, PULL_FORCE);

		cancelOnDeath(new BukkitRunnable() {

			int mTicks = 0;
			final Location mLoc = target.getLocation().add(0, target.getEyeHeight() / 2, 0);
			boolean mReturnedToPlayer = false;
			Location mOldPlayerLoc = mPlayer.getLocation();
			final List<LivingEntity> mHitEnemies = new ArrayList<>();
			List<LivingEntity> mHitMobTick = new ArrayList<>();

			@Override
			public void run() {
				// cancel if the player teleported (range = 40 blocks)
				if (mOldPlayerLoc.distance(mPlayer.getLocation()) > MAXIMUM_REND_DISTANCE) {
					this.cancel();
				}
				mOldPlayerLoc = mPlayer.getLocation();

				// loop 2 times per tick for more accurate hitbox detection
				for (int i = 0; i <= 1; i++) {
					final Hitbox razorHitbox = new Hitbox.SphereHitbox(mLoc, mRadius);
					mHitMobTick = razorHitbox.getHitMobs();
					mHitMobTick.removeIf(mHitEnemies::contains);
					mHitMobTick.forEach(enemy -> {
						if (enemy != target) {
							DamageUtils.damage(mPlayer, enemy, DamageEvent.DamageType.PROJECTILE_SKILL, mRendDamage,
								mInfo.getLinkedSpell(), true, true);
						}

						if (enemy.isValid() && !enemy.isDead()) {
							applySlowness(enemy);
						}
					});

					mHitEnemies.addAll(mHitMobTick);

					// The mIncrement is calculated by the distance to the player
					final Vector increment = mPlayer.getEyeLocation().toVector().subtract(mLoc.toVector());
					increment.normalize().multiply(mRendSpeed / 2);
					mLoc.add(increment);

					// quit function
					if (mPlayer.getEyeLocation().distance(mLoc) <= mRendSpeed / 2) {
						mReturnedToPlayer = true;
						this.cancel();
					}
				}

				// This should stay out of the for loop to prevent particle/sfx spam
				mCosmetic.razorRetrieve(mPlayer, mLoc);
				if (mTicks % 3 == 0) {
					mCosmetic.razorTravelSound(mPlayer, mLoc);
				}
				if (!mHitMobTick.isEmpty()) {
					mCosmetic.razorPierce(mHitMobTick.get(0).getLocation());
				}

				if (mReturnedToPlayer) {
					mCosmetic.razorReturned(mPlayer.getLocation());
				}
				mTicks++;
			}
		}.runTaskTimer(mPlugin, 0, 1));
	}

	private void applySlowness(final LivingEntity target) {
		if (isLevelTwo()) {
			EntityUtils.applySlow(mPlugin, mSlownessDuration, mSlownessPotency, target);
		}
	}

	private static Description<RendingRazor> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Throw a spinning razor that damages")
			.addLine("the first mob it hits, embedding itself")
			.addLine("into that mob for %t.")
				.statValues(stat(RAZOR_TRAVEL_TIME))
			.addLine()
			.addLine("Recast to rend the razor out of the mob,").styles(UNDERLINED)
			.addLine("dealing damage to it and all mobs on its")
			.addLine("way back to you.")
			.addLine()
			.addStat("Embed Damage: %d1 (p)")
				.statValues(stat(a -> a.mEmbedDamage, EMBED_DAMAGE_1))
			.addStat("Rend Damage: %d1 (p)")
				.statValues(stat(a -> a.mRendDamage, REND_DAMAGE_1))
			.addStat("Max Range: %r")
				.statValues(stat(a -> a.mRazorRange, MAXIMUM_BLOCK_DISTANCE))
			.addStat("Cooldown: %t1")
				.statValues(cooldown(COOLDOWN_1))
			.addDashedLine();
	}

	private static Description<RendingRazor> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Rending Razor*'s damage").styles(UNDERLINED)
			.addLine("and reduce its cooldown.")
			.addLine()
			.addLine("*Rending Razor* now slows all mobs hit.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Embed Damage: %d1 -> %d2 (p)")
				.statValues(stat(EMBED_DAMAGE_1), stat(a -> a.mEmbedDamage, EMBED_DAMAGE_2))
			.addStatComparison("Rend Damage: %d1 -> %d2 (p)")
				.statValues(stat(REND_DAMAGE_1), stat(a -> a.mRendDamage, REND_DAMAGE_2))
			.addStatComparison("Cooldown: %t1 -> %t2")
				.statValues(cooldown(COOLDOWN_1), cooldown(COOLDOWN_2))
			.addStat("Effect: %p Slowness for %t")
				.statValues(stat(a -> a.mSlownessPotency, SLOW_EFFECT_2), stat(a -> a.mSlownessDuration, SLOW_DURATION_2))
			.addDashedLine();
	}
}
