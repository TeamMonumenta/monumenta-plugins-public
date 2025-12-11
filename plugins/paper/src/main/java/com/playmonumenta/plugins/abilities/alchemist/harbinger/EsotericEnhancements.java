package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.alchemist.AlchemistPotions;
import com.playmonumenta.plugins.abilities.alchemist.BrutalAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.GruesomeAlchemy;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.bosses.bosses.abilities.AlchemicalAberrationBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.EsotericEnhancementsCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.Nullable;

public class EsotericEnhancements extends Ability implements PotionAbility {
	private static final double POTION_DAMAGE_MULTIPLIER_1 = 0.9;
	private static final double POTION_DAMAGE_MULTIPLIER_2 = 1.25;
	private static final double POTION_DAMAGE_RAW_1 = 7;
	private static final double POTION_DAMAGE_RAW_2 = 8;
	private static final double DAMAGE_RADIUS = 4;
	private static final int SUMMON_DURATION = 30;
	private static final double SLOW_AMOUNT = 0.35;
	private static final int SLOW_DURATION = 4 * 20;
	private static final int COOLDOWN = 5 * 20;
	private static final double TARGET_RADIUS = 8;
	private static final double MAX_TARGET_Y = 6;
	private static final int LIFETIME = 15 * 20;
	private static final int TICK_INTERVAL = 5;
	private static final int TICK_INTERVAL_TARGET_RESET = 30;

	public static final String CHARM_DAMAGE = "Esoteric Enhancements Damage";
	public static final String CHARM_RADIUS = "Esoteric Enhancements Radius";
	public static final String CHARM_DURATION = "Esoteric Enhancements Slow Duration";
	public static final String CHARM_COOLDOWN = "Esoteric Enhancements Cooldown";
	public static final String CHARM_CREEPER = "Esoteric Enhancements Creeper";
	public static final String CHARM_REACTION_TIME = "Esoteric Enhancements Reaction Time";
	public static final String CHARM_FUSE = "Esoteric Enhancements Fuse Time";
	public static final String CHARM_SPEED = "Esoteric Enhancements Speed";
	public static final String CHARM_KNOCKBACK = "Esoteric Enhancements Knockback";
	public static final String CHARM_SLOW = "Esoteric Enhancements Slow Amplifier";

