package com.playmonumenta.plugins.abilities.shaman.soothsayer;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.AbilityTrigger;
import com.playmonumenta.plugins.abilities.AbilityTriggerInfo;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.particle.PPCircle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class EyeOfTheStorm extends Ability {
	public static final int COOLDOWN_1 = 18 * 20;
	public static final int COOLDOWN_2 = 15 * 20;
	public static final int RING_DURATION = 6 * 20;
	public static final int RADIUS = 5;
	public static final int DAMAGE_1 = 3;
	public static final int DAMAGE_2 = 5;
	public static final double PULL_STRENGTH = 0.15;
	private static final double VELOCITY = 2;

	public static final String CHARM_COOLDOWN = "Eye of the Storm Cooldown";
	public static final String CHARM_DAMAGE = "Eye of the Storm Damage";
	public static final String CHARM_RADIUS = "Eye of the Storm Radius";
	public static final String CHARM_DURATION = "Eye of the Storm Duration";
	public static final String CHARM_PULL = "Eye of the Storm Pull";

	public static final AbilityInfo<EyeOfTheStorm> INFO =
		new AbilityInfo<>(EyeOfTheStorm.class, "Eye of the Storm", EyeOfTheStorm::new)
			.linkedSpell(ClassAbility.EYE_OF_THE_STORM)
			.scoreboardId("EyeoftheStorm")
			.shorthandName("EOTS")
			.descriptions(
				String.format("Punch while sneaking with a projectile weapon to summon a %s block radius circle that lasts %ss and deals %s magic damage to all mobs " +
						"in it every second (goes through iframes), pulling them towards the center (%ss cooldown)",
					RADIUS,
					StringUtils.ticksToSeconds(RING_DURATION),
					DAMAGE_1,
					StringUtils.ticksToSeconds(COOLDOWN_1)
				),
				String.format("Magic damage increased to %s and cooldown reduced to %ss.",
					DAMAGE_2,
					StringUtils.ticksToSeconds(COOLDOWN_2))
			)
			.simpleDescription("Summon a medium sized ring which deals damage to mobs within and pulls them towards its center.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.addTrigger(new AbilityTriggerInfo<>("cast", "cast", EyeOfTheStorm::cast, new AbilityTrigger(AbilityTrigger.Key.LEFT_CLICK).sneaking(true)
				.keyOptions(AbilityTrigger.KeyOptions.REQUIRE_PROJECTILE_WEAPON)))
			.displayItem(Material.WHITE_CANDLE);

	public double mDamage;
	public final double mRadius;
	public final int mDuration;
	public final float mPullStrength;
	private final Map<Snowball, ItemStatManager.PlayerItemStats> mProjectiles = new WeakHashMap<>();

	public EyeOfTheStorm(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamage *= SupportExpertise.damageBuff(mPlayer);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mDuration = CharmManager.getDuration(mPlayer, CHARM_DURATION, RING_DURATION);
		mPullStrength = (float) CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_PULL, PULL_STRENGTH);
	}

	public void cast() {
		if (isOnCooldown()) {
			return;
		}

		World world = mPlayer.getWorld();
		Location loc = mPlayer.getLocation();
		world.playSound(loc, Sound.ENTITY_HORSE_BREATHE, 1.0f, 0.25f);
		Snowball proj = AbilityUtils.spawnAbilitySnowball(mPlugin, mPlayer, world, VELOCITY, "Eye of the Storm Projectile", Particle.CLOUD);

		ItemStatManager.PlayerItemStats playerItemStats = mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer);

		mProjectiles.put(proj, playerItemStats);
		putOnCooldown();

		// Clear out list just in case
		mProjectiles.keySet().removeIf(p -> p.isDead() || !p.isValid() || p.getTicksLived() >= 100);
	}

	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (!(proj instanceof Snowball)) {
			return;
		}
		ItemStatManager.PlayerItemStats stats = mProjectiles.remove(proj);
		if (stats != null) {
			ring(proj.getLocation(), stats);
		}
	}

	private void ring(Location loc, ItemStatManager.PlayerItemStats stats) {
		loc.getWorld().playSound(loc, Sound.BLOCK_END_PORTAL_SPAWN, 0.2f, 1.4f);
		loc.getWorld().playSound(loc, Sound.BLOCK_PORTAL_AMBIENT, 0.4f, 0.3f);
		new BukkitRunnable() {
			int mTicks = 0;

			@Override
			public void run() {

				PPCircle lowerRing = new PPCircle(Particle.DRAGON_BREATH, loc.clone().add(0, 0.5, 0), mRadius).countPerMeter(0.25).delta(0).extra(0);
				lowerRing.spawnAsPlayerActive(mPlayer);
				PPCircle higherRing = new PPCircle(Particle.GLOW, loc.clone().add(0, 1, 0), mRadius).countPerMeter(0.25).delta(0).extra(0);
				higherRing.spawnAsPlayerActive(mPlayer);

				if (mTicks % 20 == 0) {
					List<LivingEntity> affectedMobs = EntityUtils.getNearbyMobsInCylinder(loc, mRadius, 3, null);
					if (!affectedMobs.isEmpty()) {
						loc.getWorld().playSound(loc, Sound.ENTITY_PLAYER_HURT_FREEZE, 0.6f, 0.1f);
						loc.getWorld().playSound(loc, Sound.ENTITY_ENDERMAN_AMBIENT, 0.3f, 0.6f);
						for (LivingEntity mob : affectedMobs) {
							DamageUtils.damage(mPlayer, mob, new DamageEvent.Metadata(DamageEvent.DamageType.MAGIC, mInfo.getLinkedSpell(), stats), mDamage, true, false, false);
							mob.getWorld().playSound(mob.getLocation(), Sound.ENTITY_CAT_HISS, 2.0f, 1.0f);
							Location pullTarget = loc.clone();
							pullTarget.setY(mob.getLocation().getY());
							MovementUtils.pullTowards(pullTarget, mob, mPullStrength);
						}
					}
				}
				if (mTicks >= mDuration) {
					this.cancel();
				}

				mTicks += 2;
			}
		}.runTaskTimer(mPlugin, 0, 2);

	}
}
