package com.playmonumenta.plugins.abilities.alchemist.harbinger;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.DescriptionBuilder;
import com.playmonumenta.plugins.abilities.alchemist.PotionAbility;
import com.playmonumenta.plugins.bosses.bosses.abilities.AlchemicalAberrationBoss;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.alchemist.harbinger.EsotericEnhancementsCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enums.AttributeType;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Creeper;
import org.bukkit.entity.Damageable;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

public class EsotericEnhancements extends Ability implements PotionAbility {
	private static final double ABERRATION_POTION_DAMAGE_MULTIPLIER_1 = 0.9;
	private static final double ABERRATION_POTION_DAMAGE_MULTIPLIER_2 = 1.25;
	private static final double ABERRATION_POTION_DAMAGE_RAW_1 = 7;
	private static final double ABERRATION_POTION_DAMAGE_RAW_2 = 8;
	private static final double ABERRATION_DAMAGE_RADIUS = 3;
	private static final int ABERRATION_SUMMON_DURATION = 30;
	private static final double ABERRATION_SLOW_AMOUNT = 0.35;
	private static final int ABERRATION_SLOW_DURATION = 4 * 20;
	private static final int ABERRATION_COOLDOWN = 5 * 20;
	private static final double ABERRATION_TARGET_RADIUS = 8;
	private static final double MAX_TARGET_Y = 6;
	private static final int ABERRATION_LIFETIME = 15 * 20;
	private static final int TICK_INTERVAL = 5;
	private static final int TICK_INTERVAL_TARGET_RESET = 30;

	public static final String CHARM_DAMAGE = "Esoteric Enhancements Damage";
	public static final String CHARM_RADIUS = "Esoteric Enhancements Radius";
	public static final String CHARM_SLOWNESS = "Esoteric Enhancements Slowness Amplifier";
	public static final String CHARM_DURATION = "Esoteric Enhancements Slowness Duration";
	public static final String CHARM_COOLDOWN = "Esoteric Enhancements Cooldown";
	public static final String CHARM_CREEPER = "Esoteric Enhancements Creeper";
	public static final String CHARM_REACTION_TIME = "Esoteric Enhancements Reaction Time";
	public static final String CHARM_FUSE = "Esoteric Enhancements Fuse Time";
	public static final String CHARM_SPEED = "Esoteric Enhancements Speed";
	public static final String CHARM_KNOCKBACK = "Esoteric Enhancements Knockback";

	public static final AbilityInfo<EsotericEnhancements> INFO =
		new AbilityInfo<>(EsotericEnhancements.class, "Esoteric Enhancements", EsotericEnhancements::new)
			.linkedSpell(ClassAbility.ESOTERIC_ENHANCEMENTS)
			.scoreboardId("Esoteric")
			.shorthandName("Es")
			.descriptions(getDescription1(), getDescription2())
			.simpleDescription("Stack the effects of gruesome and brutal on an enemy to summon a friendly creeper.")
			.cooldown(ABERRATION_COOLDOWN, CHARM_COOLDOWN)
			.displayItem(Material.CREEPER_HEAD);

	private final double mDamageMultiplier;
	private final double mDamageRaw;
	private final int mReactionTime;
	private final double mRadius;
	private final int mSlowDuration;
	private final double mSlow;
	private final double mKnockbackMultiplier;
	private final Map<LivingEntity, Integer> mAppliedMobs = new HashMap<>();
	private final EsotericEnhancementsCS mCosmetic;

	private final List<LivingEntity> mTargets;

