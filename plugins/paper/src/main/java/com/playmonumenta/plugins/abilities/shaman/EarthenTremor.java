package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.DestructiveExpertise;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SupportExpertise;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.itemstats.abilities.CharmManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.StringUtils;
import com.playmonumenta.plugins.utils.ZoneUtils;
import java.util.List;
import java.util.WeakHashMap;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.AbstractArrow;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.Snowball;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class EarthenTremor extends Ability {
	public static final String ABILITY_NAME = "Earthen Tremor";
	public static final int COOLDOWN_1 = 24 * 20;
	public static final int COOLDOWN_2 = 16 * 20;
	public static final int DELAY = 20;
	public static final int RADIUS = 6;
	public static final double KNOCKBACK = 0.8;
	public static final int DAMAGE_1 = 9;
	public static final int DAMAGE_2 = 12;

	public static String CHARM_COOLDOWN = "Earthen Tremor Cooldown";
	public static String CHARM_DAMAGE = "Earthen Tremor Damage";
	public static String CHARM_RADIUS = "Earthen Tremor Radius";
	public static String CHARM_DELAY = "Earthen Tremor Delay";
	public static String CHARM_KNOCKBACK = "Earthen Tremor Knockback";

	public int mCooldown;
	public double mDamage;
	public int mDelay;
	public double mRadius;
	public double mKnockback;
	private final WeakHashMap<Projectile, ItemStatManager.PlayerItemStats> mPlayerItemStatsMap;

	public static final AbilityInfo<EarthenTremor> INFO =
		new AbilityInfo<>(EarthenTremor.class, "Earthen Tremor", EarthenTremor::new)
			.linkedSpell(ClassAbility.EARTHEN_TREMOR)
			.scoreboardId("EarthenTremor")
			.shorthandName("ET")
			.descriptions(
				String.format("Shoot a bow while sneaking to trigger a tremor where the arrow lands, dealing %s magic damage to mobs within %s blocks of the landing location " +
						"after %s second and throwing them and yourself (if within range) into the air. Cooldown: %ss.",
					DAMAGE_1,
					RADIUS,
					StringUtils.ticksToSeconds(DELAY),
					StringUtils.ticksToSeconds(COOLDOWN_1)
				),
				String.format("Magic damage dealt is boosted to %s and cooldown is reduced to %ss.",
					DAMAGE_2,
					StringUtils.ticksToSeconds(COOLDOWN_2))
			)
			.simpleDescription("Fires an arrow that will cause a tremor where it lands, dealing damage and knocking up yourself and mobs.")
			.cooldown(COOLDOWN_1, COOLDOWN_2, CHARM_COOLDOWN)
			.displayItem(Material.DIRT);

	public EarthenTremor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AbilityUtils.resetClass(player);
		}
		mPlayerItemStatsMap = new WeakHashMap<>();
		mDamage = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_DAMAGE, isLevelOne() ? DAMAGE_1 : DAMAGE_2);
		mDamage *= DestructiveExpertise.damageBuff(mPlayer);
		mDamage *= SupportExpertise.damageBuff(mPlayer);
		mCooldown = isLevelOne() ? COOLDOWN_1 : COOLDOWN_2;

		mDelay = CharmManager.getDuration(mPlayer, CHARM_DELAY, DELAY);
		mRadius = CharmManager.getRadius(mPlayer, CHARM_RADIUS, RADIUS);
		mKnockback = CharmManager.calculateFlatAndPercentValue(mPlayer, CHARM_KNOCKBACK, KNOCKBACK);
	}

	@Override
	public boolean onDamage(DamageEvent event, LivingEntity enemy) {
		Entity damager = event.getDamager();
		if (event.getType() == DamageType.PROJECTILE && damager instanceof AbstractArrow arrow && mPlayerItemStatsMap.containsKey(damager)) {
			quake(arrow, enemy.getLocation());
		}
		return false; // prevents multiple calls by removing the arrow (from the world and the player stats map)
	}

	// Since Snowballs disappear after landing, we need an extra detection for when it hits the ground.
	@Override
	public void projectileHitEvent(ProjectileHitEvent event, Projectile proj) {
		if (proj instanceof Snowball && mPlayerItemStatsMap.containsKey(proj)) {
			quake(proj, proj.getLocation());
		}
	}

	private void quake(Projectile projectile, Location loc) {
		World world = mPlayer.getWorld();

		double particleScale = (mRadius * mRadius) / (RADIUS * RADIUS);

		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(projectile);
		if (playerItemStats != null) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks >= mDelay) {
						for (LivingEntity mob : EntityUtils.getNearbyMobsInSphere(loc, RADIUS, null)) {
							if (!EntityUtils.isCCImmuneMob(mob) && !EntityUtils.isBoss(mob)) {
								knockup(mob);
							}
							DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell());
						}

						if (!ZoneUtils.hasZoneProperty(mPlayer, ZoneUtils.ZoneProperty.NO_MOBILITY_ABILITIES) && !mPlayer.getScoreboardTags().contains("NoTremorKnockup") && mPlayer.getLocation().distance(loc) <= RADIUS) {
							knockup(mPlayer);
						}

						new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, (int) (6 * particleScale), mRadius / 2, 0.1, mRadius / 2, 0.1).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.LAVA, loc, (int) (20 * particleScale), mRadius / 2, 0.3, mRadius / 2, 0.1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 3, 1.0f);
						world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 1, 1.0f);
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.75f, 1.0f);
						this.cancel();
					} else {
						for (Material mat : List.of(Material.PODZOL, Material.GRANITE, Material.IRON_ORE)) {
							new PartialParticle(Particle.BLOCK_CRACK, loc, (int) (30 * particleScale), mRadius / 2, 0.25, mRadius / 2, 0.1, mat.createBlockData()).spawnAsPlayerActive(mPlayer);
						}
						world.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 2, 1.0f);
						world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 0.75f, 0.5f);
					}

					mTicks += 5;
				}
			}.runTaskTimer(mPlugin, 0, 5);
		}

		projectile.remove();
	}

	private void knockup(LivingEntity le) {
		le.setVelocity(le.getVelocity().add(new Vector(0.0, mKnockback, 0.0)));
	}

	@Override
	public boolean playerShotProjectileEvent(Projectile projectile) {
		if (isOnCooldown()
			|| !mPlayer.isSneaking()
			|| !EntityUtils.isAbilityTriggeringProjectile(projectile, false)) {
			return true;
		}
		putOnCooldown();
		World world = mPlayer.getWorld();
		world.playSound(mPlayer.getLocation(), Sound.BLOCK_CAMPFIRE_CRACKLE, 2, 1.0f);

		if (projectile instanceof AbstractArrow arrow) {
			arrow.setPierceLevel(0);
			arrow.setCritical(true);
			arrow.setPickupStatus(PickupStatus.CREATIVE_ONLY);
		}

		mPlayerItemStatsMap.put(projectile, mPlugin.mItemStatManager.getPlayerItemStatsCopy(mPlayer));

		mPlugin.mProjectileEffectTimers.addEntity(projectile, Particle.LAVA);

		new BukkitRunnable() {
			int mT = 0;
			Location mLastLoc = mPlayer.getLocation();

			@Override
			public void run() {
				if (mT > mCooldown || !mPlayerItemStatsMap.containsKey(projectile)) {
					projectile.remove();

					this.cancel();
					return;
				}

				if (!projectile.isDead()) {
					mLastLoc = projectile.getLocation();
				} else {
					quake(projectile, mLastLoc);
					this.cancel();
					return;
				}

				if (projectile.getVelocity().length() < .05 || projectile.isOnGround()) {
					quake(projectile, projectile.getLocation());

					this.cancel();
					return;
				}
				mT++;
			}

		}.runTaskTimer(mPlugin, 0, 1);

		return true;
	}
}
