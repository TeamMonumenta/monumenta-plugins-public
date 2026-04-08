package com.playmonumenta.plugins.abilities.warlock;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.abilities.AbilityWithDuration;
import com.playmonumenta.plugins.abilities.Description;
import com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.warlock.GraspingClawsCS;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.itemstats.enchantments.Grappling;
import com.playmonumenta.plugins.network.ClientModHandler;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import com.playmonumenta.plugins.utils.ItemUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrowableProjectile;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.Nullable;

import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.cooldown;
import static com.playmonumenta.plugins.abilities.FormattedDescriptionBuilder.StatValue.stat;
import static com.playmonumenta.plugins.utils.DescriptionUtils.UNDERLINED;

public class GraspingClaws extends Ability implements AbilityWithDuration {

	private static final int PULL_RADIUS = 8;
	private static final float PULL_SPEED = 0.175f;
	private static final double AMPLIFIER_1 = 0.2;
	private static final double AMPLIFIER_2 = 0.3;
	private static final int PULL_DAMAGE = 3;
	private static final int CLEAVE_FLAT_DAMAGE = 4;
	private static final double CLEAVE_PERCENT_DAMAGE = 0.3;
	private static final int CLEAVE_RADIUS = 3;
	private static final int CLEAVE_WINDOW = 4 * 20;
	private static final int DURATION = 8 * 20;
	private static final int COOLDOWN = 16 * 20;
	private static final int CAGE_RADIUS = 6;
	private static final int CAGE_DURATION = 6 * 20;
	private static final double HEAL_AMOUNT = 0.05;
	private static final int CAGE_DELAY = 20;

	public static final String CHARM_COOLDOWN = "Grasping Claws Cooldown";
	public static final String CHARM_PROJ_SPEED = "Grasping Claws Projectile Speed";
	public static final String CHARM_PULL_STRENGTH = "Grasping Claws Pull Strength";
	public static final String CHARM_PULL_DAMAGE = "Grasping Claws Pull Damage";
	public static final String CHARM_PULL_RADIUS = "Grasping Claws Pull Radius";
	public static final String CHARM_SLOW = "Grasping Claws Slowness Amplifier";
	public static final String CHARM_SLOW_DURATION = "Grasping Claws Slowness Duration";
	public static final String CHARM_CLEAVE_FLAT_DAMAGE = "Grasping Claws Cleave Flat Damage";
	public static final String CHARM_CLEAVE_PERCENT_DAMAGE = "Grasping Claws Cleave Percent Damage";
	public static final String CHARM_CLEAVE_RADIUS = "Grasping Claws Cleave Radius";
	public static final String CHARM_CAGE_RADIUS = "Grasping Claws Cage Radius";
	public static final String CHARM_CAGE_HEALING = "Grasping Claws Cage Healing";
	public static final String CHARM_CAGE_DURATION = "Grasping Claws Cage Duration";

