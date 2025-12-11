package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.VolatileReactionCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import java.util.Comparator;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class VolatileReaction extends Ability implements PotionAbility {
	private static final ClassAbility VOLATILE_DOT_ABILITY = ClassAbility.VOLATILE_REACTION_DOT;
	public static final String POTION_METAKEY = "VolatileReactionPotion";
	private static final int COOLDOWN = 15 * 20;
	private static final int PRIME_DURATION = 10 * 20;
	private static final double POTION_RADIUS_MULTIPLIER = 1.5;
	private static final double DETONATE_RADIUS = 3;
	private static final double MAIN_DAMAGE_MULTIPLIER_1 = 0.5;
	private static final double MAIN_DAMAGE_MULTIPLIER_2 = 0.6;
	private static final double DETONATION_DAMAGE_MULTIPLIER = 0.1;
	private static final int ENHANCEMENT_SPREAD_CAP = 5;
	private static final double ENHANCEMENT_DOT_POTENCY_PER_STACK = 0.15;
	private static final int ENHANCEMENT_DOT_TICKS_PER_STACK = 3;
	private static final int ENHANCEMENT_DOT_BASE_TICK_DELAY = 30;
	private static final int ENHANCEMENT_DOT_TICK_DELAY_REDUCTION_PER_STACK = 5;
	private static final double ENHANCEMENT_DOT_SPREAD_RADIUS = 5;

	public static final String CHARM_COOLDOWN = "Volatile Reaction Cooldown";
	public static final String CHARM_RADIUS_MULTIPLIER = "Volatile Reaction Radius";
	public static final String CHARM_MAIN_DAMAGE_MULTIPLIER = "Volatile Reaction Main Damage";
	public static final String CHARM_DETONATE_RADIUS = "Volatile Reaction Detonation Radius";
	public static final String CHARM_DETONATE_DAMAGE_MULTIPLIER = "Volatile Reaction Detonation Damage";
	public static final String CHARM_ENHANCEMENT_SPREAD_CAP = "Volatile Reaction Enhancement Spread Cap";
	public static final String CHARM_ENHANCEMENT_DOT_POTENCY_PER_STACK = "Volatile Reaction Enhancement DoT Potency per Stack";
	public static final String CHARM_ENHANCEMENT_DOT_TICKS_PER_STACK = "Volatile Reaction Enhancement DoT Ticks per Stack";
	public static final String CHARM_ENHANCEMENT_DOT_BASE_TICK_DELAY = "Volatile Reaction Enhancement DoT Base Delay";
	public static final String CHARM_ENHANCEMENT_DOT_TICK_DELAY_PER_STACK = "Volatile Reaction Enhancement DoT Tick Delay per Stack";
	public static final String CHARM_ENHANCEMENT_DOT_SPREAD_RADIUS = "Volatile Reaction Enhancement DoT Spread Radius";

	public static final AbilityInfo<VolatileReaction> INFO =
		new AbilityInfo<>(VolatileReaction.class, "Volatile Reaction", VolatileReaction::new)
			.linkedSpell(ClassAbility.VOLATILE_REACTION)
			.scoreboardId("VolatileReaction")
			.shorthandName("VR")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Prime enemies to be detonated by afflicting them with Brutal and Gruesome potions.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", VolatileReaction::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false),
				HOLDING_ALCHEMIST_BAG_RESTRICTION))
			.displayItem(Material.FIRE_CHARGE);

	private class PrimedMobInfo {
		private final UUID mUuid;
		private final int mTicksLived;

		private boolean mAffectedByBrutal = false;
		private boolean mAffectedByGruesome = false;

		private PrimedMobInfo(UUID uuid, int ticksLived) {
			this.mUuid = uuid;
			this.mTicksLived = ticksLived;
		}

		private boolean hasExpired() {
			@Nullable Entity entity = Bukkit.getEntity(mUuid);
			if (entity == null) {
				return true;
			}
			return entity.getTicksLived() - mTicksLived > PRIME_DURATION;
		}

		private boolean isDone() {
			if (isLevelOne() && !isEnhanced()) {
				return mAffectedByBrutal || mAffectedByGruesome;
			}
			return mAffectedByBrutal && mAffectedByGruesome;
		}

		private void setAffected(boolean isGruesome) {
			if (isGruesome) {
				mAffectedByGruesome = true;
				return;
			}
			mAffectedByBrutal = true;
		}

		private boolean isAffected(boolean isGruesome) {
			if (isGruesome) {
				return mAffectedByGruesome;
			}
			return mAffectedByBrutal;
		}

		private boolean isAffectedByBoth() {
			return mAffectedByGruesome && mAffectedByBrutal;
		}
	}

	private class EnhancementDotInfo {
		private final UUID mUuid;
		private final ItemStatManager.PlayerItemStats mPlayerItemStats;
		private final AtomicInteger mStacks;
		private boolean mDotRunnableStarted = false;
		private boolean mDotRunnableFinished = false;

		private EnhancementDotInfo(UUID uuid, int initialStacks, ItemStatManager.PlayerItemStats playerItemStats) {
			this.mUuid = uuid;
			this.mStacks = new AtomicInteger(initialStacks);
			this.mPlayerItemStats = playerItemStats;
		}

		private boolean canAddStack() {
			return mStacks.get() < mEnhancementSpreadCap;
		}

		private void addStack() {
			if (mStacks.get() >= mEnhancementSpreadCap) {
				return;
			}
			mStacks.incrementAndGet();
		}

		private void startRunnableIfNotAlreadyStarted() {
			if (mDotRunnableStarted) {
				return;
			}

			mDotRunnableStarted = true;
			new BukkitRunnable() {
				final @Nullable LivingEntity mMobToDamage = (LivingEntity) Bukkit.getEntity(mUuid);
				int mTicks = 0;
				int mRuns = 0;

				@Override
				public void run() {
					// Adaptive period for runs - prevent it from getting faster than a 0.25s delay
					if (mTicks >= Math.max(mEnhancementDotBaseTickDelay - mEnhancementTickDelayReductionPerStack * mStacks.get(), 5)) {
						mRuns++;

						if (mAlchemistPotions == null || mMobToDamage == null || !mMobToDamage.isValid() || mMobToDamage.isDead()) {
							mDotRunnableFinished = true;
							cancel();
							return;
						}

						DamageUtils.damage(
							mPlayer,
							mMobToDamage,
							new DamageEvent.Metadata(
								DamageEvent.DamageType.MAGIC,
								VOLATILE_DOT_ABILITY,
								mPlayerItemStats
							),
							mAlchemistPotions.getDamage() * mEnhancementDotPotencyPerStack * mStacks.get(),
							true,
							false,
							false
						);
						mCosmetic.enhancementDoTTickParticleEffects(mPlayer, mMobToDamage);

						if (mRuns >= mStacks.get() * mEnhancementDotTicksPerStack) {
							mDotRunnableFinished = true;
							cancel();
							return;
						}
						mTicks = 0;
					}
					mTicks++;
				}
			}.runTaskTimer(Plugin.getInstance(), 0, 1);
		}

		private boolean isDone() {
			return mDotRunnableFinished;
		}
	}

	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable GruesomeAlchemy mGruesomeAlchemy;
	private @Nullable BrutalAlchemy mBrutalAlchemy;
	private final ConcurrentHashMap<UUID, PrimedMobInfo> mVolatilePrimedMobs = new ConcurrentHashMap<>();
	private final ConcurrentHashMap<UUID, EnhancementDotInfo> mEnhancementDoTInfos = new ConcurrentHashMap<>();

	private final VolatileReactionCS mCosmetic;
	private final double mGruesomeLevelThreeSlownessAmount;
	private final double mGruesomeLevelThreeVulnerabilityAmount;
	private final double mGruesomeLevelThreeWeakenAmount;
	private final double mBrutalLevelThreeFlatDamageIncrease;
	private final double mBrutalLevelThreeMultDamageIncrease;
	private final double mBrutalLevelThreeExplosionDamageMult;
	private final double mRadiusMultiplier;
	private final double mMainDamageMultiplier;
	private final double mDetonateRadius;
	private final double mDetonateDamageMultiplier;
	private final int mEnhancementSpreadCap;
	private final double mEnhancementDotPotencyPerStack;
	private final int mEnhancementDotTicksPerStack;
	private final int mEnhancementDotBaseTickDelay;
	private final int mEnhancementTickDelayReductionPerStack;
	private final double mEnhancementDotSpreadRadius;

	public VolatileReaction(Plugin plugin, Player player) {
		super(plugin, player, INFO);

		mGruesomeLevelThreeSlownessAmount = GruesomeAlchemy.GRUESOME_ALCHEMY_3_SLOWNESS_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, GruesomeAlchemy.CHARM_SLOWNESS);
		mGruesomeLevelThreeVulnerabilityAmount = GruesomeAlchemy.GRUESOME_ALCHEMY_3_VULNERABILITY_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, GruesomeAlchemy.CHARM_VULNERABILITY);
		mGruesomeLevelThreeWeakenAmount = GruesomeAlchemy.GRUESOME_ALCHEMY_3_WEAKEN_AMPLIFIER + CharmManager.getLevelPercentDecimal(mPlayer, GruesomeAlchemy.CHARM_WEAKEN);
		mBrutalLevelThreeFlatDamageIncrease = BrutalAlchemy.DOT_FLAT_INCREASE_3 + CharmManager.getLevel(mPlayer, BrutalAlchemy.CHARM_DOT_INCREASE_DAMAGE_FLAT);
		mBrutalLevelThreeMultDamageIncrease = BrutalAlchemy.DOT_MULT_INCREASE_3 + CharmManager.getLevelPercentDecimal(mPlayer, BrutalAlchemy.CHARM_DOT_INCREASE_DAMAGE_MULT);
		mBrutalLevelThreeExplosionDamageMult = BrutalAlchemy.DOT_EXPLOSION_MULT_3 + CharmManager.getLevelPercentDecimal(mPlayer, BrutalAlchemy.CHARM_DOT_EXPLOSION_DAMAGE_MULT);
		mRadiusMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_RADIUS_MULTIPLIER, POTION_RADIUS_MULTIPLIER);
		mMainDamageMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_MAIN_DAMAGE_MULTIPLIER, isLevelOne() ? MAIN_DAMAGE_MULTIPLIER_1 : MAIN_DAMAGE_MULTIPLIER_2);
		mDetonateRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DETONATE_RADIUS, DETONATE_RADIUS);
		mDetonateDamageMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DETONATE_DAMAGE_MULTIPLIER, DETONATION_DAMAGE_MULTIPLIER);
		mEnhancementSpreadCap = ENHANCEMENT_SPREAD_CAP + (int) CharmManager.getLevel(mPlayer, CHARM_ENHANCEMENT_SPREAD_CAP);
		mEnhancementDotPotencyPerStack = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_DOT_POTENCY_PER_STACK, ENHANCEMENT_DOT_POTENCY_PER_STACK);
		mEnhancementDotTicksPerStack = ENHANCEMENT_DOT_TICKS_PER_STACK + (int) CharmManager.getLevel(mPlayer, CHARM_ENHANCEMENT_DOT_TICKS_PER_STACK);
		mEnhancementDotBaseTickDelay = ENHANCEMENT_DOT_BASE_TICK_DELAY + (int) CharmManager.getLevel(mPlayer, CHARM_ENHANCEMENT_DOT_BASE_TICK_DELAY);
		mEnhancementTickDelayReductionPerStack = ENHANCEMENT_DOT_TICK_DELAY_REDUCTION_PER_STACK + (int) CharmManager.getLevel(mPlayer, CHARM_ENHANCEMENT_DOT_TICK_DELAY_PER_STACK);
		mEnhancementDotSpreadRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCEMENT_DOT_SPREAD_RADIUS, ENHANCEMENT_DOT_SPREAD_RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new VolatileReactionCS());

		Bukkit.getScheduler().runTask(
			plugin,
			() -> {
				mAlchemistPotions = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
				mGruesomeAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, GruesomeAlchemy.class);
				mBrutalAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, BrutalAlchemy.class);
			}
		);
	}

	public boolean cast() {
		if (mAlchemistPotions == null || isOnCooldown()) {
			return false;
		}

		if (mAlchemistPotions.getCharges() > 0) {
			putOnCooldown();
			// This call already decrements 1 charge
			mAlchemistPotions.throwPotion(false, POTION_METAKEY);
		}

		return true;
	}

	private void explode(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null) {
			return;
		}
		AlchemistPotions nonNullAlchemistPotions = mAlchemistPotions;

		// Main damage
		DamageUtils.damage(
			mPlayer,
			mob,
			new DamageEvent.Metadata(
				DamageEvent.DamageType.MAGIC,
				mInfo.getLinkedSpell(),
				playerItemStats
			),
			nonNullAlchemistPotions.getDamage() * mMainDamageMultiplier,
			true,
			false,
			false
		);

		// Detonation (to other mobs only)
		Hitbox detonationHitbox = new Hitbox.SphereHitbox(mob.getLocation(), mDetonateRadius);
		detonationHitbox.getHitMobs(mob).forEach(hitMob -> {
			// Damage
			DamageUtils.damage(
				mPlayer,
				hitMob,
				new DamageEvent.Metadata(
					DamageEvent.DamageType.MAGIC,
					mInfo.getLinkedSpell(),
					playerItemStats
				),
				nonNullAlchemistPotions.getDamage() * mDetonateDamageMultiplier,
				true,
				false,
				false
			);

			// Effect spread
			nonNullAlchemistPotions.applyEffects(hitMob, isGruesome, playerItemStats);
		});

		// Increase the potency of the effect on the mob.
		if (isGruesome && mGruesomeAlchemy != null) {
			mGruesomeAlchemy.applyHigherLevel(mob, playerItemStats);
		} else if (!isGruesome && mBrutalAlchemy != null) {
			mBrutalAlchemy.applyHigherLevel(mob, playerItemStats);
		}

		GruesomeAlchemy.tryDoEnhancementEffect(mGruesomeAlchemy, mob);
		BrutalAlchemy.tryDoEnhancementEffect(mBrutalAlchemy, mob);
		mCosmetic.detonatedMobParticleEffects(mPlayer, mPlugin, mob, isGruesome, mAlchemistPotions);
	}

	private void hitMob(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null) {
			return;
		}

		PrimedMobInfo mobInfo = mVolatilePrimedMobs.get(mob.getUniqueId());
		if (mobInfo == null) {
			return;
		}

		if (isLevelOne() && !isEnhanced()) {
			if (!mobInfo.mAffectedByBrutal && !mobInfo.mAffectedByGruesome) {
				mobInfo.setAffected(isGruesome);
				explode(mob, isGruesome, playerItemStats);
			}
		} else {
			if (!mobInfo.isAffected(isGruesome)) {
				mobInfo.setAffected(isGruesome);
				if (mobInfo.isAffectedByBoth()) {
					if (isLevelTwo()) {
						explode(mob, isGruesome, playerItemStats);
					}
					if (isEnhanced()) {
						// Spread Enhancement's DoT
						Hitbox spreadHitbox = new Hitbox.SphereHitbox(mob.getLocation(), mEnhancementDotSpreadRadius);
						// Look for highest hp target that doesn't already have max stacks
						Optional<LivingEntity> mobToSpreadTo = spreadHitbox.getHitMobs()
							.stream()
							.filter(le -> !DamageUtils.isImmuneToDamage(le, DamageEvent.DamageType.MAGIC))
							.filter(le -> !le.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG))
							.filter(possibleSpreadTarget -> {
								if (!mEnhancementDoTInfos.containsKey(possibleSpreadTarget.getUniqueId())) {
									return true;
								}

								EnhancementDotInfo dotInfo = mEnhancementDoTInfos.get(possibleSpreadTarget.getUniqueId());
								return dotInfo.canAddStack();
							})
							.max(Comparator.comparingDouble(Damageable::getHealth));
						if (mobToSpreadTo.isPresent()) {
							// Increment stacks
							UUID mobSpreadUuid = mobToSpreadTo.get().getUniqueId();
							if (!mEnhancementDoTInfos.containsKey(mobSpreadUuid)) {
								mEnhancementDoTInfos.put(mobSpreadUuid, new EnhancementDotInfo(mobSpreadUuid, 1, playerItemStats));
							} else {
								mEnhancementDoTInfos.get(mobSpreadUuid).addStack();
							}
							mCosmetic.spreadEnhancementDoT(mPlayer, mob, mobToSpreadTo.get());
						}
					}
				} else {
					explode(mob, isGruesome, playerItemStats);
				}
			}
		}
	}

	private void clearOldEntries() {
		mVolatilePrimedMobs.entrySet().removeIf(entry -> entry.getValue().hasExpired() || entry.getValue().isDone());
		mEnhancementDoTInfos.entrySet().removeIf(entry -> entry.getValue().isDone());
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats, int level, boolean refreshBrutalDot) {
		clearOldEntries();
		hitMob(mob, isGruesome, playerItemStats);
		mEnhancementDoTInfos.forEach((uuid, dotInfo) -> dotInfo.startRunnableIfNotAlreadyStarted());
	}

	@Override
	public boolean createAura(Location loc, @Nullable ThrownPotion potion, Vector originalPotionVelocity, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null || potion == null || !potion.hasMetadata(POTION_METAKEY)) {
			return false;
		}

		clearOldEntries();
		double finalRadius = mAlchemistPotions.getRadius() * mRadiusMultiplier;
		mCosmetic.landEffects(mPlayer, mPlugin, loc, finalRadius);
		Hitbox hitbox = new Hitbox.SphereHitbox(loc, finalRadius);
		hitbox.getHitMobs().forEach(mob -> mVolatilePrimedMobs.put(
			mob.getUniqueId(),
			new PrimedMobInfo(mob.getUniqueId(), mob.getTicksLived())
		));

		return true;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		clearOldEntries();
		mVolatilePrimedMobs.forEach((uuid, primedMobInfo) -> {
			Entity entity = Bukkit.getEntity(uuid);
			if (entity == null) {
				return;
			}

			if (primedMobInfo.isDone()) {
				return;
			}

			mCosmetic.primedMobParticleEffects(entity, mPlayer, !primedMobInfo.mAffectedByBrutal, !primedMobInfo.mAffectedByGruesome, mAlchemistPotions);
		});
	}

	private static Description<VolatileReaction> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.addTrigger()
			.add(" to consume a potion and throw a Volatile Concoction with ")
			.addPercent(a -> a.mRadiusMultiplier, POTION_RADIUS_MULTIPLIER)
			.add(" radius which deals no damage, but marks all hit mobs as Volatile.")
			.add(" When Volatile mobs gain either Gruesome or Brutal effects, they explode, taking ")
			.addPercent(a -> a.mMainDamageMultiplier, MAIN_DAMAGE_MULTIPLIER_1, false, Ability::isLevelOne)
			.add(" of your potion's damage, and dealing ")
			.addPercent(a -> a.mDetonateDamageMultiplier, DETONATION_DAMAGE_MULTIPLIER)
			.add(" of your potion's damage within a ")
			.add(a -> a.mDetonateRadius, DETONATE_RADIUS)
			.add(" block radius, spreading the Gruesome or Brutal effect they were hit with. ")
			.add("Additionally, the potency of the effect applied to the Volatile mobs is increased by one level. ")
			.addCooldown(COOLDOWN)
			.add(Component.text("\nBrutal Alchemy level 3 effect:\n").color(NamedTextColor.YELLOW))
			.add(getDescriptionBrutalAlchemy3())
			.add(Component.text("\nGruesome Alchemy level 3 effect:\n").color(NamedTextColor.YELLOW))
			.add(getDescriptionGruesomeAlchemy3());
	}

	private static Description<VolatileReaction> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Volatile mobs hit with both Gruesome and Brutal effects now explode for a second time. The main damage of both explosions is increased to ")
			.addPercent(a -> a.mMainDamageMultiplier, MAIN_DAMAGE_MULTIPLIER_2, false, Ability::isLevelTwo)
			.add(" of your potion's damage.");
	}

	private static Description<VolatileReaction> getDescriptionBrutalAlchemy3() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Refreshing the DoT now increases its potency by ")
			.add(a -> a.mBrutalLevelThreeFlatDamageIncrease, BrutalAlchemy.DOT_FLAT_INCREASE_3)
			.add(" + ")
			.addPercent(a -> a.mBrutalLevelThreeMultDamageIncrease, BrutalAlchemy.DOT_MULT_INCREASE_3)
			.add(" of your potion's damage. The additional damage from the explosion is now ")
			.addPercent(a -> a.mBrutalLevelThreeExplosionDamageMult, BrutalAlchemy.DOT_EXPLOSION_MULT_3)
			.add(" of your potion's damage.");
	}

	private static Description<VolatileReaction> getDescriptionGruesomeAlchemy3() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("The slow is increased to ")
			.addPercent(a -> a.mGruesomeLevelThreeSlownessAmount, GruesomeAlchemy.GRUESOME_ALCHEMY_3_SLOWNESS_AMPLIFIER)
			.add(", the vulnerability is increased to ")
			.addPercent(a -> a.mGruesomeLevelThreeVulnerabilityAmount, GruesomeAlchemy.GRUESOME_ALCHEMY_3_VULNERABILITY_AMPLIFIER)
			.add(", and the weakness is increased to ")
			.addPercent(a -> a.mGruesomeLevelThreeWeakenAmount, GruesomeAlchemy.GRUESOME_ALCHEMY_3_WEAKEN_AMPLIFIER)
			.add(".");
	}

	private static Description<VolatileReaction> getDescriptionEnhancement() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Volatile mobs hit with both Gruesome and Brutal effects now spread a DoT with stacking potency to the enemy with the highest remaining health within ")
			.add(a -> a.mEnhancementDotSpreadRadius, ENHANCEMENT_DOT_SPREAD_RADIUS)
			.add(" blocks (capped at ")
			.add(a -> a.mEnhancementSpreadCap, ENHANCEMENT_SPREAD_CAP)
			.add(" stacks). The DoT has a base delay of ")
			.addDuration(a -> a.mEnhancementDotBaseTickDelay, ENHANCEMENT_DOT_BASE_TICK_DELAY)
			.add("s between each tick. For each stack, it ticks an additional ")
			.add(a -> a.mEnhancementDotTicksPerStack, ENHANCEMENT_DOT_TICKS_PER_STACK)
			.add(" times, deals ")
			.addPercent(a -> a.mEnhancementDotPotencyPerStack, ENHANCEMENT_DOT_POTENCY_PER_STACK)
			.add(" more of your potion's damage, and has the tick delay reduced by ")
			.addDuration(a -> a.mEnhancementTickDelayReductionPerStack, ENHANCEMENT_DOT_TICK_DELAY_REDUCTION_PER_STACK)
			.add("s.");
	}
}
