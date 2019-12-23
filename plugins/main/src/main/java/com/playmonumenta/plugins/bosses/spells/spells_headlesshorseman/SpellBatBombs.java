package com.playmonumenta.plugins.bosses.spells.spells_headlesshorseman;

import java.util.concurrent.ThreadLocalRandom;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.bosses.bosses.HeadlessHorsemanBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.utils.DamageUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Bat Bomb - The Horseman summons 20* bats around himself to distract his enemies. After 3 seconds
the bats explode dealing 18/25 damage to anyone with 4 blocks of them, killing the bat in the process.
The bats have 20hp.
 */
public class SpellBatBombs extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private ThreadLocalRandom mRand;
	private HeadlessHorsemanBoss mHorseman;

	public SpellBatBombs(Plugin plugin, LivingEntity entity, HeadlessHorsemanBoss horseman) {
		mPlugin = plugin;
		mBoss = entity;
		mRand = ThreadLocalRandom.current();
		mHorseman = horseman;
	}

	public void spawnBat(Location loc) {
		World world = loc.getWorld();
		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;
				if (t >= 30) {
					this.cancel();
					world.spawnParticle(Particle.SMOKE_NORMAL, loc, 25, 0.15, .15, .15, 0.125);

					LivingEntity bat = (LivingEntity) world.spawnEntity(loc, EntityType.BAT);
					bat.setCustomName("A Spooky Bat");
					new BukkitRunnable() {
						int t = 0;
						@Override
						public void run() {
							t++;
							world.spawnParticle(Particle.FLAME, bat.getLocation(), 1, 0.25, .25, .25, 0.025);
							world.spawnParticle(Particle.SMOKE_NORMAL, bat.getLocation(), 2, 0.25, .25, .25, 0.025);
							if (t >= 20 * 6) {
								bat.remove();
								this.cancel();
								Location loc = bat.getLocation();
								world.spawnParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.15);
								world.spawnParticle(Particle.SMOKE_LARGE, loc, 25, 0, 0, 0, 0.1);
								world.spawnParticle(Particle.SMOKE_NORMAL, loc, 50, 0, 0, 0, 0.15);
								world.spawnParticle(Particle.EXPLOSION_LARGE, loc, 1, 0, 0, 0, 0);
								world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 0.65f, 1);

								for (Player player : PlayerUtils.playersInRange(loc, 6)) {
									if (mHorseman.getSpawnLocation().distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
										if (!player.isBlocking()) {
											DamageUtils.damage(bat, player, 32);
											MovementUtils.knockAway(loc, player, 0.2f, 0.4f);
										} else {
											player.setCooldown(Material.SHIELD, 20 * 8);
										}
									}
								}
							}
						}

					}.runTaskTimer(mPlugin, 0, 1);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		for (int i = 0; i < 14; i++) {
			Location loc = new Location(
			    world,
			    mBoss.getLocation().getX() + mRand.nextDouble(-15, 15),
			    mBoss.getLocation().getY() + mRand.nextDouble(1, 3),
			    mBoss.getLocation().getZ() + mRand.nextDouble(-15, 15)
			);
			while (loc.getBlock().getType().isSolid()) {
				loc = new Location(
				    world,
				    mBoss.getLocation().getX() + mRand.nextDouble(-15, 15),
				    mBoss.getLocation().getY() + mRand.nextDouble(1, 3),
				    mBoss.getLocation().getZ() + mRand.nextDouble(-15, 15)
				);
			}
			if (mHorseman.getSpawnLocation().distance(loc) < HeadlessHorsemanBoss.detectionRange) {
				spawnBat(loc);
			} else {
				i--;
			}
		}

		world.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_MIRROR, 3, 1.1f);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 3, 0.75f);
		world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 20, 0.4, .4, .4, 0.1);
		new BukkitRunnable() {
			int t = 0;
			Location loc = mBoss.getLocation().add(0, 1, 0);
			@Override
			public void run() {
				t++;

				if (mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
					return;
				}

				world.spawnParticle(Particle.SMOKE_NORMAL, loc, 5, 0.4, .4, .4, 0.025);
				if (t >= 30) {
					this.cancel();
					world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 5, 0.4, .4, .4, 0.125);
					world.spawnParticle(Particle.SMOKE_LARGE, mBoss.getLocation().add(0, 1, 0), 20, 0.4, .4, .4, 0.09);
					world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation().add(0, 1, 0), 40, 0.4, .4, .4, 0.1);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3, 0.65f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 3, 0.75f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 3, 1f);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 3, 1.25f);
				}
			}

		}.runTaskTimer(mPlugin, 0, 1);
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 10;
	}

}
