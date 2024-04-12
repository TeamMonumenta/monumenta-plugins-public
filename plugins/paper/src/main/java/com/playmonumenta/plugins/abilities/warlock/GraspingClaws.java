package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.GraspingClawsCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import javax.annotation.Nullable;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class GraspingClaws extends Ability implements AbilityWithDuration {

	private static final int PULL_RADIUS = 8;
	private static final float PULL_SPEED = 0.175f;
	private static final double AMPLIFIER_1 = 0.2;
	private static final double AMPLIFIER_2 = 0.3;
	private static final int PULL_DAMAGE = 3;
	private static final int CLEAVE_FLAT_DAMAGE = 4;
	private static final double CLEAVE_PERCENT_DAMAGE = 0.1;
	private static final int CLEAVE_RADIUS = 3;
	private static final int CLEAVE_WINDOW = 4 * 20;
	private static final int DURATION = 8 * 20;
	private static final int COOLDOWN = 16 * 20;
	private static final int CAGE_RADIUS = 6;
	private static final int CAGE_DURATION = 6 * 20;
	private static final double HEAL_AMOUNT = 0.05;
	private static final int CAGE_DELAY = 1 * 20;

	public static final String CHARM_COOLDOWN = "Grasping Claws Cooldown";
	public static final String CHARM_PROJ_SPEED = "Grasping Claws Projectile Speed";
	public static final String CHARM_PULL_STRENGTH = "Grasping Claws Pull Strength";
	public static final String CHARM_PULL_DAMAGE = "Grasping Claws Pull Damage";
	public static final String CHARM_PULL_RADIUS = "Grasping Claws Pull Radius";
	public static final String CHARM_SLOW = "Grasping Claws Slowness Amplifier";
	public static final String CHARM_SLOW_DURATION = "Grasping Claws Slowness Duration";
	public static final String CHARM_CLEAVE_DAMAGE = "Grasping Claws Cleave Damage";
	public static final String CHARM_CLEAVE_RADIUS = "Grasping Claws Cleave Radius";
	public static final String CHARM_CAGE_RADIUS = "Grasping Claws Cage Radius";
	public static final String CHARM_CAGE_HEALING = "Grasping Claws Cage Healing";
	public static final String CHARM_CAGE_DURATION = "Grasping Claws Cage Duration";

	public static final AbilityInfo<GraspingClaws> INFO =
			new AbilityInfo<>(GraspingClaws.class, "Grasping Claws", GraspingClaws::new)
					.linkedSpell(ClassAbility.GRASPING_CLAWS)
					.scoreboardId("GraspingClaws")
					.shorthandName("GC")
					.descriptions(
							("Pressing the drop key while sneaking and holding a scythe or projectile weapon fires a projectile " +
									"that pulls nearby enemies towards it once it makes contact with a mob or block. " +
									"Mobs caught in the projectile's %s block radius are given %s%% Slowness for %s seconds and take %s magic damage. Cooldown: %ss.")
									.formatted(PULL_RADIUS, StringUtils.multiplierToPercentage(AMPLIFIER_1), StringUtils.ticksToSeconds(DURATION), PULL_DAMAGE, StringUtils.ticksToSeconds(COOLDOWN)),
							("Slowness is increased to %s%%. " +
									"After the projectile lands, your next melee scythe attack within %s seconds will deal %s + %s%% of the attack’s damage as magic damage to all mobs in a %s block radius.")
									.formatted(StringUtils.multiplierToPercentage(AMPLIFIER_2), StringUtils.ticksToSeconds(CLEAVE_WINDOW), CLEAVE_FLAT_DAMAGE, StringUtils.multiplierToPercentage(CLEAVE_PERCENT_DAMAGE), CLEAVE_RADIUS),
							("When the projectile lands, an impenetrable cage is summoned at its location. " +
									"Non-boss mobs within a %s block radius of the location cannot enter or exit the cage, " +
									"and players within the cage are granted %s%% max health healing every second. " +
									"The cage disappears after %s seconds. Mobs that are immune to crowd control cannot be trapped.")
									.formatted(CAGE_RADIUS, StringUtils.multiplierToPercentage(HEAL_AMOUNT), StringUtils.ticksToSeconds(CAGE_DURATION)))
					.simpleDescription("Fire a projectile that damages, pulls, and slows mobs.")
					.cooldown(COOLDOWN, CHARM_COOLDOWN)
					.addTrigger(new AbilityTriggerInfo<>("cast", "cast", GraspingClaws::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true),
							new AbilityTriggerInfo.TriggerRestriction("holding a scythe or projectile weapon", player -> ItemUtils.isHoe(player.getInventory().getItemInMainHand()) || ItemUtils.isProjectileWeapon(player.getInventory().getItemInMainHand()))))
					.displayItem(Material.BOW);

	private final double mAmplifier;
	private final double mPullDamage;
	private final double mCleaveDamageFlat;
	private final double mCleaveDamagePercent;
	private final double mCleaveRadius;
	private final double mCageRadius;
	private final double mCageHeal;
	private final int mCageDuration;
	private final Map<Projectile, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap = new WeakHashMap<>();
	private @Nullable BukkitRunnable mCleaveRunnable;
	private int mCurrDuration = -1;

	private final GraspingClawsCS mCosmetic;

	public GraspingClaws(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAmplifier = CharmManager.getLevelPercentDecimal(player, CHARM_SLOW) + (isLevelOne() ? AMPLIFIER_1 : AMPLIFIER_2);
		mPullDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_PULL_DAMAGE, PULL_DAMAGE);
		mCleaveDamageFlat = CLEAVE_FLAT_DAMAGE;
		mCleaveDamagePercent = CLEAVE_PERCENT_DAMAGE;
		mCleaveRadius = CharmManager.getRadius(player, CHARM_CLEAVE_RADIUS, CLEAVE_RADIUS);
		mCageRadius = CharmManager.getRadius(player, CHARM_CAGE_RADIUS, CAGE_RADIUS);
		mCageHeal = CharmManager.calculateFlatAndPercentValue(player, CHARM_CAGE_HEALING, HEAL_AMOUNT);
		mCageDuration = CharmManager.getDuration(player, CHARM_CAGE_DURATION, CAGE_DURATION);
		mCleaveRunnable = null;
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GraspingClawsCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}
		World world = mPlayer.getWorld();
		double speed = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PROJ_SPEED, 1.5);
		Snowball proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, speed, mCosmetic.getProjectileName(), mCosmetic.getProjectileParticle());
		mPlayerItemStatsMap.put(proj, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));
		putOnCooldown();
		return true;
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(proj);
		if (playerItemStats != null) {
			Location loc = proj.getLocation();
			World world = proj.getWorld();
			mCosmetic.onLand(mPlayer, world, loc);

			int duration = CharmManager.getDuration(mPlayer, CHARM_SLOW_DURATION, DURATION);
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, CharmManager.getRadius(mPlayer, CHARM_PULL_RADIUS, PULL_RADIUS), mPlayer)) {
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mPullDamage, true, true, false);
				MovementUtils.pullTowards(proj, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PULL_STRENGTH, PULL_SPEED));
				EntityUtils.applySlow(mPlugin, duration, mAmplifier, mob);
			}

			if (isLevelTwo()) {
				if (mCleaveRunnable != null) {
					mCleaveRunnable.cancel();
				}
				mCleaveRunnable = new BukkitRunnable() {
					int mTicks = 0;
					@Override
					public void run() {
						mCosmetic.cleaveReadyTick(mPlayer);

						mTicks += 5;
						if (mTicks >= CLEAVE_WINDOW) {
							this.cancel();
							mCleaveRunnable = null;
						}
					}
				};
				mCleaveRunnable.runTaskTimer(mPlugin, 0, 5);
			}

			if (isEnhanced()) {
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> createCage(loc), CAGE_DELAY);
			}

			proj.remove();
		}
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		if (event.getType() == DamageType.MELEE && ItemUtils.isHoe(mPlayer.getInventory().getItemInMainHand())
			&& mCleaveRunnable != null && !mCleaveRunnable.isCancelled()) {
			mCleaveRunnable.cancel();
			mCleaveRunnable = null;

			double damage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CLEAVE_DAMAGE, mCleaveDamageFlat + event.getDamage() * mCleaveDamagePercent);
			for (LivingEntity mob : EntityUtils.getNearbyMobs(enemy.getLocation(), mCleaveRadius)) {
				DamageUtils.damage(mPlayer, mob, DamageEvent.DamageType.MAGIC, damage, mInfo.getLinkedSpell(), true, true);
			}

			mCosmetic.onCleaveHit(mPlayer, enemy, mCleaveRadius);
		}
		return false;
	}

	private void createCage(Location loc) {
		World world = loc.getWorld();
		mCosmetic.onCageCreation(world, loc);

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);

		new BukkitRunnable() {
			int mT = 0;
			final Hitbox mHitbox = Hitbox.approximateHollowCylinderSegment(loc, 5, mCageRadius - 0.6, mCageRadius + 0.6, Math.PI);
			List<LivingEntity> mMobsAlreadyHit = new ArrayList<>();

			List<Integer> mDegrees1 = new ArrayList<>();
			List<Integer> mDegrees2 = new ArrayList<>();
			List<Integer> mDegrees3 = new ArrayList<>();

			@Override
			public void run() {
				mT++;
				mCurrDuration++;

				// Wall Portion (Particles + Hitbox Definition)
				if (mT % 4 == 0) {
					for (double degree = 0; degree < 360; degree += 20) {
						double radian1 = Math.toRadians(degree);
						Vector vec = new Vector(FastUtils.cos(radian1) * mCageRadius, 0, FastUtils.sin(radian1) * mCageRadius);
						vec = VectorUtils.rotateYAxis(vec, loc.getYaw());
						Location l = loc.clone().add(vec);
						for (int y = 0; y < 5; y++) {
							l.add(0, 1, 0);
							mCosmetic.cageParticle(mPlayer, l);
						}
					}
				}

				List<LivingEntity> entities = mHitbox.getHitMobs();
				for (LivingEntity le : entities) {
					// This list does not update to the mobs hit this tick until after everything runs
					if (!mMobsAlreadyHit.contains(le)) {
						mMobsAlreadyHit.add(le);
						if (!EntityUtils.isCCImmuneMob(le)) {
							Location eLoc = le.getLocation();
							if (loc.distance(eLoc) > mCageRadius) {
								MovementUtils.knockAway(loc, le, 0.3f, true);
							} else {
								MovementUtils.pullTowards(loc, le, 0.15f);
							}
							mCosmetic.onCagedMob(mPlayer, world, eLoc, le);
						}
					}
				}

				/*
				 * Compare the two lists of mobs and only remove from the
				 * actual hit tracker if the mob isn't detected as hit this
				 * tick, meaning it is no longer in the shield wall hitbox
				 * and is thus eligible for another hit.
				 */
				List<LivingEntity> mobsAlreadyHitAdjusted = new ArrayList<>();
				for (LivingEntity mob : mMobsAlreadyHit) {
					if (entities.contains(mob)) {
						mobsAlreadyHitAdjusted.add(mob);
					}
				}
				mMobsAlreadyHit = mobsAlreadyHitAdjusted;
				if (mT >= mCageDuration) {
					this.cancel();
				}

				// Player Effect + Outline Particles
				if (mT % 5 == 0) {
					if (mT % 20 == 0) {
						List<Player> affectedPlayers = new Hitbox.UprightCylinderHitbox(loc, 5, mCageRadius).getHitPlayers(true);
						for (Player p : affectedPlayers) {
							PlayerUtils.healPlayer(mPlugin, p, EntityUtils.getMaxHealth(p) * mCageHeal, mPlayer);
						}
					}

					List<Integer> degreesToKeep = mCosmetic.cageTick(mPlayer, loc, mCageRadius, mDegrees1, mDegrees2, mDegrees3);

					mDegrees3 = new ArrayList<>(mDegrees2);
					mDegrees2 = new ArrayList<>(mDegrees1);
					mDegrees1 = new ArrayList<>(degreesToKeep);
				}
			}

			@Override
			public synchronized void cancel() {
				super.cancel();
				mCurrDuration = -1;
				ClientModHandler.updateAbility(mPlayer, GraspingClaws.this);
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int getInitialAbilityDuration() {
		return isEnhanced() ? CAGE_DURATION : 0;
	}

	@Override
	public int getRemainingAbilityDuration() {
		return this.mCurrDuration >= 0 && isEnhanced() ? getInitialAbilityDuration() - this.mCurrDuration : 0;
	}
}
