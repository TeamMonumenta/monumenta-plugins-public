package com.playmonumenta.plugins.bosses.spells.xenotopsis;

import com.playmonumenta.plugins.bosses.bosses.Xenotopsis;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PPParametric;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.Hitbox;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class DarkRetaliation extends Spell {

	// the cooldown between each time the boss can retaliate, in ticks
	private static final int COOLDOWN = 24;

	// the distance from which the attack will start triggering on players
	private static final double TRIGGER_DISTANCE = 8;

	// the percent of the damage to transfer back
	private static final double DAMAGE_TRANSFER_PERCENT = 0.5;

	// the speed of the projectile, in blocks per tick
	private static final double PROJECTILE_SPEED = 0.9;

	// the turn speed of the projectile, or speed of the projectile orthogonal to the velocity, in blocks per tick
	private static final double TURN_SPEED = 0.028;

	// the death damage of the attack
	private static final int DEATH_DAMAGE = 8;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;
	private final Xenotopsis mXenotopsis;

	private int mTicks = 0;

	public DarkRetaliation(Plugin plugin, LivingEntity boss, Xenotopsis xenotopsis) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();
		mXenotopsis = xenotopsis;
	}

	@Override
	public void run() {
		if (mTicks > 0) {
			mTicks -= 5;
		}
		if (mTicks < 0) {
			mTicks = 0;
		}
	}

	@Override
	public void onHurt(DamageEvent event) {
		super.onHurt(event);

		// check if requirements for retaliation are met
		if (mTicks == 0 && event.getDamager() != null && event.getDamager() instanceof Player player && mBoss.getLocation().distance(player.getLocation()) > TRIGGER_DISTANCE) {
			attackPlayer(player, event.getDamage() * DAMAGE_TRANSFER_PERCENT);
			mTicks = COOLDOWN;
		}
	}

	private void attackPlayer(Player player, double damage) {
		Vector dirToPlayer = player.getLocation().clone().toVector().add(new Vector(0, 1.5, 0)).subtract(mBoss.getLocation().clone().toVector()).normalize();

		Vector crossXZ = dirToPlayer.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		Vector crossY = dirToPlayer.clone().crossProduct(crossXZ).normalize();

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_REPAIR, SoundCategory.HOSTILE, 3f, 1.4f);

		// crit particle effect
		new PPParametric(Particle.CRIT, mBoss.getLocation().clone().add(dirToPlayer.clone().multiply(1.2)).add(0, 1.5, 0), (parameter, builder) -> {
			double angle = parameter * Math.PI * 2;

			Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(1.2);

			builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
		}).count(50).directionalMode(true).extra(1).spawnAsBoss();
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			new PPParametric(Particle.CRIT, mBoss.getLocation().clone().add(dirToPlayer.clone().multiply(1.35)).add(0, 1.5, 0), (parameter, builder) -> {
				double angle = parameter * Math.PI * 2;

				Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(0.8);

				builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
			}).count(40).directionalMode(true).extra(1).spawnAsBoss();
		}, 4);
		Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
			new PPParametric(Particle.CRIT, mBoss.getLocation().clone().add(dirToPlayer.clone().multiply(1.5)).add(0, 1.5, 0), (parameter, builder) -> {
				double angle = parameter * Math.PI * 2;

				Vector launchDir = crossXZ.clone().multiply(FastUtils.cos(angle)).add(crossY.clone().multiply(FastUtils.sin(angle))).multiply(0.4);

				builder.offset(launchDir.getX(), launchDir.getY(), launchDir.getZ());
			}).count(30).directionalMode(true).extra(1).spawnAsBoss();
		}, 8);

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			final Location mLocation = mBoss.getLocation().clone().add(0, 1.7, 0);
			final Vector mToPlayer = dirToPlayer.clone().multiply(PROJECTILE_SPEED);

			@Override
			public void run() {
				if (mTicks == 0) {
					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ALLAY_HURT, SoundCategory.HOSTILE, 3f, 0.67f);
				}

				// move the projectile
				Vector angleDelta = mToPlayer.clone().crossProduct(mToPlayer.clone().crossProduct(player.getLocation().clone().toVector().add(new Vector(0, 1.5, 0)).subtract(mLocation.clone().toVector()))).normalize().multiply(TURN_SPEED);
				mToPlayer.add(angleDelta.multiply(-1)).normalize().multiply(PROJECTILE_SPEED);
				mLocation.add(mToPlayer);

				// check hitboxes
				Hitbox hitbox = new Hitbox.AABBHitbox(mWorld, BoundingBox.of(mLocation, 0.25, 0.25, 0.25));
				if (hitbox.getHitPlayers(true).contains(player)) {
					new PartialParticle(Particle.SMOKE_LARGE, player.getLocation().clone().add(0, 0.1, 0)).extra(0).delta(0.4, 0, 0.4).count(40).spawnAsBoss();

					DamageUtils.damage(mBoss, player, DamageEvent.DamageType.PROJECTILE, mXenotopsis.scaleDamage(damage), null, false, false, "Dark Retaliation");
					mXenotopsis.changePlayerDeathValue(player, DEATH_DAMAGE, false);

					this.cancel();
				}

				new PartialParticle(Particle.SQUID_INK, mLocation).delta(0.1).extra(0).count(3).spawnAsBoss();
				new PartialParticle(Particle.SMOKE_LARGE, mLocation).delta(0.1).extra(0).count(10).spawnAsBoss();

				mTicks++;
				if (mTicks > 60 || mBoss.isDead()) {
					this.cancel();
				}
			}
		};
		mActiveRunnables.add(runnable);
		runnable.runTaskTimer(mPlugin, 12, 1);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
