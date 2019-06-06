package com.playmonumenta.plugins.abilities.warrior;

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.abilities.Ability;
import com.playmonumenta.plugins.classes.Spells;
import com.playmonumenta.plugins.events.AbilityCastEvent;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MetadataUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class CounterStrike extends Ability {

	private static final float COUNTER_STRIKE_DISTANCE = 7;
	private static final float COUNTER_STRIKE_DOT_ANGLE = 0.33f;
	private static final int COUNTER_STRIKE_COOLDOWN = 20 * 8;
	private static final int COUNTER_STRIKE_ACTIVATION_PERIOD = 20 * 2;
	private static final int COUNTER_STRIKE_1_DAMAGE = 6;
	private static final int COUNTER_STRIKE_2_DAMAGE = 12;
	private static final float COUNTER_STRIKE_RADIUS = 5.0f;
	private static final double COUNTER_STRIKE_MELEE_THRESHOLD = 2;

	private BukkitRunnable mActivityTimer;
	private boolean mActive = false;

	public CounterStrike(Plugin plugin, World world, Random random, Player player) {
		super(plugin, world, random, player);
		mInfo.scoreboardId = "CounterStrike";
		mInfo.linkedSpell = Spells.COUNTER_STRIKE;
		mInfo.cooldown = COUNTER_STRIKE_COOLDOWN;
		mInfo.ignoreCooldown = true;
	}

	@Override
	public boolean PlayerDamagedByLivingEntityEvent(EntityDamageByEntityEvent event) {
		// Prevent ranged mob abilities from triggering Counter Strike
		if (event.getEntity().getBoundingBox().expand(COUNTER_STRIKE_MELEE_THRESHOLD).contains(mPlayer.getLocation().toVector())) {

			// Passive chance to damage nearby mobs
			if (mRandom.nextFloat() < 0.15f) {
				int counterStrike = getAbilityScore();
				Entity damager = event.getDamager();
				Vector dir = LocationUtils.getDirectionTo(mPlayer.getLocation().add(0, 1, 0), damager.getLocation().add(0, damager.getHeight() / 2, 0));
				Location loc = mPlayer.getLocation().add(0, 1, 0).subtract(dir);
				mWorld.spawnParticle(Particle.SWEEP_ATTACK, loc, 8, 0.75, 0.5, 0.75, 0.001);
				mWorld.spawnParticle(Particle.FIREWORKS_SPARK, loc, 20, 0.75, 0.5, 0.75, 0.1);
				mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1f, 0.7f);
				double csDamage = counterStrike == 1 ? COUNTER_STRIKE_1_DAMAGE : COUNTER_STRIKE_2_DAMAGE;
	
				for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), COUNTER_STRIKE_RADIUS, mPlayer)) {
					EntityUtils.damageEntity(mPlugin, mob, csDamage, mPlayer);
				}
			}
	
			// Active trigger if blocking, Riposte check is done separately through AbilityCastEvent
			if (!mPlugin.mTimers.isAbilityOnCooldown(mPlayer.getUniqueId(), Spells.COUNTER_STRIKE)) {
				if (mPlayer.isBlocking()) {
					mPlayer.spawnParticle(Particle.CRIT, mPlayer.getLocation(), 10, 0, 0, 0, 1);
					activate();
				}
			}
		}

		return true;
	}

	@Override
	public boolean LivingEntityDamagedByPlayerEvent(EntityDamageByEntityEvent event) {
		if (mActive && event.getCause() == DamageCause.ENTITY_ATTACK
		    && !MetadataUtils.happenedThisTick(mPlugin, mPlayer, EntityUtils.PLAYER_DEALT_CUSTOM_DAMAGE_METAKEY, 0)) {
			Location loc = mPlayer.getLocation().add(mPlayer.getLocation().getDirection().multiply(0.5));
			mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mPlayer.getLocation(), 25, 0, 0, 0, 0.15);
			new BukkitRunnable() {
				double d = 30;
				@Override
				public void run() {
					Vector vec;
					for (double r = 1; r < 5; r += 0.5) {
						for (double degree = d; degree <= d + 60; degree += 8) {
							double radian1 = Math.toRadians(degree);
							vec = new Vector(Math.cos(radian1) * r, 0.75, Math.sin(radian1) * r);
							vec = VectorUtils.rotateZAxis(vec, 20);
							vec = VectorUtils.rotateXAxis(vec, -loc.getPitch() + 20);
							vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

							Location l = loc.clone().add(vec);
							mWorld.spawnParticle(Particle.CRIT, l, 1, 0.1, 0.1, 0.1, 0.025);
							mWorld.spawnParticle(Particle.CRIT_MAGIC, l, 1, 0.1, 0.1, 0.1, 0.025);
						}
					}
					d += 60;
					if (d >= 150) {
						this.cancel();
					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1f, 0.7f);
			mPlayer.playSound(mPlayer.getLocation(), Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1f, 1.25f);
			LivingEntity damagee = (LivingEntity) event.getEntity();
			int damage = getAbilityScore() == 1 ? COUNTER_STRIKE_1_DAMAGE : COUNTER_STRIKE_2_DAMAGE;

			event.setDamage(event.getDamage() + damage);

			Vector playerDir = mPlayer.getEyeLocation().getDirection().setY(0).normalize();
			for (LivingEntity mob : EntityUtils.getNearbyMobs(mPlayer.getLocation(), COUNTER_STRIKE_DISTANCE, mPlayer)) {
				if (mob != damagee) {
					Vector toMobVector = mob.getLocation().toVector().subtract(mPlayer.getLocation().toVector()).setY(0).normalize();
					if (playerDir.dot(toMobVector) > COUNTER_STRIKE_DOT_ANGLE) {
						EntityUtils.damageEntity(mPlugin, mob, damage, mPlayer);
					}
				}
			}
			mActive = false;
			putOnCooldown();
		}

		return true;
	}

	@Override
	public boolean AbilityCastEvent(AbilityCastEvent event) {
		if (event.getAbility() == Spells.RIPOSTE) {
			activate();
		}

		return true;
	}

	private void activate() {
		mActive = true;
		// Prevent multiple activity timers from overwriting each other
		if (mActivityTimer != null && !mActivityTimer.isCancelled()) {
			mActivityTimer.cancel();
		}
		mActivityTimer = new BukkitRunnable() {
			@Override
			public void run() {
				mActive = false;
				this.cancel();
			}
		};
		mActivityTimer.runTaskLater(mPlugin, COUNTER_STRIKE_ACTIVATION_PERIOD);
	}

}