	public static final AbilityInfo<EsotericEnhancements> INFO =
		new AbilityInfo<>(EsotericEnhancements.class, "Esoteric Enhancements", EsotericEnhancements::new)
			.linkedSpell(ClassAbility.ESOTERIC_ENHANCEMENTS)
			.scoreboardId("Esoteric")
			.shorthandName("Es")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Stack the effects of your Gruesome and Brutal potions on an enemy to summon a friendly creeper.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.CREEPER_HEAD);

	private class ApplicationInfo {
		private int mApplicationTime;
		private final boolean mIsGruesome;
		private final Location mLoc;
		private final double mRadius;

		private ApplicationInfo(int applicationTime, boolean isGruesome, Location loc, double radius) {
			mApplicationTime = applicationTime;
			mIsGruesome = isGruesome;
			mLoc = loc;
			mRadius = radius;
		}

		private boolean isReactionCompleteWithNextApplication(boolean nextApplicationIsGruesome) {
			return mIsGruesome != nextApplicationIsGruesome;
		}

		private boolean isWithinRadius(Location otherLoc) {
			return mLoc.distanceSquared(otherLoc) <= Math.pow(mRadius, 2);
		}

		private void refresh() {
			mApplicationTime = Bukkit.getCurrentTick();
		}

		private boolean periodicEffectsAndIsNotValid() {
			boolean isNotValid = Bukkit.getCurrentTick() - mApplicationTime > mReactionTime;
			if (isNotValid) {
				return true;
			}

			mCosmetic.periodicAppliedLocationEffects(mPlayer, mLoc, mRadius, mIsGruesome, mAlchemistPotions);
			return false;
		}
	}

	private final double mDamageMultiplier;
	private final double mDamageRaw;
	private final int mReactionTime;
	private final double mRadius;
	private final int mSlowDuration;
	private final double mSlow;
	private final double mKnockbackMultiplier;
	private final int mCreeperCount;
	private final ConcurrentLinkedQueue<ApplicationInfo> mAppliedLocs = new ConcurrentLinkedQueue<>();
	private final EsotericEnhancementsCS mCosmetic;
	private final List<LivingEntity> mTargets;
	private final ConcurrentLinkedQueue<Creeper> mActiveAberrations = new ConcurrentLinkedQueue<>();

	private @Nullable AlchemistPotions mAlchemistPotions;
	private @Nullable GruesomeAlchemy mGruesomeAlchemy;
	private @Nullable BrutalAlchemy mBrutalAlchemy;

	public EsotericEnhancements(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? POTION_DAMAGE_MULTIPLIER_1 : POTION_DAMAGE_MULTIPLIER_2);
		mDamageRaw = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? POTION_DAMAGE_RAW_1 : POTION_DAMAGE_RAW_2);
		mReactionTime = CharmManager.getDuration(mPlayer, CHARM_REACTION_TIME, SUMMON_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, DAMAGE_RADIUS);
		mSlowDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, SLOW_DURATION);
		mSlow = SLOW_AMOUNT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOW);
		mKnockbackMultiplier = 1 + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_KNOCKBACK);
		mCreeperCount = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_CREEPER);
		mTargets = new ArrayList<>();
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EsotericEnhancementsCS());

		Bukkit.getScheduler().runTask(
			plugin,
			() -> {
				mAlchemistPotions = mPlugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, AlchemistPotions.class);
				mGruesomeAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, GruesomeAlchemy.class);
				mBrutalAlchemy = plugin.mAbilityManager.getPlayerAbilityIgnoringSilence(player, BrutalAlchemy.class);
			}
		);
	}

	public synchronized void registerExplosion(LivingEntity boss) {
		if (boss instanceof Creeper aberration) {
			mActiveAberrations.remove(aberration);
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	public synchronized void registerExpiry(@Nullable Creeper aberration) {
		if (aberration != null) {
			mActiveAberrations.remove(aberration);
			aberration.remove();
			mCosmetic.expireEffects(mPlayer, aberration);
			ClientModHandler.updateAbility(mPlayer, this);
		}
	}

	public void createPuddle(Location loc, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null) {
			return;
		}

		createPuddle(loc, isGruesome, playerItemStats, mAlchemistPotions.getRadius(playerItemStats));
	}

	public void createPuddle(Location loc, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats, double radius) {
		if (mAlchemistPotions == null) {
			return;
		}

		if (isOnCooldown()) {
			return;
		}

		Optional<ApplicationInfo> appliedLocInfo = mAppliedLocs.stream()
			.filter(appInfo -> appInfo.isWithinRadius(loc))
			.findFirst();

		if (appliedLocInfo.isEmpty()) {
			mAppliedLocs.add(
				new ApplicationInfo(
					Bukkit.getCurrentTick(),
					isGruesome,
					loc,
					radius));
			return;
		}

		ApplicationInfo foundAppInfo = appliedLocInfo.get();
		if (foundAppInfo.isReactionCompleteWithNextApplication(isGruesome)) {
			// Cancel currently active creepers
			mActiveAberrations.forEach(this::registerExpiry);
			// Summoning each aberration should be delayed by 2 ticks to prevent them from targeting the same enemy
			putOnCooldown();
			new BukkitRunnable() {
				int mCount = 0;
				@Override public void run() {
					summonAberration(foundAppInfo.mLoc.clone().add(0, 0.1, 0), playerItemStats);
					mCount++;
					if (mCount >= mCreeperCount) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 2);
			mAppliedLocs.clear();
			return;
		}

		foundAppInfo.refresh();
	}

	@Override
	public boolean createAura(Location loc, ThrownPotion potion, Vector originalPotionVelocity, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null) {
			return false;
		}
		boolean isGruesome = mAlchemistPotions.isGruesome(potion);
		createPuddle(loc, isGruesome, playerItemStats);
		return false;
	}

	@Override
	public void periodicTrigger(boolean twoHertz, boolean oneSecond, int ticks) {
		mAppliedLocs.removeIf(ApplicationInfo::periodicEffectsAndIsNotValid);
	}

	private void summonAberration(Location loc, ItemStatManager.PlayerItemStats playerItemStats) {
		if (mAlchemistPotions == null) {
			registerExpiry(null);
			return;
		}

		AlchemistPotions nonNullAlchemistPotions = mAlchemistPotions;
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Creeper aberration = (Creeper) LibraryOfSoulsIntegration.summon(loc, "Alchemicalaberration");
			if (aberration == null) {
				MMLog.warning("Failed to spawn Alchemical Aberration from Library of Souls");
				registerExpiry(null);
				return;
			}
			EntityUtils.setRemoveEntityOnUnload(aberration);
			aberration.customName(Component.text(mCosmetic.getAberrationName()));

			AlchemicalAberrationBoss alchemicalAberrationBoss = BossUtils.getBossOfClass(aberration, AlchemicalAberrationBoss.class);
			if (alchemicalAberrationBoss == null) {
				MMLog.warning("Failed to get AlchemicalAberrationBoss for AlchemicalAberration");
				registerExpiry(null);
				return;
			}

			double damage = mDamageRaw + mDamageMultiplier * nonNullAlchemistPotions.getDamage(playerItemStats);
			alchemicalAberrationBoss.spawn(
				mPlayer,
				damage,
				mRadius,
				isLevelOne() ? 1 : 2,
				mSlowDuration,
				mSlow,
				mKnockbackMultiplier,
				playerItemStats,
				mCosmetic,
				this,
				mGruesomeAlchemy,
				mBrutalAlchemy,
				mAlchemistPotions);

			aberration.setMaxFuseTicks(CharmManager.getDuration(mPlayer, CHARM_FUSE, aberration.getMaxFuseTicks()));
			aberration.setExplosionRadius((int) mRadius);
			EntityUtils.setAttributeBase(aberration, Attribute.GENERIC_MOVEMENT_SPEED, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SPEED, EntityUtils.getAttributeBaseOrDefault(aberration, Attribute.GENERIC_MOVEMENT_SPEED, 0)));
			aberration.setPowered(isLevelTwo());

			mCosmetic.esotericSummonEffect(mPlayer, aberration);

			mActiveAberrations.add(aberration);
			ClientModHandler.updateAbility(mPlayer, this);

			new BukkitRunnable() {
				int mTicks = 0;
				@Nullable LivingEntity mTarget = null;

				private boolean shouldFindNewTarget() {
					if (mTarget == null || mTarget.isDead() || !mTarget.isValid() || mTarget.getHealth() <= 0) {
						return true;
					}

					return !isValidTarget(aberration, mTarget, true, new ArrayList<>());
				}

				@Override
				public void run() {
					if (mTicks >= LIFETIME || !mPlayer.isOnline() || mPlayer.isDead() || !aberration.isValid()) {
						registerExpiry(aberration);
						mTargets.remove(mTarget);
						this.cancel();
						return;
					}

					if (mTicks % TICK_INTERVAL == 0) {
						/* target validation can sometimes return false even if the target is clearly alive and force target switch
						 * if it already have target and target health > 0, do not switch targets
						 *
						 * target validation checking needs to be empty list for all living mobs nearby
						 */
						if (shouldFindNewTarget()) {
							LivingEntity newTarget = findTarget(aberration, mTargets);
							if (newTarget != null) {
								mTarget = newTarget;
								mTargets.add(newTarget);
							}
						}

						if (mTarget != null && mTarget.isValid()) {
							aberration.setTarget(mTarget);
							// Make the aberration immediately start moving
							aberration.getPathfinder().moveTo(mTarget, 1);
						} else if (aberration.getTarget() == null && aberration.getWorld() == mPlayer.getWorld()) {
							// If there's no target to be found, and the aberration has no old target either, follow the player (but keep some distance)
							double distanceSquared = aberration.getLocation().distanceSquared(mPlayer.getLocation());
							if (distanceSquared > 4 * 4) {
								// Slow down a bit near the player to get less jerky movement
								aberration.getPathfinder().moveTo(mPlayer, distanceSquared > 6 * 6 ? 1 : 0.66);
							} else {
								aberration.getPathfinder().stopPathfinding();
							}
						}
					}

					mTargets.removeIf(e -> e.isDead() || !e.isValid());
					if ((mTicks + 1) % TICK_INTERVAL_TARGET_RESET == 0) {
						mTargets.clear();
					}

					mCosmetic.periodicEffects(mPlayer, aberration, mTicks);

					mTicks += 1;
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}, 1);
	}

	private boolean isValidTarget(Creeper aberration, @Nullable LivingEntity mob, boolean withPathfinding, List<LivingEntity> targets) {
		return mob != null
			&& !mob.isDead()
			&& mob.isValid()
			&& !targets.contains(mob)
			&& !mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)
			&& !DamageUtils.isImmuneToDamage(mob, DamageEvent.DamageType.MAGIC)
			&& mob.getLocation().distance(aberration.getLocation()) <= 1.5 * TARGET_RADIUS
			&& Math.abs(mob.getLocation().getY() - aberration.getLocation().getY()) <= 1.5 * MAX_TARGET_Y
			&& (!withPathfinding || canPathfind(aberration, mob));
	}

	private boolean canPathfind(Creeper aberration, LivingEntity mob) {
		Pathfinder.PathResult path = aberration.getPathfinder().findPath(mob);
		return path != null && path.getFinalPoint() != null && path.getFinalPoint().distanceSquared(mob.getLocation()) < 1;
	}

	/**
	 * Finds a target for the aberration. Prioritizes targets that it can see and pathfind to within 1.5 meters, then ones it cannot see but pathfind to,
	 * followed by ones it can only see, and finally any within the targeting radius, always prioritizing the highest-health targets among those groups.
	 */
	private @Nullable LivingEntity findTarget(Creeper aberration, List<LivingEntity> targets) {
		List<LivingEntity> nearbyMobs = new Hitbox.SphereHitbox(aberration.getLocation(), TARGET_RADIUS)
			.getHitMobs(aberration)
			.stream()
			.filter(mob -> Math.abs(mob.getLocation().getY() - aberration.getLocation().getY()) <= MAX_TARGET_Y)
			.filter(mob -> isValidTarget(aberration, mob, false, targets))
			.sorted(Comparator.comparingDouble(Damageable::getHealth).reversed())
			.toList();

		List<LivingEntity> lineOfSightNearbyMobs = nearbyMobs.stream()
			.filter(mob -> mob.hasLineOfSight(aberration))
			.toList();

		LivingEntity fallback = (lineOfSightNearbyMobs.isEmpty() ? nearbyMobs : lineOfSightNearbyMobs)
			.stream()
			.findFirst()
			.orElse(null);

		return (lineOfSightNearbyMobs.isEmpty() ? nearbyMobs : lineOfSightNearbyMobs)
			.stream()
		   .filter(le -> canPathfind(aberration, le))
		   .findFirst()
			.orElse(fallback);
	}

	private static Description<EsotericEnhancements> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When splashing the ground with both Gruesome and Brutal effects within ")
			.addDuration(a -> a.mReactionTime, SUMMON_DURATION)
			.add(" seconds of each other, summon an Alchemical Aberration. The Aberration targets the mob with the highest health within ")
			.add(a -> TARGET_RADIUS, TARGET_RADIUS)
			.add(" blocks and explodes on that mob, dealing ")
			.add(a -> a.mDamageRaw, POTION_DAMAGE_RAW_1, false, Ability::isLevelOne)
			.add(" + ")
			.addPercent(a -> a.mDamageMultiplier, POTION_DAMAGE_MULTIPLIER_1, false, Ability::isLevelOne)
			.add(" of your potion damage and applying ")
			.addPercent(a -> a.mSlow, SLOW_AMOUNT)
			.add(" Slow for ")
			.addDuration(a -> a.mSlowDuration, SLOW_DURATION)
			.add(" seconds, and Level 1 Brutal to all mobs within ")
			.add(a -> a.mRadius, DAMAGE_RADIUS)
			.add(" blocks. The aberration expires after ")
			.addDuration(LIFETIME)
			.add("s.")
			.addCooldown(COOLDOWN);
	}

	private static Description<EsotericEnhancements> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mDamageRaw, POTION_DAMAGE_RAW_2, false, Ability::isLevelTwo)
			.add(" + ")
			.addPercent(a -> a.mDamageMultiplier, POTION_DAMAGE_MULTIPLIER_2, false, Ability::isLevelTwo)
			.add(" of your potion damage, and the level of Brutal applied is increased to 2.");
	}
}