	public EsotericEnhancements(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mDamageMultiplier = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? ABERRATION_POTION_DAMAGE_MULTIPLIER_1 : ABERRATION_POTION_DAMAGE_MULTIPLIER_2);
		mDamageRaw = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? ABERRATION_POTION_DAMAGE_RAW_1 : ABERRATION_POTION_DAMAGE_RAW_2);
		mReactionTime = CharmManager.getDuration(mPlayer, CHARM_REACTION_TIME, ABERRATION_SUMMON_DURATION);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, ABERRATION_DAMAGE_RADIUS);
		mSlowDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, ABERRATION_SLOW_DURATION);
		mSlow = ABERRATION_SLOW_AMOUNT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS);
		mKnockbackMultiplier = 1 + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_KNOCKBACK);
		mTargets = new ArrayList<>();

		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new EsotericEnhancementsCS());
	}

	@Override
	public void apply(LivingEntity mob, boolean isGruesome, ItemStatManager.PlayerItemStats playerItemStats) {
		if (isGruesome) {
			mAppliedMobs.put(mob, mob.getTicksLived());
		} else if (!isOnCooldown()) {
			// Clear out list so it doesn't build up
			mAppliedMobs.entrySet().removeIf((entry) -> (entry.getKey().getTicksLived() - entry.getValue() > mReactionTime));

			// If it's still in the list, it was applied recently enough
			if (mAppliedMobs.remove(mob) != null) {
				mTargets.clear();
				int num = 1 + (int) CharmManager.getLevel(mPlayer, CHARM_CREEPER);
				Location summonLoc = mob.getLocation();
				// summoning each should be delayed by 2 ticks, to prevent creepers targeting the same enemy.
				new BukkitRunnable() {
					int mCount = 0;

					@Override
					public void run() {
						summonAberration(summonLoc, playerItemStats);
						mCount++;
						if (mCount >= num) {
							this.cancel();
						}
					}
				}.runTaskTimer(mPlugin, 0, 2);
				putOnCooldown();
			}
		}
	}

	private void summonAberration(Location loc, ItemStatManager.PlayerItemStats playerItemStats) {
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			Creeper aberration = (Creeper) LibraryOfSoulsIntegration.summon(loc, "Alchemicalaberration");
			if (aberration == null) {
				MMLog.warning("Failed to spawn Alchemical Aberration from Library of Souls");
				return;
			}
			EntityUtils.setRemoveEntityOnUnload(aberration);
			aberration.customName(Component.text(mCosmetic.getAberrationName()));

			AlchemicalAberrationBoss alchemicalAberrationBoss = BossUtils.getBossOfClass(aberration, AlchemicalAberrationBoss.class);
			if (alchemicalAberrationBoss == null) {
				MMLog.warning("Failed to get AlchemicalAberrationBoss for AlchemicalAberration");
				return;
			}

			double damage = mDamageRaw + mDamageMultiplier * playerItemStats.getItemStats().get(AttributeType.POTION_DAMAGE.getItemStat());
			alchemicalAberrationBoss.spawn(mPlayer, damage, mRadius, mSlowDuration, mSlow, mKnockbackMultiplier, playerItemStats, mCosmetic);

			aberration.setMaxFuseTicks(CharmManager.getDuration(mPlayer, CHARM_FUSE, aberration.getMaxFuseTicks()));
			aberration.setExplosionRadius((int) mRadius);
			EntityUtils.setAttributeBase(aberration, Attribute.GENERIC_MOVEMENT_SPEED, CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_SPEED, EntityUtils.getAttributeBaseOrDefault(aberration, Attribute.GENERIC_MOVEMENT_SPEED, 0)));
			if (isLevelTwo()) {
				aberration.setPowered(true);
			}

			mCosmetic.esotericSummonEffect(mPlayer, aberration);

			new BukkitRunnable() {
				int mTicks = 0;
				@Nullable LivingEntity mTarget = null;

				@Override
				public void run() {
					if (mTicks >= ABERRATION_LIFETIME || !mPlayer.isOnline() || mPlayer.isDead() || !aberration.isValid()) {
						mCosmetic.expireEffects(mPlayer, aberration);
						aberration.remove();
						this.cancel();
						return;
					}

					if (mTicks % TICK_INTERVAL == 0) {
						/* target validation can sometimes return false even if the target is clearly alive and force target switch
						 * if it already have target and target health > 0, do not switch targets
						 *
						 * target validation checking needs to be empty list for all living mobs nearby
						 */
						if (!(mTarget != null && mTarget.getHealth() > 0) &&
							!isValidTarget(aberration, mTarget, true, new ArrayList<>())) {
							LivingEntity newTarget = findTarget(aberration, mTargets);
							if (newTarget != null) {
								mTarget = newTarget;
								mTargets.add(newTarget);
							}
						}

						if (mTarget != null && mTarget.isValid()) {
							aberration.setTarget(mTarget);
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
		// remove nearby mobs around target to prevent overspill damage
		List<LivingEntity> nearbyTargeted = new ArrayList<>();
		if (!targets.isEmpty()) {
			for (LivingEntity le : targets) {
				nearbyTargeted.addAll(EntityUtils.getNearbyMobs(le.getLocation(), 2));
				nearbyTargeted.add(le);
			}
		}

		return mob != null
			&& mob.isValid()
			&& mob.getLocation().distance(aberration.getLocation()) <= 1.5 * ABERRATION_TARGET_RADIUS
			&& Math.abs(mob.getLocation().getY() - aberration.getLocation().getY()) <= 1.5 * MAX_TARGET_Y
			&& !DamageUtils.isImmuneToDamage(mob, DamageEvent.DamageType.MAGIC)
			&& !mob.getScoreboardTags().contains(AbilityUtils.IGNORE_TAG)
			&& (!withPathfinding || canPathfind(aberration, mob))
			&& !nearbyTargeted.contains(mob);
	}

	private boolean canPathfind(Creeper aberration, LivingEntity mob) {
		Pathfinder.PathResult path = aberration.getPathfinder().findPath(mob);
		return path != null && path.getFinalPoint() != null && path.getFinalPoint().distanceSquared(mob.getLocation()) < 1;
	}

	/**
	 * Finds a target for the aberration. Prioritizes targets that it can see and pathfind to within 1.5 meters, then ones it cannot see but pathfind to,
	 * followed by ones it can only see,and finally any within the targeting radius, always prioritizing the highest-health targets among those groups.
	 */
	private @Nullable LivingEntity findTarget(Creeper aberration, List<LivingEntity> targets) {

		List<LivingEntity> nearbyMobs = EntityUtils.getNearbyMobs(aberration.getLocation(), ABERRATION_TARGET_RADIUS, aberration).stream()
			.filter(mob -> Math.abs(mob.getLocation().getY() - aberration.getLocation().getY()) <= MAX_TARGET_Y)
			.filter(mob -> isValidTarget(aberration, mob, false, targets))
			.sorted(Comparator.comparingDouble(Damageable::getHealth).reversed())
			.toList();

		List<LivingEntity> lineOfSightNearbyMobs = nearbyMobs.stream().filter(mob -> mob.hasLineOfSight(aberration)).toList();

		LivingEntity fallback = (lineOfSightNearbyMobs.isEmpty() ? nearbyMobs : lineOfSightNearbyMobs).stream().findFirst().orElse(null);

		return (lineOfSightNearbyMobs.isEmpty() ? nearbyMobs : lineOfSightNearbyMobs).stream()
			.filter(le -> canPathfind(aberration, le))
			.findFirst().orElse(fallback);
	}

	private static Description<EsotericEnhancements> getDescription1() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("When afflicting a mob with a Brutal potion within ")
			.addDuration(a -> a.mReactionTime, ABERRATION_SUMMON_DURATION)
			.add(" seconds of afflicting that mob with a Gruesome potion, summon an Alchemical Aberration. The Aberration targets the mob with the highest health within ")
			.add(a -> ABERRATION_TARGET_RADIUS, ABERRATION_TARGET_RADIUS)
			.add(" blocks and explodes on that mob, dealing ")
			.add(a -> a.mDamageRaw, ABERRATION_POTION_DAMAGE_RAW_1, false, Ability::isLevelOne)
			.add(" + ")
			.addPercent(a -> a.mDamageMultiplier, ABERRATION_POTION_DAMAGE_MULTIPLIER_1, false, Ability::isLevelOne)
			.add(" of your potion damage and applying ")
			.addPercent(a -> a.mSlow, ABERRATION_SLOW_AMOUNT)
			.add(" Slowness for ")
			.addDuration(a -> a.mSlowDuration, ABERRATION_SLOW_DURATION)
			.add(" seconds to all mobs within ")
			.add(a -> a.mRadius, ABERRATION_DAMAGE_RADIUS)
			.add(" blocks.")
			.addCooldown(ABERRATION_COOLDOWN);
	}

	private static Description<EsotericEnhancements> getDescription2() {
		return new DescriptionBuilder<>(() -> INFO)
			.add("Damage is increased to ")
			.add(a -> a.mDamageRaw, ABERRATION_POTION_DAMAGE_RAW_2, false, Ability::isLevelTwo)
			.add(" + ")
			.addPercent(a -> a.mDamageMultiplier, ABERRATION_POTION_DAMAGE_MULTIPLIER_2, false, Ability::isLevelTwo)
			.add(" of your potion damage.");
	}
}
