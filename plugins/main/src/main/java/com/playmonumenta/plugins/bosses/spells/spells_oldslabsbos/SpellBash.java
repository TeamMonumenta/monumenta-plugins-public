package com.playmonumenta.plugins.bosses.spells.spells_oldslabsbos;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Creature;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.utils.DamageUtils;
import com.playmonumenta.plugins.bosses.utils.Utils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class SpellBash extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final World mWorld;

	public SpellBash(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = mBoss.getWorld();
	}

	@Override
	public void run() {
		mBoss.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 25, 1));

		Creature c = (Creature) mBoss;
		LivingEntity target = c.getTarget();
		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.7f);
		mWorld.playSound(mBoss.getLocation(), Sound.BLOCK_ANVIL_PLACE, 1.5f, 1.75f);
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				mWorld.spawnParticle(Particle.CLOUD, mBoss.getLocation(), 1, 0.1, 0.1, 0.1, 0.175);
				mWorld.spawnParticle(Particle.CRIT, mBoss.getLocation().add(0, 1, 0), 3, 0.4, 0.5, 0.4, 0.025);
				if (t >= 25) {
					this.cancel();
					Location loc = mBoss.getEyeLocation().subtract(0, 0.15, 0);
					Vector direction = Utils.getDirectionTo(target.getLocation().add(0, 1.25, 0), loc);
					loc.setDirection(direction);
					mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_SWEEP, 1.5f, 0.7f);
					mWorld.playSound(loc, Sound.ENTITY_PLAYER_ATTACK_KNOCKBACK, 1.5f, 1.25f);
					mWorld.spawnParticle(Particle.CLOUD, mBoss.getLocation(), 25, 0.1, 0.1, 0.1, 0.25);
					mWorld.spawnParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation(), 15, 0.1, 0.1, 0.1, 0.2);
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

					for (Player player : Utils.playersInRange(loc, 4)) {
						Vector toPlayerVector = player.getLocation().toVector().subtract(loc.toVector()).normalize();
						if (direction.dot(toPlayerVector) > 0.33f) {
							DamageUtils.damage(mBoss, player, 6);
							Utils.KnockAway(mBoss.getLocation(), player, 0.5f, 0.65f);
						}
					}
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 5;
	}

	@Override
	public boolean canRun() {
		Creature creature = (Creature) mBoss;
		if (creature.getTarget() != null) {

			if (creature.getTarget().getLocation().distance(mBoss.getLocation()) < 4) {
				return true;
			}

		}

		return false;
	}

}
