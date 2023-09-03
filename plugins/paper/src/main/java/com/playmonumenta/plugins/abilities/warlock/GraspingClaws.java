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

	private static final int RADIUS = 8;
	private static final float PULL_SPEED = 0.175f;
	private static final double AMPLIFIER_1 = 0.2;
	private static final double AMPLIFIER_2 = 0.3;
	private static final int DAMAGE_1 = 3;
	private static final int DAMAGE_2 = 8;
	private static final int DURATION = 8 * 20;
	private static final int COOLDOWN = 16 * 20;
	private static final int CAGE_RADIUS = 6;
	private static final int CAGE_DURATION = 6 * 20;
	private static final double HEAL_AMOUNT = 0.05;
	private static final int CAGE_DELAY = 1 * 20;

	public static final String CHARM_DAMAGE = "Grasping Claws Damage";
	public static final String CHARM_COOLDOWN = "Grasping Claws Cooldown";
	public static final String CHARM_PULL = "Grasping Claws Pull Strength";
	public static final String CHARM_SLOW = "Grasping Claws Slowness Amplifier";
	public static final String CHARM_RADIUS = "Grasping Claws Radius";
	public static final String CHARM_DURATION = "Grasping Claws Slowness Duration";
	public static final String CHARM_PROJ_SPEED = "Grasping Claws Projectile Speed";
	public static final String CHARM_CAGE_RADIUS = "Grasping Claws Cage Radius";

	public static final AbilityInfo<GraspingClaws> INFO =
			new AbilityInfo<>(GraspingClaws.class, "Grasping Claws", GraspingClaws::new)
					.linkedSpell(ClassAbility.GRASPING_CLAWS)
					.scoreboardId("GraspingClaws")
					.shorthandName("GC")
					.descriptions(
							("Pressing the drop key while sneaking and holding a scythe or projectile weapon fires a projectile " +
									"that pulls nearby enemies towards it once it makes contact with a mob or block. " +
									"Mobs caught in the projectile's %s block radius are given %s%% Slowness for %s seconds and take %s magic damage. Cooldown: %ss.")
									.formatted(RADIUS, StringUtils.multiplierToPercentage(AMPLIFIER_1), StringUtils.ticksToSeconds(DURATION), DAMAGE_1, StringUtils.ticksToSeconds(COOLDOWN)),
							"The pulled enemies now take %s damage, and their Slowness is increased to %s%%."
									.formatted(DAMAGE_2, StringUtils.multiplierToPercentage(AMPLIFIER_2)),
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
	private final double mDamage;
	private final Map<Projectile, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap = new WeakHashMap<>();

	private int mCurrDuration = -1;

	private final GraspingClawsCS mCosmetic;

	public GraspingClaws(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		mAmplifier = CharmManager.getLevelPercentDecimal(player, CHARM_SLOW) + (isLevelOne() ? AMPLIFIER_1 : AMPLIFIER_2);
		mDamage = CharmManager.calculateFlatAndPercentValue(player, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new GraspingClawsCS());
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}
		World world = mPlayer.getWorld();
		double speed = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PROJ_SPEED, 1.5);
		Snowball proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, speed, mCosmetic.getProjectileName(), mCosmetic.getProjectileParticle());
		mPlayerItemStatsMap.put(proj, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));
		putOnCooldown();
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(proj);
		if (playerItemStats != null) {
			Location loc = proj.getLocation();
			World world = proj.getWorld();
			mCosmetic.onLand(mPlayer, world, loc);

			int duration = CharmManager.getDuration(mPlayer, CHARM_DURATION, DURATION);
			for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS), mPlayer)) {
				DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageType.MAGIC, mInfo.getLinkedSpell(), playerItemStats), mDamage, true, true, false);
				MovementUtils.pullTowards(proj, mob, (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PULL, PULL_SPEED));
				EntityUtils.applySlow(mPlugin, duration, mAmplifier, mob);
			}

			if (isEnhanced()) {
				Bukkit.getScheduler().runTaskLater(mPlugin, () -> createCage(loc), CAGE_DELAY);
			}

			proj.remove();
		}
	}

	private void createCage(Location loc) {
		World world = loc.getWorld();
		mCosmetic.onCageCreation(world, loc);
		double radius = CharmManager.getRadius(mPlayer, CHARM_CAGE_RADIUS, CAGE_RADIUS);

		mCurrDuration = 0;
		ClientModHandler.updateAbility(mPlayer, this);

		new BukkitRunnable() {
			int mT = 0;
			final Hitbox mHitbox = Hitbox.approximateHollowCylinderSegment(loc, 5, radius - 0.6, radius + 0.6, Math.PI);
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
						Vector vec = new Vector(FastUtils.cos(radian1) * radius, 0, FastUtils.sin(radian1) * radius);
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
							if (loc.distance(eLoc) > radius) {
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
				if (mT >= CAGE_DURATION) {
					this.cancel();
				}

				// Player Effect + Outline Particles
				if (mT % 5 == 0) {
					if (mT % 20 == 0) {
						List<Player> affectedPlayers = new Hitbox.UprightCylinderHitbox(loc, 5, radius).getHitPlayers(true);
						for (Player p : affectedPlayers) {
							PlayerUtils.healPlayer(mPlugin, p, EntityUtils.getMaxHealth(p) * HEAL_AMOUNT, mPlayer);
						}
					}

					List<Integer> degreesToKeep = mCosmetic.cageTick(mPlayer, loc, radius, mDegrees1, mDegrees2, mDegrees3);

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
