package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.cosmetics.skills.CosmeticSkills;
import com.playmonumenta.plugins.cosmetics.skills.shaman.TotemicProjectionCS;
import com.playmonumenta.plugins.effects.PercentDamageDealt;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.*;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class TotemicProjection extends Ability {

	private static final int COOLDOWN_1 = 6 * 20;
	private static final int COOLDOWN_2 = 4 * 20;
	private static final double SLOWNESS_PERCENT = 0.2;
	private static final int SLOWNESS_DURATION = 3 * 20;
	private static final int RADIUS = 6;
	private static final double VELOCITY = 1.25;
	private static final double TIME_TO_DROP = 75;
	private static final double XZ_DISTANCE_TO_DROP = 14;
	private static final int DISTRIBUTION_RADIUS = 3;
	private static final double ENHANCE_DAMAGE_PERCENT_PER = 0.15;
	private static final int ENHANCE_DAMAGE_PERCENT_DURATION = 6 * 20;

	public static final String CHARM_SLOWNESS_PERCENT = "Totemic Projection Slowness Amplifier";
	public static final String CHARM_SLOWNESS_DURATION = "Totemic Projection Slowness Duration";

	public static final String CHARM_COOLDOWN = "Totemic Projection Cooldown";
	public static final String CHARM_DAMAGE_RADIUS = "Totemic Projection Damage Radius";
	public static final String CHARM_DISTANCE = "Totemic Projection Spread";
	public static final String CHARM_ENHANCE_DAMAGE_PERCENT_PER = "Totemic Projection Enhancement Damage";
	public static final String CHARM_ENHANCE_DAMAGE_DURATION = "Totemic Projection Enhancement Damage Duration";

	public static final AbilityInfo<TotemicProjection> INFO =
		new AbilityInfo<>(TotemicProjection.class, "Totemic Projection", TotemicProjection::new)
			.linkedSpell(ClassAbility.TOTEMIC_PROJECTION)
			.scoreboardId("TotemicProjection")
			.shorthandName("TP")
			.descriptions(
				String.format("Press swap with a projectile weapon to fire a projectile that, on landing, moves all active totems to within %s blocks of it. %ss cooldown.",
					DISTRIBUTION_RADIUS,
					StringUtils.ticksToSeconds(COOLDOWN_1)
				),
				String.format("Slow mobs near the projectile landing spot by %s%% for %ss within a %s block radius on hit. %ss cooldown.",
					StringUtils.multiplierToPercentage(SLOWNESS_PERCENT),
					StringUtils.ticksToSeconds(SLOWNESS_DURATION),
					RADIUS,
					StringUtils.ticksToSeconds(COOLDOWN_2)),
				String.format("For every totem affected by this ability, " +
					"the Shaman gets +%s%% Attack/Projectile damage for %ss.",
					StringUtils.multiplierToPercentage(ENHANCE_DAMAGE_PERCENT_PER),
					StringUtils.ticksToSeconds(ENHANCE_DAMAGE_PERCENT_DURATION))
			)
			.simpleDescription("Fires a projectile that summons all of your active totems around the landing location.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", TotemicProjection::cast, new AbilityTrigger(AbilityTrigger.Key.SWAP).sneaking(false)
				.keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.ENDER_PEARL);

	private final Map<Snowball, ItemStatManager.PlayerItemStats> mProjectiles = new WeakHashMap<>();
	private final double mSlownessPercent;
	private final int mSlownessDuration;
	private final double mEnhanceDamagePercent;
	private final int mEnhanceDamageDuration;
	private final double mDistributionRadius;
	private final double mRadius;
	private final TotemicProjectionCS mCosmetic;

	public TotemicProjection(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mSlownessPercent = isLevelTwo() ? SLOWNESS_PERCENT + CharmManager.getLevelPercentDecimal(mPlayer, CHARM_SLOWNESS_PERCENT) : 0;
		mSlownessDuration = CharmManager.getDuration(mPlayer, CHARM_SLOWNESS_DURATION, SLOWNESS_DURATION);
		mEnhanceDamageDuration = CharmManager.getDuration(mPlayer, CHARM_ENHANCE_DAMAGE_DURATION, ENHANCE_DAMAGE_PERCENT_DURATION);
		mEnhanceDamagePercent = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_ENHANCE_DAMAGE_PERCENT_PER, ENHANCE_DAMAGE_PERCENT_PER);
		mDistributionRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DISTANCE, DISTRIBUTION_RADIUS);
		mRadius = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE_RADIUS, RADIUS);
		mCosmetic = CosmeticSkills.getPlayerCosmeticSkill(player, new TotemicProjectionCS());
	}

	public boolean cast() {
		if (isOnCooldown()) {
			return false;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE,
			SoundCategory.PLAYERS, 1.0f, 0.25f);
		Snowball proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, VELOCITY, "Totemic Projection Projectile", Particle.CLOUD);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		mProjectiles.put(proj, playerItemStats);
		putOnCooldown();

		// Clear out list just in case
		mProjectiles.keySet().removeIf(p -> p.isDead() || !p.isValid());
		new BukkitRunnable() {
			int mT = 0;
			final Location mPlayerLocation = mPlayer.getLocation();
			@Override
			public void run() {
				if (mProjectiles.get(proj) != playerItemStats) {
					this.cancel();
				}

				Location projLoc = proj.getLocation();
				projLoc.setY(mPlayer.getLocation().getY());
				if (mT >= TIME_TO_DROP
					|| projLoc.distance(mPlayerLocation) >= XZ_DISTANCE_TO_DROP) {
					proj.setVelocity(new Vector(0, -2, 0));
				}

				if (proj.isDead()) {
					mProjectiles.remove(proj);
					this.cancel();
				}
				mT++;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (!(proj instanceof Snowball) || event.isCancelled()) {
			return;
		}
		ItemStatManager.PlayerItemStats stats = mProjectiles.remove(proj);

		if (stats != null) {
			Location dropCenter = proj.getLocation();
			mCosmetic.projectionCollision(mPlayer, dropCenter);

			List<LivingEntity> totems = TotemicEmpowerment.getTotemList(mPlayer);
			if (totems.size() == 1) {
				LivingEntity totem = totems.get(0);
				Location loc = dropCenter.clone().add(0, 0.5, 0);
				if (loc.getBlock().isPassable()) {
					totem.teleport(loc);
				} else {
					totem.teleport(dropCenter);
				}
			} else if (totems.size() > 1) {
				Vector forward = dropCenter.getDirection().setY(0).normalize().multiply(mDistributionRadius);
				int currentDeg;
				switch (totems.size()) {
					case 2 -> currentDeg = -90;
					case 3 -> currentDeg = -60;
					default -> currentDeg = -45;
				}
				int degIncrement = 360 / totems.size();
				for (LivingEntity totem : totems) {
					Vector dir = VectorUtils.rotateYAxis(forward, currentDeg);
					Location locLower = dropCenter.clone().add(dir);
					Location loc = locLower.clone().add(0, 0.5, 0);
					if (loc.getBlock().isPassable()) {
						totem.teleport(loc);
					} else if (locLower.getBlock().isPassable()) {
						totem.teleport(locLower);
					} else {
						totem.teleport(dropCenter);
					}
					currentDeg += degIncrement;
				}
			}
			if (isEnhanced()) {
				mPlugin.mEffectManager.addEffect(mPlayer, "ShamanProjectionEnhancement",
					new PercentDamageDealt(mEnhanceDamageDuration,
						mEnhanceDamagePercent * totems.size(),
						EnumSet.of(DamageEvent.DamageType.PROJECTILE, DamageEvent.DamageType.MELEE)));
			}

			if (isLevelTwo()) {
				mCosmetic.projectionAOE(mPlayer, dropCenter, mRadius);
				List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobsInSphere(dropCenter, mRadius, null);
				for (LivingEntity mob : affectedMobs) {
					EntityUtils.applySlow(mPlugin, mSlownessDuration, mSlownessPercent, mob);
				}
			}
		}
	}
}
