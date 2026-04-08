package com.playmonumenta.plugins.abilities.alchemist;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
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
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
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

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.classes.Alchemist.BRUTAL_COLOR;
import static com.playmonumenta.plugins.classes.Alchemist.GRUESOME_COLOR;
import static com.playmonumenta.plugins.utils.DescriptionUtils.WHITE;

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

	public static final Style VOLATILE_COLOR = Style.style(TextColor.color(0xCC6729));

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
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Spend *1* potion to throw a concoction that").styles(WHITE)
			.addLine("deals no damage, but makes mobs *Volatile*.").styles(VOLATILE_COLOR)
			.addLine()
			.addStat("Concoction Radius: %p (of potion radius)")
			.statValues(stat(a -> a.mRadiusMultiplier, POTION_RADIUS_MULTIPLIER))
			.addLine()
			.addLine("Afflicting a *Volatile* mob with *Gruesome* or").styles(VOLATILE_COLOR, GRUESOME_COLOR)
			.addLine("*Brutal* causes an explosion that damages that").styles(BRUTAL_COLOR)
			.addLine("mob and other nearby mobs, spreading the")
			.addLine("*Gruesome*/*Brutal* effect to other mobs.").styles(GRUESOME_COLOR, BRUTAL_COLOR)
			.addLine()
			.addLine("Additionally, the level of the *Gruesome*/*Brutal*").styles(GRUESOME_COLOR, BRUTAL_COLOR)
			.addLine("effect applied to the main mob is increased by *1*.").styles(WHITE)
			.addLine()
			.addStat("Direct Damage: %p (s) (of potion damage)")
				.statValues(stat(a -> a.mMainDamageMultiplier, MAIN_DAMAGE_MULTIPLIER_1))
			.addStat("Explosion Damage: %p (s) (of potion damage)")
				.statValues(stat(a -> a.mDetonateDamageMultiplier, DETONATION_DAMAGE_MULTIPLIER))
			.addStat("Explosion Radius: %r")
				.statValues(stat(a -> a.mDetonateRadius, DETONATE_RADIUS))
			.addDashedLine();
	}

	private static Description<VolatileReaction> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("*Volatile* mobs afflicted with both *Gruesome*").styles(VOLATILE_COLOR, GRUESOME_COLOR)
			.addLine("and *Brutal* now cause a second explosion.").styles(BRUTAL_COLOR)
			.addLine()
			.addLine("The direct damage of both explosions is increased.")
			.addLine()
			.addStatComparison("Direct Damage: %p1 -> %p2 (s)")
			.statValues(stat(MAIN_DAMAGE_MULTIPLIER_1), stat(a -> a.mMainDamageMultiplier, MAIN_DAMAGE_MULTIPLIER_2))
			.addDashedLine();
	}

	private static Description<VolatileReaction> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("*Volatile* mobs hit with both *Gruesome* and").styles(VOLATILE_COLOR, GRUESOME_COLOR)
			.addLine("*Brutal* now spread a damaging effect to the").styles(BRUTAL_COLOR)
			.addLine("highest HP mob within %d blocks.")
			.statValues(stat(a -> a.mEnhancementDotSpreadRadius, ENHANCEMENT_DOT_SPREAD_RADIUS))
			.addLine()
			.addLine("The effect can stack up to %d times, and")
			.statValues(stat(a -> a.mEnhancementSpreadCap, ENHANCEMENT_SPREAD_CAP))
			.addLine("for each stack, it deals more damage, damages")
			.addLine("more frequently, and hits more times.")
			.addLine()
			.addStat("Damage: %p (s) per stack (of potion damage)")
			.statValues(stat(a -> a.mEnhancementDotPotencyPerStack, ENHANCEMENT_DOT_POTENCY_PER_STACK))
			.addStat("Interval: every %t, -%t per stack")
			.statValues(stat(a -> a.mEnhancementDotBaseTickDelay, ENHANCEMENT_DOT_BASE_TICK_DELAY), stat(a -> a.mEnhancementTickDelayReductionPerStack, ENHANCEMENT_DOT_TICK_DELAY_REDUCTION_PER_STACK))
			.addStat("Duration: %d hits per stack")
			.statValues(stat(a -> a.mEnhancementDotTicksPerStack, ENHANCEMENT_DOT_TICKS_PER_STACK))
			.addDashedLine();
	}
}
