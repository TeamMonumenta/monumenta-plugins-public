package com.playmonumenta.plugins.bosses.spells.spells_headlesshorseman;

import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.HeadlessHorsemanBoss;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Hellzone Grenade - The horseman fires a pumpkin (Fireball with pumpkin block maybe) that
explodes on contact with the ground or a player, dealing NM20/HM35 damage in a 3 block radius.
Enemies hit are ignited for 3 seconds. The area affected leaves behind a lingering AoE if it hits
the ground, this lingering fire is also 3 blocks in radius and deals 5% max health damage every 0.5
seconds to players and ignites them for 3 seconds. (Does not leave the lingering thing behind if
it hits a player or the horseman)

Falling Block Projectile
 */

public class SpellHellzoneGrenade extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mRange;
	private Location mCenter;
	private HeadlessHorsemanBoss mHorseman;

	public SpellHellzoneGrenade(Plugin plugin, LivingEntity entity, Location center, double range, HeadlessHorsemanBoss horseman) {
		mPlugin = plugin;
		mBoss = entity;
		mRange = range;
		mCenter = center;
		mHorseman = horseman;
	}

	@Override
	public void run() {
		mHorseman.disableShield();
		World world = mBoss.getWorld();
		// Choose random player within range that has line of sight to boss
		List<Player> players = PlayerUtils.playersInRange(mCenter, mRange);

		new BukkitRunnable() {
			int t = 0;
			@Override
			public void run() {
				t++;

				world.spawnParticle(Particle.FLAME, mBoss.getLocation(), 40, 0, 0, 0, 0.1);
				world.spawnParticle(Particle.SMOKE_NORMAL, mBoss.getLocation(), 35, 0, 0, 0, 0.1);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 3, 0.75f);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_SNOWBALL_THROW, 3, 0.65f);
				Collections.shuffle(players);
				for (Player player : players) {
					if (LocationUtils.hasLineOfSight(mBoss, player)) {
						launch(player);
						break;
					}
				}
				if (t >= 5) {
					this.cancel();
				}
			}

		}.runTaskTimer(mPlugin, 0, 15);
	}

	public void launch(Player target) {
		Location sLoc = mBoss.getLocation();
		sLoc.setY(sLoc.getY() + 1.7f);
		try {
			FallingBlock fallingBlock = sLoc.getWorld().spawnFallingBlock(sLoc, Material.JACK_O_LANTERN.createBlockData());
			fallingBlock.setDropItem(false);

			Location pLoc = target.getLocation();
			Location tLoc = fallingBlock.getLocation();
			Vector vect = new Vector(pLoc.getX() - tLoc.getX(), 0, pLoc.getZ() - tLoc.getZ());
			vect.normalize().multiply((pLoc.distance(tLoc)) / 20).setY(0.7f);
			fallingBlock.setVelocity(vect);

			new BukkitRunnable() {
				World world = mBoss.getWorld();
				@Override
				public void run() {
					world.spawnParticle(Particle.FLAME, fallingBlock.getLocation().add(0, fallingBlock.getHeight() / 2, 0), 3, 0.25, .25, .25, 0.025);
					world.spawnParticle(Particle.SMOKE_NORMAL, fallingBlock.getLocation().add(0, fallingBlock.getHeight() / 2, 0), 2, 0.25, .25, .25, 0.025);
					if (fallingBlock.isOnGround() || !fallingBlock.isValid()) {
						fallingBlock.remove();
						fallingBlock.getLocation().getBlock().setType(Material.AIR);;
						world.spawnParticle(Particle.FLAME, fallingBlock.getLocation(), 150, 0, 0, 0, 0.165);
						world.spawnParticle(Particle.SMOKE_LARGE, fallingBlock.getLocation(), 65, 0, 0, 0, 0.1);
						world.spawnParticle(Particle.EXPLOSION_LARGE, fallingBlock.getLocation(), 1, 0, 0, 0, 0);
						world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 2, 0.85f);

						for (Player player : PlayerUtils.playersInRange(fallingBlock.getLocation(), 4)) {
							if (mCenter.distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
								BossUtils.bossDamage(mBoss, player, 35);
								// Shields don't stop fire!
								player.setFireTicks(20 * 3);
							}

						}

						new BukkitRunnable() {
							int t = 0;
							Location loc = fallingBlock.getLocation();
							@Override
							public void run() {
								t += 2;
								world.spawnParticle(Particle.FLAME, loc, 12, 1.5, 0.15, 1.5, 0.05);
								world.spawnParticle(Particle.SMOKE_LARGE, loc, 4, 1.5, 0.15, 1.5, 0.025);

								if (t % 10 == 0) {
									for (Player player : PlayerUtils.playersInRange(fallingBlock.getLocation(), 4)) {
										if (mCenter.distance(player.getLocation()) < HeadlessHorsemanBoss.detectionRange) {
											/* Fire aura can not be blocked */
											BossUtils.bossDamagePercent(mBoss, player, 0.05, (Location)null);
											player.setFireTicks(20 * 3);
										}
									}
								}

								if (t >= 20 * 5) {
									this.cancel();
								}
							}

						}.runTaskTimer(mPlugin, 10, 2);
						this.cancel();


					}
				}

			}.runTaskTimer(mPlugin, 0, 1);
		} catch (Exception e) {
			mPlugin.getLogger().warning("Failed to summon grenade for hellzone grenade toss: " + e.getMessage());
			e.printStackTrace();
		}
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 20 * 10;
	}

}
