package com.playmonumenta.plugins.bosses.spells.kaul;

import java.util.Collections;
import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

/*
 * Volcanic Demise:
 * Death.
=======
 * Volcanic Demise (CD: 20): Kaul starts summoning meteors that fall from the sky in random areas.
 * Each Meteor deals 42 damage in a 4 block radius on collision with the ground.
 * This ability lasts X seconds and continues spawning meteors until the ability duration runs out.
 * Kaul is immune to damage during the channel of this ability.
 *
 *
 *
 *
 *
 *
 */
public class SpellVolcanicDemise extends Spell {
	private LivingEntity mBoss;
	private Plugin mPlugin;
	private double mRange;
	private Location mCenter;

	public SpellVolcanicDemise(Plugin plugin, LivingEntity boss, double range, Location center) {
		mPlugin = plugin;
		mBoss = boss;
		mRange = range;
		mCenter = center;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		List<Player> players = PlayerUtils.playersInRange(mCenter, 50);
		players.removeIf(p -> p.getLocation().getY() >= 61);
		for (Player player : players) {
			player.sendMessage(ChatColor.GREEN + "SCATTER, INSECTS.");
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {
				mTicks += 2;
				float fTick = mTicks;
				float ft = fTick / 25;
				world.spawnParticle(Particle.LAVA, mBoss.getLocation(), 4, 0.35, 0, 0.35, 0.005);
				world.spawnParticle(Particle.FLAME, mBoss.getLocation().add(0, 1, 0), 3, 0.3, 0, 0.3, 0.125);
				world.playSound(mBoss.getLocation(), Sound.ENTITY_WITHER_SPAWN, 10, 0.5f + ft);
				if (mTicks >= 20 * 2) {
					this.cancel();
					mActiveRunnables.remove(this);
					world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, 1, 0.5f);
					BukkitRunnable runnable = new BukkitRunnable() {

						int i = 0;
						@Override
						public void run() {
							i++;
							List<Player> players = PlayerUtils.playersInRange(mCenter, 50);
							players.removeIf(p -> p.getLocation().getY() >= 61);
							Collections.shuffle(players);
							for (Player player : players) {
								Vector loc = player.getLocation().toVector();
								if (player.getLocation().getBlock().isLiquid() || !loc.isInSphere(mCenter.toVector(), 42)) {
									rainMeteor(player.getLocation(), players, 10);
								}
							}
							for (int j = 0; j < 4; j++) {
								rainMeteor(mCenter.clone().add(FastUtils.randomDoubleInRange(-mRange, mRange), 0, FastUtils.randomDoubleInRange(-mRange, mRange)), players, 40);
							}

							// Target one random player. Have a meteor rain nearby them.
							if (players.size() >= 1) {
								Player rPlayer = players.get(FastUtils.RANDOM.nextInt(players.size()));
								Location loc = rPlayer.getLocation();
								rainMeteor(loc.add(FastUtils.randomDoubleInRange(-8, 8), 0, FastUtils.randomDoubleInRange(-8, 8)), players, 40);
							}

							if (i >= 25) {
								this.cancel();
								mActiveRunnables.remove(this);
							}
						}

					};
					runnable.runTaskTimer(mPlugin, 0, 10);
					mActiveRunnables.add(runnable);
				}
			}

		};
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	private void rainMeteor(Location locInput, List<Player> players, double spawnY) {
		if (locInput.distance(mCenter) > 50 || locInput.getY() >= 55) {
			// Somehow tried to spawn a meteor too far away from the center point
			return;
		}

		BukkitRunnable runnable = new BukkitRunnable() {
			double y = spawnY;
			Location loc = locInput.clone();
			World world = locInput.getWorld();

			@Override
			public void run() {
				players.removeIf(p -> p.getLocation().distance(mCenter) > 50 || p.getLocation().getY() >= 61);

				y -= 1;
				if (y % 2 == 0) {
					for (Player player : players) {
						// Player gets more particles the closer they are to the landing area
						double dist = player.getLocation().distance(loc);
						double step = dist < 10 ? 0.5 : (dist < 15 ? 1 : 3);
						for (double deg = 0; deg < 360; deg += (step * 30)) {
							player.spawnParticle(Particle.FLAME, loc.clone().add(Math.cos(deg), 0, Math.sin(deg)), 1, 0.15, 0.15, 0.15, 0);
						}
					}
				}
				Location particle = loc.clone().add(0, y, 0);
				world.spawnParticle(Particle.FLAME, particle, 3, 0.2f, 0.2f, 0.2f, 0.05, null, true);
				if (FastUtils.RANDOM.nextBoolean()) {
					world.spawnParticle(Particle.SMOKE_LARGE, particle, 1, 0, 0, 0, 0, null, true);
				}
				world.playSound(particle, Sound.ENTITY_BLAZE_SHOOT, 1, 1);
				if (y <= 0) {
					this.cancel();
					mActiveRunnables.remove(this);
					world.spawnParticle(Particle.FLAME, loc, 50, 0, 0, 0, 0.175, null, true);
					world.spawnParticle(Particle.SMOKE_LARGE, loc, 10, 0, 0, 0, 0.25, null, true);
					world.playSound(loc, Sound.ENTITY_GENERIC_EXPLODE, 1.5f, 0.9f);
					BoundingBox death = BoundingBox.of(loc, 1.5, 1.5, 1.5);
					BoundingBox box = BoundingBox.of(loc, 4, 4, 4);
					for (Player player : PlayerUtils.playersInRange(loc, 4)) {
						BoundingBox pBox = player.getBoundingBox();
						if (pBox.overlaps(death)) {
							BossUtils.bossDamage(mBoss, player, 1000, loc, (event) -> {
								MovementUtils.knockAway(loc, player, 0.5f, 0.65f);
							});
						} else if (pBox.overlaps(box)) {
							BossUtils.bossDamage(mBoss, player, 42, loc, (event) -> {
								if (!event.isPlayerBlocking()) {
									MovementUtils.knockAway(loc, player, 0.5f, 0.65f);
								}
							});
						}
					}
					for (Block block : LocationUtils.getNearbyBlocks(loc.getBlock(), 4)) {
						if (FastUtils.RANDOM.nextDouble() < 0.125) {
							if (block.getType() == Material.SMOOTH_RED_SANDSTONE) {
								block.setType(Material.NETHERRACK);
							} else if (block.getType() == Material.NETHERRACK) {
								block.setType(Material.MAGMA_BLOCK);
							} else if (block.getType() == Material.SMOOTH_SANDSTONE) {
								block.setType(Material.SMOOTH_RED_SANDSTONE);
							}
						}
					}
				}
			}
		};
		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int castTime() {
		return 20 * 17;
	}

	@Override
	public int duration() {
		return 20 * 35;
	}

}
