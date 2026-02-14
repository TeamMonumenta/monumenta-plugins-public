package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityManager;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.abilities.MultipleChargeAbility;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.ScorchedEarthCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager.PlayerItemStats;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class ScorchedEarth extends MultipleChargeAbility implements PotionAbility, AbilityWithDuration {
	private static final String SCORCHED_EARTH_POTION_METAKEY = "ScorchedEarthPotion";

	private static final int COOLDOWN = 30 * 20;
	private static final int CHARGES = 2;
	private static final int MAX_SCORCHED_DURATION = 5 * 20;
	private static final int SCORCHED_DURATION_PER_APPLICATION = 20;
	private static final double SCORCHED_DAMAGE_PER_SECOND_1 = 0.4;
	private static final double SCORCHED_DAMAGE_PER_SECOND_2 = 0.5;
	private static final int SCORCHED_DAMAGE_INTERVAL = 5;
	private static final int ON_ENTER_SCORCH = 2 * SCORCHED_DURATION_PER_APPLICATION;
	private static final double SLOWNESS_AMP_1 = 0.25;
	private static final double SLOWNESS_AMP_2 = 0.35;
	private static final double WEAKNESS_AMP_1 = 0.1;
	private static final double WEAKNESS_AMP_2 = 0.15;
	private static final int DURATION = 10 * 20;
	private static final double RADIUS = 6;
	private static final int SHRAPNEL_COUNT = 3;
	public static final double SHRAPNEL_RADIUS = 2.5;

	public static final String CHARM_COOLDOWN = "Scorched Earth Cooldown";
	public static final String CHARM_CHARGES = "Scorched Earth Charge";
	public static final String CHARM_DURATION = "Scorched Earth Duration";
	public static final String CHARM_RADIUS = "Scorched Earth Radius";
	public static final String CHARM_SCORCH_ON_ENTER = "Scorched Earth Scorched Duration On Enter";
	public static final String CHARM_SLOWNESS = "Scorched Earth Slowness Amplifier";
	public static final String CHARM_WEAKNESS = "Scorched Earth Weakness Amplifier";
	public static final String CHARM_SHRAPNEL_COUNT = "Scorched Earth Shrapnel Count";
	public static final String CHARM_SHRAPNEL_RADIUS = "Scorched Earth Shrapnel Radius";
	public static final String CHARM_SCORCHED_DURATION = "Scorched Earth Scorched Duration";
	public static final String CHARM_SCORCHED_MAX_DURATION = "Scorched Earth Scorched Max Duration";
	public static final String CHARM_SCORCHED_DAMAGE = "Scorched Earth Scorched Damage";

	public static final Style SCORCH_COLOR = Style.style(TextColor.color(0xDD452C));

	public static final AbilityInfo<ScorchedEarth> INFO =
		new AbilityInfo<>(ScorchedEarth.class, "Scorched Earth", ScorchedEarth::new)
			.linkedSpell(ClassAbility.SCORCHED_EARTH)
			.scoreboardId("ScorchedEarth")
			.shorthandName("SE")
			.actionBarColor(TextColor.color(230, 134, 0))
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Deploy a circular zone in which your potions break into fragments that Scorch enemies.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.BROWN_DYE);

	private static class Instance {
		final Location mLocation;
		final int mEndTick;
		final PlayerItemStats mStats;
		final ScorchedEarthCS mCosmetic;
		final ArrayList<UUID> mMobsHit = new ArrayList<>();
		int mLastPotionSplashTick;

		private Instance(Location location, int endTick, PlayerItemStats playerItemStats, ScorchedEarthCS cosmetic,
						 int lastPotionSplashTick) {
			mLocation = location;
			mEndTick = endTick;
			mStats = playerItemStats;
			mCosmetic = cosmetic;
			mLastPotionSplashTick = lastPotionSplashTick;
		}

		public boolean hitMob(LivingEntity mob) {
			if (mMobsHit.contains(mob.getUniqueId())) {
				return false;
			}
			mMobsHit.add(mob.getUniqueId());
			return true;
		}

		public Location location() {
			return mLocation;
		}

		public int endTick() {
			return mEndTick;
		}

		public ScorchedEarthCS cosmetic() {
			return mCosmetic;
		}

		private void setLastPotionSplashTick(int newTick) {
			mLastPotionSplashTick = newTick;
		}
	}

	private class ScorchedInfo {
		final LivingEntity mTarget;
		final PlayerItemStats mPlayerItemStats;
		int mTimer;
		int mRemainingDuration;
		boolean mIsDone = false;

		private ScorchedInfo(LivingEntity target, PlayerItemStats playerItemStats, int initialDuration) {
			mTarget = target;
			mPlayerItemStats = playerItemStats;
			mTimer = 0;
			mRemainingDuration = initialDuration;
		}

		private void reapply(int extraDuration) {
			mRemainingDuration = Math.min(mScorchedMaxDuration, mRemainingDuration + extraDuration);
		}

		private void tick() {
			if (mAlchemistPotions == null) {
				return;
			}
			mRemainingDuration = Math.max(0, mRemainingDuration - 5);
			mTimer += 5;

			if (mTimer >= SCORCHED_DAMAGE_INTERVAL) {
				double damage = mScorchedDamage / ((double) mScorchedDuration / SCORCHED_DAMAGE_INTERVAL)
					* mAlchemistPotions.getDamage(mPlayerItemStats);
				DamageUtils.damage(
					mPlayer,
					mTarget,
					new DamageEvent.Metadata(
						DamageEvent.DamageType.MAGIC,
						mInfo.getLinkedSpell(),
						mPlayerItemStats
					),
					damage,
					true,
					false,
					false
				);
				EntityUtils.applyFire(mPlugin, SCORCHED_DAMAGE_INTERVAL + 1, mTarget, mPlayer);
				mCosmetic.damageEffect(mTarget, mPlayer);
				mTimer -= SCORCHED_DAMAGE_INTERVAL;
				if (mRemainingDuration == 0) {
					mIsDone = true;
				}
			}
		}

		private boolean isDone() {
			return mAlchemistPotions == null || mTarget.isDead() || !mTarget.isValid() || mIsDone;
		}
	}

	private final List<Instance> mActiveInstances = new ArrayList<>();
	private final ConcurrentHashMap<UUID, ScorchedInfo> mScorchedInfos = new ConcurrentHashMap<>();

	private final int mDuration;
	private final double mRadius;
	private final int mShrapnelCount;
	private final double mShrapnelRadius;
	private final double mSlownessAmp;
	private final double mWeaknessAmp;
	private final int mScorchOnEnterDuration;
	private final int mScorchedDuration;
	private final int mScorchedMaxDuration;
	private final double mScorchedDamage;
	private int mLastCastTicks = 0;
	private final ScorchedEarthCS mCosmetic;
	private @Nullable AlchemistPotions mAlchemistPotions;
	private int mCurrDuration = -1;

	public ScorchedEarth(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mMaxCharges = CHARGES + (int) CharmManager.getLevel(mPlayer, CHARM_CHARGES);
		mCharges = getTrackedCharges();
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mShrapnelCount = SHRAPNEL_COUNT + (int) CharmManager.getLevel(mPlayer, CHARM_SHRAPNEL_COUNT);
		mShrapnelRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SHRAPNEL_RADIUS, SHRAPNEL_RADIUS);
		mSlownessAmp = isLevelOne() ? SLOWNESS_AMP_1 : SLOWNESS_AMP_2
			+ CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		mWeaknessAmp = isLevelOne() ? WEAKNESS_AMP_1 : WEAKNESS_AMP_2
			+ CharmManager.getLevelPercentDecimal(mPlayer, CHARM_WEAKNESS);
		mScorchOnEnterDuration = CharmManager.getDuration(mPlayer, CHARM_SCORCH_ON_ENTER, ON_ENTER_SCORCH);
		mScorchedDuration = CharmManager.getDuration(mPlayer, CHARM_SCORCHED_DURATION, SCORCHED_DURATION_PER_APPLICATION);
		mScorchedMaxDuration = CharmManager.getDuration(mPlayer, CHARM_SCORCHED_MAX_DURATION, MAX_SCORCHED_DURATION);
		mScorchedDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SCORCHED_DAMAGE, isLevelOne() ? SCORCHED_DAMAGE_PER_SECOND_1 : SCORCHED_DAMAGE_PER_SECOND_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new ScorchedEarthCS());

		Bukkit.getScheduler().runTask(mPlugin, () ->
			mAlchemistPotions = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class));
	}

	private void applyOrRefreshScorched(LivingEntity mob, PlayerItemStats playerItemStats, int duration) {
		@Nullable ScorchedInfo scorchedInfo = mScorchedInfos.get(mob.getUniqueId());
		if (scorchedInfo == null) {
			mScorchedInfos.put(mob.getUniqueId(), new ScorchedInfo(mob, playerItemStats, duration));
			return;
		}

		scorchedInfo.reapply(duration);
	}

	private void shootShrapnel(Location from, Vector velocity, boolean isGruesome, PlayerItemStats playerItemStats) {
		@Nullable Item physicsItem = EntityUtils.createUnpickableItem(mCosmetic.getFragmentMaterial(), from);
		if (physicsItem == null) {
			return;
		}

		physicsItem.setVelocity(velocity);
		new BukkitRunnable() {
			int mTimer = 0;

			@Override
			public void run() {
				mTimer++;
				if (!physicsItem.isValid() || mAlchemistPotions == null || mPlayer.isDead() || !mPlayer.isValid() || !mPlayer.isOnline()) {
					physicsItem.remove();
					cancel();
					return;
				}

				if (physicsItem.isOnGround() || mTimer >= 100) {
					physicsItem.remove();
					shrapnelHitGround(physicsItem.getLocation(), isGruesome, playerItemStats);
					cancel();
					return;
				}

				mCosmetic.shrapnelFlyEffect(physicsItem.getLocation().clone().add(0, 0.25, 0), isGruesome, mPlayer);
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}

	private void shrapnelHitGround(Location where, boolean isGruesome, PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null) {
			return;
		}

		double radius = mShrapnelRadius * (mAlchemistPotions.getRadius(playerItemStats) / 3);
		mCosmetic.shrapnelLandEffect(where, radius, isGruesome, mPlayer);
		new Hitbox.SphereHitbox(where, radius)
			.getHitMobs()
			.forEach(mob -> applyOrRefreshScorched(mob, playerItemStats, mScorchedDuration));
	}

	private void handleCooldown() {
		if (mWasOnCooldown && !isOnCooldown()) {
			mCharges = mMaxCharges;
			AbilityManager.getManager().trackCharges(mPlayer, ClassAbility.SCORCHED_EARTH, mCharges);
			showOffCooldownMessage();
			ClientModHandler.updateAbility(mPlayer, this);
		}

		mWasOnCooldown = isOnCooldown();

		if (!isOnCooldown() && mCharges != mMaxCharges) {
			putOnCooldown();
		}
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		if (mAlchemistPotions == null) {
			mActiveInstances.clear();
			return;
		}
		handleCooldown();
		if (!mActiveInstances.isEmpty()) {
			for (Iterator<Instance> iterator = mActiveInstances.iterator(); iterator.hasNext(); ) {
				Instance instance = iterator.next();
				int timeRemaining = instance.endTick() - Bukkit.getCurrentTick();

				if (timeRemaining <= 0) {
					iterator.remove();
					continue;
				}

				instance.cosmetic().activeEffects(mPlayer, instance.location(), mRadius, timeRemaining, mDuration);

				new Hitbox.SphereHitbox(instance.mLocation, mRadius)
					.getHitMobs()
					.forEach(mob -> {
						// Periodically apply "difficult terrain" effects
						EntityUtils.applySlow(mPlugin, 10, mSlownessAmp, mob);
						EntityUtils.applyWeaken(mPlugin, 10, mWeaknessAmp, mob);

						if (!instance.hitMob(mob)) {
							return;
						}

						applyOrRefreshScorched(mob, instance.mStats, mScorchOnEnterDuration);
						mCosmetic.damageEffect(mob, mPlayer);
					});
			}
		}

		mScorchedInfos.values().removeIf(ScorchedInfo::isDone);
		mScorchedInfos.values().forEach(ScorchedInfo::tick);

		if (mCurrDuration >= mDuration) {
			mCurrDuration = -1;
			ClientModHandler.updateAbility(mPlayer, this);
		}

		if (mCurrDuration >= 0) {
			mCurrDuration += 5;
		}
	}

	@Override
	public void alchemistPotionThrown(ThrownPotion potion) {
		if (!mPlayer.isSneaking()) {
			return;
		}
		final int ticks = mPlayer.getTicksLived();
		// Prevent double casting on accident
		if (ticks - mLastCastTicks <= 5 || !consumeCharge()) {
			return;
		}
		mLastCastTicks = ticks;
		potion.setMetadata(SCORCHED_EARTH_POTION_METAKEY, new FixedMetadataValue(mPlugin, null));

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);
	}

	@Override
	public boolean createAura(Location loc, ThrownPotion potion, Vector originalPotionVelocity, PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null) {
			return false;
		}

		AlchemistPotions nonNullAlchemistPotions = mAlchemistPotions;
		int currentTick = Bukkit.getCurrentTick();
		if (potion.hasMetadata(SCORCHED_EARTH_POTION_METAKEY)) {
			loc.setDirection(loc.toVector().subtract(mPlayer.getLocation().toVector()));
			mCosmetic.landEffects(mPlayer, loc, mRadius, mDuration);
			final ScorchedEarthCS activeCosmetic = mCosmetic.copyForActiveInstance();
			// immediately do periodic effects too (the next ordinary execution may be delayed by up to 5 ticks)
			activeCosmetic.activeEffects(mPlayer, loc, mRadius, mDuration, mDuration);
			mActiveInstances.add(new Instance(loc, currentTick + mDuration, playerItemStats, activeCosmetic, currentTick));
			return true;
		}

		if (mActiveInstances.isEmpty()) {
			return false;
		}

		List<Instance> hitInstances = mActiveInstances.stream()
			.filter(instance ->
				instance.mLocation.distanceSquared(loc) <= Math.pow(mRadius, 2) &&
				currentTick - instance.mLastPotionSplashTick >= AlchemistPotions.IFRAME_BETWEEN_POT
			)
			.toList();

		if (hitInstances.isEmpty()) {
			return false;
		}

		Location slightlyElevatedLoc = loc.clone().add(0, 0.2, 0);
		// Extract a unit vector with only X and Z components for direction
		// which will be made to point upwards later.
		Vector velocity = originalPotionVelocity.clone().setY(0).normalize();
		double rotation = Math.PI * 2 / mShrapnelCount;
		new BukkitRunnable() {
			final Iterator<Instance> mInstanceIterator = hitInstances.iterator();

			@Override
			public void run() {
				if (!mInstanceIterator.hasNext()) {
					cancel();
					return;
				}
				Instance instance = mInstanceIterator.next();
				instance.setLastPotionSplashTick(currentTick);
				instance.mCosmetic.potionLandInZoneEffect(slightlyElevatedLoc, mPlayer);
				for (int i = 0; i < mShrapnelCount; i++) {
					double horizontalMultiplier = FastUtils.randomDoubleInRange(0.05, 0.25);
					double height = FastUtils.randomDoubleInRange(0.2, 0.35);
					double angleOffset = FastUtils.randomDoubleInRange(0, Math.PI / 4) - Math.PI / 8;
					Vector fragmentVelocity = velocity.clone().rotateAroundY(angleOffset).multiply(horizontalMultiplier).setY(height);
					shootShrapnel(slightlyElevatedLoc, fragmentVelocity, nonNullAlchemistPotions.isGruesome(potion), playerItemStats);
					velocity.rotateAroundY(rotation);
				}
				// Offset the next cluster by a bit
				velocity.rotateAroundY(rotation / 2);
			}
		}.runTaskTimer(mPlugin, 0, 5);

		return false;
	}

	@Override
	public int getInitialAbilityDuration() {
		return mDuration;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return mCurrDuration >= 0 ? getInitialAbilityDuration() - mCurrDuration : 0;
	}

	private static Description<ScorchedEarth> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addDashedLine()
			.addLine("Throwing a potion while sneaking creates")
			.addLine("a scorched area that lasts for %t.")
				.statValues(stat(a -> a.mDuration, DURATION))
			.addLine()
			.addLine("The area slows and weakens mobs inside, and")
			.addLine("mobs entering it are *Scorched* for +%t.").styles(SCORCH_COLOR)
				.statValues(stat(a -> a.mScorchOnEnterDuration, ON_ENTER_SCORCH))
			.addLine()
			.addStat("Effect: %p1 Slowness")
				.statValues(stat(a -> a.mSlownessAmp, SLOWNESS_AMP_1))
			.addStat("Effect: %p1 Weakness")
				.statValues(stat(a -> a.mWeaknessAmp, WEAKNESS_AMP_1))
			.addLine()
			.addLine("Potions that splash inside the area break into %d")
				.statValues(stat(a -> a.mShrapnelCount, SHRAPNEL_COUNT))
			.addLine("smaller fragments that *Scorch* mobs for +%t.").styles(SCORCH_COLOR)
				.statValues(stat(a -> a.mScorchedDuration, SCORCHED_DURATION_PER_APPLICATION))
			.addLine()
			.addLine("*Scorched* mobs take rapid damage over time.").styles(SCORCH_COLOR)
			.addLine("Its duration can be stacked up to a maximum of %t,")
				.statValues(stat(a -> a.mScorchedMaxDuration, MAX_SCORCHED_DURATION))
			.addLine("which is consumed over time.")
			.addLine()
			.addStat("Scorch Damage: %p1 (s) over 1s (of potion damage)")
				.statValues(stat(a -> a.mScorchedDamage, SCORCHED_DAMAGE_PER_SECOND_1))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mRadius, RADIUS))
			.addStat("Charges: %d")
				.statValues(stat(a -> a.mMaxCharges, CHARGES))
			.addStat("Cooldown: %t (all charges refreshed at once)")
				.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<ScorchedEarth> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Scorched Earth*'s slowness,").styles(UNDERLINED)
			.addLine("weakness, and the damage of *Scorch*.").styles(SCORCH_COLOR)
			.addLine()
			.addStatComparison("Effect: %p1 -> %p2 Slowness")
				.statValues(stat(SLOWNESS_AMP_1), stat(a -> a.mSlownessAmp, SLOWNESS_AMP_2))
			.addStatComparison("Effect: %p1 -> %p2 Weakness")
				.statValues(stat(WEAKNESS_AMP_1), stat(a -> a.mWeaknessAmp, WEAKNESS_AMP_2))
			.addStatComparison("Scorch Damage: %p1 -> %p2 (s)")
				.statValues(stat(SCORCHED_DAMAGE_PER_SECOND_1), stat(a -> a.mScorchedDamage, SCORCHED_DAMAGE_PER_SECOND_2))
			.addDashedLine();
	}
}
