package com.playmonumenta.plugins.abilities.shaman;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.abilities.AbilityInfo;
import com.playmonumenta.plugins.abilities.shaman.hexbreaker.HexbreakerPassive;
import com.playmonumenta.plugins.abilities.shaman.soothsayer.SoothsayerPassive;
import com.playmonumenta.plugins.classes.ClassAbility;
import com.playmonumenta.plugins.classes.Shaman;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.itemstats.ItemStatManager;
import com.playmonumenta.plugins.listeners.AuditListener;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.WeakHashMap;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.entity.AbstractArrow.PickupStatus;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class EarthenTremor extends Ability {
	public static final String ABILITY_NAME = "Earthen Tremor";
	public static final int COOLDOWN = 24 * 20;
	public static final int EARTHQUAKE_TIME = 20;
	public static final int RADIUS = 6;
	public static final double KNOCKBACK = 0.8;
	public static final int DAMAGE_1 = 6;
	public static final int DAMAGE_2 = 10;

	public double mDamage;
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
					EARTHQUAKE_TIME / 20,
					COOLDOWN / 20
				),
				String.format("Magic damage dealt is boosted to %s.",
					DAMAGE_2)
			)
			.simpleDescription("Fires an arrow that will cause a tremor where it lands, dealing damage and knocking up yourself and mobs.")
			.cooldown(COOLDOWN)
			.displayItem(Material.DIRT);

	public EarthenTremor(Plugin plugin, Player player) {
		super(plugin, player, INFO);
		if (!player.hasPermission(Shaman.PERMISSION_STRING)) {
			AuditListener.logSevere(player.getName() + " has accessed shaman abilities incorrectly, class has been reset, please report to developers.");
			AbilityUtils.resetClass(player);
		}
		mPlayerItemStatsMap = new WeakHashMap<>();
		mDamage = isLevelOne() ? DAMAGE_1 : DAMAGE_2;
		mDamage *= HexbreakerPassive.damageBuff(mPlayer);
		mDamage *= SoothsayerPassive.damageBuff(mPlayer);
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

		ItemStatManager.PlayerItemStats playerItemStats = mPlayerItemStatsMap.remove(projectile);
		if (playerItemStats != null) {
			new BukkitRunnable() {
				int mTicks = 0;

				@Override
				public void run() {
					if (mTicks >= EARTHQUAKE_TIME) {
						for (LivingEntity mob : EntityUtils.getNearbyMobs(loc, RADIUS)) {
							if (!EntityUtils.isCCImmuneMob(mob) && !EntityUtils.isBoss(mob)) {
								knockup(mob);
							}
							DamageUtils.damage(mPlayer, mob, DamageType.MAGIC, mDamage, mInfo.getLinkedSpell());
						}
						for (Player player : PlayerUtils.playersInRange(loc, RADIUS, true)) {
							if (player == mPlayer && player.getGameMode() == GameMode.SURVIVAL && !player.getScoreboardTags().contains("NoTremorKnockup")) {
								knockup(player);
							}
						}

						new PartialParticle(Particle.CAMPFIRE_COSY_SMOKE, loc, 30, RADIUS / 2.0, 0.1, RADIUS / 2.0, 0.1).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.LAVA, loc, 20, RADIUS / 2.0, 0.3, RADIUS / 2.0, 0.1).spawnAsPlayerActive(mPlayer);
						world.playSound(loc, Sound.BLOCK_CAMPFIRE_CRACKLE, 3, 1.0f);
						world.playSound(loc, Sound.BLOCK_ANVIL_PLACE, 1, 1.0f);
						world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.75f, 1.0f);
						this.cancel();
					} else {
						new PartialParticle(Particle.BLOCK_CRACK, loc, 30, RADIUS / 2.0, 0.25, RADIUS / 2.0, 0.1, Bukkit.createBlockData(Material.PODZOL)).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.BLOCK_CRACK, loc, 30, RADIUS / 2.0, 0.25, RADIUS / 2.0, 0.1, Bukkit.createBlockData(Material.GRANITE)).spawnAsPlayerActive(mPlayer);
						new PartialParticle(Particle.BLOCK_CRACK, loc, 30, RADIUS / 2.0, 0.25, RADIUS / 2.0, 0.1, Bukkit.createBlockData(Material.IRON_ORE)).spawnAsPlayerActive(mPlayer);
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
		le.setVelocity(le.getVelocity().add(new Vector(0.0, KNOCKBACK, 0.0)));
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
				if (mT > COOLDOWN || !mPlayerItemStatsMap.containsKey(projectile)) {
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
