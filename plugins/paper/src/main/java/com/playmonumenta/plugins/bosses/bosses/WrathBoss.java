package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellBaseLeapAttack;
import com.playmonumenta.plugins.bosses.spells.SpellDuelist;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class WrathBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_wrath";
	public static final int detectionRange = 32;

	private static final int COOLDOWN = 20 * 3;
	private static final int MIN_RANGE = 8;
	private static final int RUN_DISTANCE = 2;
	private static final int VELOCITY_MULTIPLIER = 1;
	private static final double DAMAGE_RADIUS = 2.5;
	private static final int DAMAGE = 20;
	private static final int ULTIMATE_EYE_DISTANCE = 6;
	private static final double DODGE_CHANCE = 0.3;


	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new WrathBoss(plugin, boss);
	}

	public WrathBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellBaseLeapAttack(plugin, boss, detectionRange, MIN_RANGE, RUN_DISTANCE, COOLDOWN, VELOCITY_MULTIPLIER,
					// Initiate Aesthetic
					(World world, Location loc) -> {
						world.spawnParticle(Particle.VILLAGER_ANGRY, loc, 10, 0.5, 0.5, 0.5, 0);
						world.playSound(loc, Sound.ENTITY_VINDICATOR_HURT, 1f, 0.5f);
					},
					// Leap Aesthetic
					(World world, Location loc) -> {
						world.spawnParticle(Particle.CLOUD, loc, 20, 0.1, 0.1, 0.1, 0.1);
						world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 1f, 0.5f);
					},
					// Leaping Aesthetic
					(World world, Location loc) -> {
						world.spawnParticle(Particle.FLAME, loc, 3, 0, 0, 0, 0.1);
						world.spawnParticle(Particle.VILLAGER_ANGRY, loc, 1, 0.25, 0.25, 0.25, 0);
					},
					// Hit Action
					(World world, Player player, Location loc, Vector dir) -> {
						new BukkitRunnable() {
							World mWorld = world;
							Location mLocation = loc;
							Vector mDirection = dir;
							int mTime = 0;

							@Override
							public void run() {
								mTime++;

								if (mTime <= 5) {
									Location locParticle = mBoss.getLocation().add(0, 1.5, 0);
									Vector sideways = new Vector(mDirection.getZ(), 1, -mDirection.getX()).multiply(3);
									Vector forward = mDirection.clone().multiply(3);
									locParticle.subtract(sideways.clone().multiply(0.5));
									locParticle.subtract(forward.clone().multiply(0.5));
									locParticle.add(forward.clone().multiply(Math.sin(Math.PI / 10 * mTime)));
									locParticle.add(sideways.clone().multiply(Math.cos(Math.PI / 10 * mTime)));
									mWorld.spawnParticle(Particle.SWEEP_ATTACK, locParticle, 4, 0.5, 0.5, 0.5, 0);

									if (mTime == 2) {
										mWorld.spawnParticle(Particle.CRIT, mLocation, 100, 0, 0, 0, 0.5);
										mWorld.spawnParticle(Particle.CRIT_MAGIC, mLocation, 100, 2, 2, 2, 0);
										mWorld.playSound(mLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
										mWorld.playSound(mLocation, Sound.ITEM_SHIELD_BREAK, 1f, 1f);
										for (Player p : PlayerUtils.playersInRange(mLocation.add(mDirection), DAMAGE_RADIUS)) {
											BossUtils.bossDamage(mBoss, p, DAMAGE);
										}
									}
								} else if (mTime <= 10) {
									Location locParticle = mBoss.getLocation().add(0, 1.5, 0);
									Vector sideways = new Vector(-mDirection.getZ(), 1, mDirection.getX()).multiply(3);
									Vector forward = mDirection.clone().multiply(3);
									locParticle.subtract(sideways.clone().multiply(0.5));
									locParticle.subtract(forward.clone().multiply(0.5));
									locParticle.add(forward.clone().multiply(Math.sin(Math.PI / 10 * (mTime - 5))));
									locParticle.add(sideways.clone().multiply(Math.cos(Math.PI / 10 * (mTime - 5))));
									mWorld.spawnParticle(Particle.SWEEP_ATTACK, locParticle, 4, 0.5, 0.5, 0.5, 0);

									if (mTime == 7) {
										mWorld.spawnParticle(Particle.CRIT, mLocation, 200, 0, 0, 0, 1);
										mWorld.spawnParticle(Particle.CRIT_MAGIC, mLocation, 200, 2, 2, 2, 0);
										mWorld.playSound(mLocation, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 1f);
										mWorld.playSound(mLocation, Sound.ITEM_SHIELD_BREAK, 1f, 1f);
										for (Player p : PlayerUtils.playersInRange(mLocation.add(mDirection), DAMAGE_RADIUS)) {
											p.setNoDamageTicks(0);
											BossUtils.bossDamage(mBoss, p, DAMAGE);
										}
									}
								} else {
									this.cancel();
								}
							}
						}.runTaskTimer(mPlugin, 0, 1);
					}, null, null),
			new SpellDuelist(plugin, boss, COOLDOWN, DAMAGE)
		));

		super.constructBoss(plugin, identityTag, mBoss, activeSpells, null, detectionRange, null);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		Location loc = mBoss.getLocation();
		Entity damager = event.getDamager();
		if (damager instanceof Projectile) {
			ProjectileSource source = ((Projectile) damager).getShooter();
			if (source instanceof LivingEntity) {
				damager = (LivingEntity) source;
			}
		}

		if (damager instanceof LivingEntity) {
			if (loc.distance(damager.getLocation()) > ULTIMATE_EYE_DISTANCE) {
				dodge(event);
			} else {
				if (FastUtils.RANDOM.nextDouble() < DODGE_CHANCE) {
					dodge(event);
				}
			}
		}
	}

	private void dodge(EntityDamageByEntityEvent event) {
		event.setCancelled(true);
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation().add(0, 1, 0);
		Vector direction = event.getDamager().getLocation().subtract(loc).toVector().setY(0).normalize();
		Vector sideways = new Vector(direction.getZ(), 0, -direction.getX());
		sideways.subtract(direction.multiply(0.25));
		if (FastUtils.RANDOM.nextBoolean()) {
			sideways.multiply(-1);
		}

		loc.add(sideways.multiply(3));
		for (int i = 0; i < 3; i++) {
			if (loc.getBlock().isPassable()) {
				world.spawnParticle(Particle.SMOKE_LARGE, loc, 10, 0, 0, 0, 0.5);
				world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.5f);

				mBoss.teleport(loc);
				break;
			} else {
				loc.add(0, 1, 0);
			}
		}
	}
}
