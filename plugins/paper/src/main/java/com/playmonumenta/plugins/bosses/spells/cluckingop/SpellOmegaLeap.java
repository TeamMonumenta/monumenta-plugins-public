package com.playmonumenta.plugins.bosses.spells.cluckingop;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellOmegaLeap extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;

	public SpellOmegaLeap(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		mBoss.setVelocity(new Vector(0, 1.5, 0));
		world.spawnParticle(Particle.FLAME, mBoss.getLocation(), 175, 0, 0, 0, 0.175, null, true);
		world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation(), 75, 0, 0, 0, 0.25, null, true);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.9f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0f);
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				if (t >= 5 && mBoss.isOnGround() && !mBoss.isDead() && mBoss.isValid()) {
					this.cancel();
					Location loc = mBoss.getLocation();
					world.spawnParticle(Particle.EXPLOSION_NORMAL, loc, 50, 0.02, 0.02, 0.02, 0);
					world.spawnParticle(Particle.FLAME, loc, 175, 0, 0, 0, 0.175);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 50, 0, 0, 0, 0.25);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.5f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1f);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 1.5f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.5f, 0f);
					new BukkitRunnable() {
						double rotation = 0;
						double radius = 1;
						Location l = mBoss.getLocation();
						@Override
						public void run() {
							if (radius >= 15) {
								this.cancel();
							}
							for (int i = 0; i < 36; i++) {
								Location loc = mBoss.getLocation();
								rotation += 10;
								double radian1 = Math.toRadians(rotation);
								loc.add(Math.cos(radian1)*radius, 0.25, Math.sin(radian1)*radius);
								world.spawnParticle(Particle.FLAME, loc, 2, 0.2, 0.2, 0.2, 0.1, null, true);
								world.spawnParticle(Particle.SMOKE_NORMAL, loc, 1, 0.2, 0.2, 0.2, 0.65, null, true);

								loc.subtract(Math.cos(radian1)*radius, 0.25, Math.sin(radian1)*radius);
							}
							for (Player player : PlayerUtils.playersInRange(l, radius)) {
								if (player.getLocation().toVector().isInSphere(l.toVector(), radius)) {
									BossUtils.bossDamage(mBoss, player, 1);
								}
							}
							radius += 1;

						}

					}.runTaskTimer(mPlugin, 1, 1);
				} else if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 6;
	}

}