	public static final AbilityInfo<GraspingClaws> INFO =
		new AbilityInfo<>(GraspingClaws.class, "Grasping Claws", GraspingClaws::new)
			.linkedSpell(ClassAbility.GRASPING_CLAWS)
			.scoreboardId("GraspingClaws")
			.shorthandName("GC")
			.descriptions(getDescription1(), getDescription2(), getDescriptionEnhancement())
			.simpleDescription("Fire a projectile that damages, pulls, and slows mobs.")
			.cooldown(COOLDOWN, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", GraspingClaws::cast, new AbilityTrigger(AbilityTrigger.Key.DROP).sneaking(true),
				new AbilityTriggerInfo.TriggerRestriction("holding a scythe or projectile weapon", player -> ItemUtils.isHoe(player.getInventory().getItemInMainHand()) || (ItemUtils.isProjectileWeapon(player.getInventory().getItemInMainHand()) && !Grappling.playerHoldingHook(player)))))
			.displayItem(Material.BOW);

	private final double mAmplifier;
	private final int mSlowDuration;
	private final double mPullDamage;
	private final double mPullRadius;
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
		mSlowDuration = CharmManager.getDuration(mPlayer, CHARM_SLOW_DURATION, DURATION);
		mPullDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_PULL_DAMAGE, PULL_DAMAGE);
		mPullRadius = CharmManager.getRadius(mPlayer, CHARM_PULL_RADIUS, PULL_RADIUS);
		mCleaveDamageFlat = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CLEAVE_FLAT_DAMAGE, CLEAVE_FLAT_DAMAGE);
		mCleaveDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_CLEAVE_PERCENT_DAMAGE, CLEAVE_PERCENT_DAMAGE);
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
		ThrowableProjectile proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, speed, mCosmetic.getProjectileName(), mCosmetic.getProjectileParticle(), LocationUtils.isLocationInWater(mPlayer.getLocation()));
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
			mCosmetic.onLand(mPlayer, world, loc, mPullRadius);

			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, mPullRadius, mPlayer)) {
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mPullDamage, true, true, false);
				MovementUtils.pullTowards(proj, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PULL_STRENGTH, PULL_SPEED));
				EntityUtils.applySlow(mPlugin, mSlowDuration, mAmplifier, mob);
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

			double damage = mCleaveDamageFlat + event.getDamage() * mCleaveDamagePercent;
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

			@Override
			public void run() {
				mT++;
				mCurrDuration++;

				mCosmetic.cageTick(mPlayer, loc, mCageRadius, mT);

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

				// Player Effect
				if (mT % 5 == 0) {
					if (mT % 20 == 0) {
						List<Player> affectedPlayers = new Hitbox.UprightCylinderHitbox(loc, 5, mCageRadius).getHitPlayers(true);
						for (Player p : affectedPlayers) {
							PlayerUtils.healPlayer(mPlugin, p, EntityUtils.getMaxHealth(p) * mCageHeal, mPlayer);
						}
					}
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

	private static Description<GraspingClaws> getDescription1() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 1)
			.addTrigger()
			.addDashedLine()
			.addLine("Fire a projectile that damages, slows, and")
			.addLine("pulls nearby mobs towards where it lands.")
			.addLine()
			.addStat("Damage: %d (s)")
				.statValues(stat(a -> a.mPullDamage, PULL_DAMAGE))
			.addStat("Effect: %p1 Slowness for %t")
				.statValues(stat(a -> a.mAmplifier, AMPLIFIER_1), stat(a -> a.mSlowDuration, DURATION))
			.addStat("Radius: %r")
				.statValues(stat(a -> a.mPullRadius, PULL_RADIUS))
			.addStat("Cooldown: %t")
				.statValues(cooldown(COOLDOWN))
			.addDashedLine();
	}

	private static Description<GraspingClaws> getDescription2() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 2)
			.addDashedLine()
			.addLine("Increase *Grasping Claws*'s slowness.").styles(UNDERLINED)
			.addLine()
			.addStatComparison("Effect: %p1 -> %p2 Slowness")
				.statValues(stat(AMPLIFIER_1), stat(a -> a.mAmplifier, AMPLIFIER_2))
			.addLine()
			.addLine("After casting *Grasping Claws*, your next attack").styles(UNDERLINED)
			.addLine("within %t will deal bonus magic damage (s)")
				.statValues(stat(CLEAVE_WINDOW))
			.addLine("to the target and other nearby mobs.")
			.addLine()
			.addStat("Cleave Damage: +%d + %p (s)")
				.statValues(stat(a -> a.mCleaveDamageFlat, CLEAVE_FLAT_DAMAGE), stat(a -> a.mCleaveDamagePercent, CLEAVE_PERCENT_DAMAGE))
			.tab().addLine("(of the attack's damage)")
			.addStat("Cleave Radius: %r")
				.statValues(stat(a -> a.mCleaveRadius, CLEAVE_RADIUS))
			.addDashedLine();
	}

	private static Description<GraspingClaws> getDescriptionEnhancement() {
		return new FormattedDescriptionBuilder<>(() -> INFO, 3)
			.addDashedLine()
			.addLine("The projectile now summons a cage around")
			.addLine("its landing location that lasts for %t.")
				.statValues(stat(a -> a.mCageDuration, CAGE_DURATION))
			.addLine()
			.addLine("Mobs cannot enter or exit the cage, and")
			.addLine("players inside the cage are healed.")
			.addLine("(Crowd control immune mobs are unaffected)")
			.addLine()
			.addStat("Healing: %p HP every 1s")
				.statValues(stat(a -> a.mCageHeal, HEAL_AMOUNT))
			.addStat("Cage Radius: %r")
				.statValues(stat(a -> a.mCageRadius, CAGE_RADIUS))
			.addDashedLine();
	}
}
