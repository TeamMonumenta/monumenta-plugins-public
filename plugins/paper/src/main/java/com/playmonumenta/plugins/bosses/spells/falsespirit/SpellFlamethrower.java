package com.playmonumenta.plugins.bosses.spells.falsespirit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.bosses.bosses.FalseSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellFlamethrower extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private final int mRange = FalseSpirit.detectionRange;
	private final int mNumTicks = 20 * 3;
	private final int mDuration = 20 * 5;
	private final int mCooldown = 20 * 15;
	private Location mLoc = null;

	public SpellFlamethrower(Plugin plugin, LivingEntity boss) {
		mPlugin = plugin;
		mBoss = boss;
	}

	@Override
	public void run() {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange);
		if (!players.isEmpty()) {
			Collections.shuffle(players);
			launch(players.get(0));
			if (players.size() >= 5) {
				launch(players.get(1));
			}
		}

	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	private void launch(Player target) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange);

		mLoc = target.getEyeLocation();
		World world = mBoss.getWorld();

		//Start up warning for flamethrower
		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;
			@Override
			public void run() {
				if (mTicks % 6 == 0 && mLoc.distance(target.getEyeLocation()) > 0.5) {
					mLoc.add(target.getEyeLocation().toVector().subtract(mLoc.toVector()).normalize());
				}

				Location launLoc = mBoss.getLocation().add(0, 1.6f, 0);
				Location tarLoc = mLoc.clone();
				Location endLoc = launLoc;
				BoundingBox box = BoundingBox.of(endLoc, 0.5, 0.5, 0.5);

				Vector baseVect = new Vector(tarLoc.getX() - launLoc.getX(), tarLoc.getY() - launLoc.getY(), tarLoc.getZ() - launLoc.getZ()).normalize().multiply(0.5);

				boolean blocked = false;
				for (int i = 0; i < 100; i++) {
					box.shift(baseVect);
					endLoc = box.getCenter().toLocation(mBoss.getWorld());

					if (FastUtils.RANDOM.nextInt(3) == 0) {
						endLoc.getWorld().spawnParticle(Particle.CLOUD, endLoc, 1, 0.02, 0.02, 0.02, 0);
					}

					List<Block> blocks = new ArrayList<Block>();
					for (int x = -1; x < 1; x++) {
						for (int y = -1; y < 1; y++) {
							for (int z = -1; z < 1; z++) {
								blocks.add(endLoc.clone().add(x, y, z).getBlock());
							}
						}
					}

					boolean cancel = false;
					for (Block block : blocks) {
						if (block.getBoundingBox().overlaps(box) && !block.isLiquid()) {
							cancel = true;
							break;
						}
					}

					if (cancel) {
						blocked = true;
						break;
					}
					if (endLoc.getBlock().getType().isSolid()) {
						blocked = true;
						break;
					}
				}

				if (!blocked) {
					// Really check to make sure it's not blocked
					// This takes into account block shapes!
					blocked = !(LocationUtils.hasLineOfSight(launLoc, tarLoc));
				}

				target.playSound(target.getLocation(), Sound.ENTITY_SHULKER_BULLET_HIT, SoundCategory.HOSTILE, 0.8f, 0.5f + (mTicks / 80f) * 1.5f);

				if (mTicks >= mNumTicks) {

					world.playSound(target.getLocation(), Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 10f, 0);

					//Activate flamethrower
					BukkitRunnable runnable = new BukkitRunnable() {
						int mT = 0;
						//Where the fire particle should go in the laser
						int mFireLoc = 0;
						//Don't hit a player twice in the same tick
						List<Player> mHitPlayers = new ArrayList<>();
						@Override
						public void run() {
							if (mT % 20 == 0) {
								mHitPlayers.clear();
							}

							if (mT >= mDuration) {
								world.playSound(mBoss.getLocation(), Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.HOSTILE, 10f, 1f);

								this.cancel();
								mActiveRunnables.remove(this);
							}

							if (mT % 6 == 0 && mLoc.distance(target.getEyeLocation()) > 0.5) {
								mLoc.add(target.getEyeLocation().toVector().subtract(mLoc.toVector()).normalize());
							}

							Location launLoc = mBoss.getLocation().add(0, 1.6f, 0);
							Location tarLoc = mLoc.clone();
							Location endLoc = launLoc;
							BoundingBox box = BoundingBox.of(endLoc, 0.5, 0.5, 0.5);

							Vector baseVect = new Vector(tarLoc.getX() - launLoc.getX(), tarLoc.getY() - launLoc.getY(), tarLoc.getZ() - launLoc.getZ()).normalize().multiply(0.5);

							boolean blocked = false;
							for (int i = 0; i < 100; i++) {
								box.shift(baseVect);
								endLoc = box.getCenter().toLocation(mBoss.getWorld());
								if (FastUtils.RANDOM.nextInt(3) == 0) {
									endLoc.getWorld().spawnParticle(Particle.FLAME, endLoc, 1, 0.02, 0.02, 0.02, 0);
								}
								if (i % 10 == mFireLoc) {
									endLoc.getWorld().spawnParticle(Particle.SOUL_FIRE_FLAME, endLoc, 3, 0.05, 0.05, 0.05, 0.05);
								}
								if (i % 40 == 0) {
									world.playSound(endLoc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.HOSTILE, 0.5f, 2f);
								}

								if (mT % 10 == 0) {
									//Do damage here
									for (Player player : players) {
										if (box.overlaps(player.getBoundingBox()) && !mHitPlayers.contains(player)) {
											endLoc.getWorld().playSound(endLoc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 1, 1);
											BossUtils.bossDamagePercent(mBoss, player, 0.2);
											mHitPlayers.add(player);
										}
									}
								}

								List<Block> blocks = new ArrayList<Block>();
								for (int x = -1; x < 1; x++) {
									for (int y = -1; y < 1; y++) {
										for (int z = -1; z < 1; z++) {
											blocks.add(endLoc.clone().add(x, y, z).getBlock());
										}
									}
								}

								boolean cancel = false;
								for (Block block : blocks) {
									if (block.getBoundingBox().overlaps(box) && !block.isLiquid()) {
										cancel = true;
										break;
									}
								}

								if (cancel) {
									blocked = true;
									break;
								}
								if (endLoc.getBlock().getType().isSolid()) {
									blocked = true;
									break;
								}
							}

							if (!blocked) {
								// Really check to make sure it's not blocked
								// This takes into account block shapes!
								blocked = !(LocationUtils.hasLineOfSight(launLoc, tarLoc));
							}

							mFireLoc++;
							if (mFireLoc >= 9) {
								mFireLoc = 0;
							}

							mT += 2;
						}
					};
					runnable.runTaskTimer(mPlugin, 0, 2);
					mActiveRunnables.add(runnable);

					this.cancel();
					mActiveRunnables.remove(this);
					return;
				}

				mTicks += 2;
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

}
