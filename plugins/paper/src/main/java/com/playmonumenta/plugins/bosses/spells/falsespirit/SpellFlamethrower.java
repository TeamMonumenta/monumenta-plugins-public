package com.playmonumenta.plugins.bosses.spells.falsespirit;

import com.playmonumenta.plugins.bosses.bosses.FalseSpirit;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.ArrayList;
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

public class SpellFlamethrower extends Spell {

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private static final int mRange = FalseSpirit.detectionRange;
	private final int mNumTicks = 20 * 3;
	private final int mDuration = 20 * 5;
	private static final int mCooldown = 20 * 15;

	private final boolean mDelve;

	public SpellFlamethrower(Plugin plugin, LivingEntity boss, boolean delve) {
		mPlugin = plugin;
		mBoss = boss;
		mDelve = delve;
	}

	@Override
	public void run() {
		//List is sorted with nearest players further in the list, and farthest players at the beginning
		List<Player> players = EntityUtils.getNearestPlayers(mBoss.getLocation(), FalseSpirit.detectionRange);
		launch(players.get(0));
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}

	private void launch(Player target) {
		List<Player> players = PlayerUtils.playersInRange(mBoss.getLocation(), mRange, true);

		World world = mBoss.getWorld();

		//Start up warning for flamethrower
		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;
			final Location mLoc = target.getEyeLocation();

			@Override
			public void run() {
				if (((!mDelve && mTicks % 6 == 0) || (mDelve && mTicks % 4 == 0)) && mLoc.distance(target.getEyeLocation()) > 0.5) {
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
						new PartialParticle(Particle.CLOUD, endLoc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(mBoss);
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
					blocked = !LocationUtils.hasLineOfSight(launLoc, tarLoc);
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
						final List<Player> mHitPlayers = new ArrayList<>();

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

							if (((!mDelve && mT % 6 == 0) || (mDelve && mT % 4 == 0)) && mLoc.distance(target.getEyeLocation()) > 0.5) {
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
									new PartialParticle(Particle.FLAME, endLoc, 1, 0.02, 0.02, 0.02, 0).spawnAsEntityActive(mBoss);
								}
								if (i % 10 == mFireLoc) {
									new PartialParticle(Particle.SOUL_FIRE_FLAME, endLoc, 3, 0.05, 0.05, 0.05, 0.05).spawnAsEntityActive(mBoss);
								}
								if (i % 40 == 0) {
									world.playSound(endLoc, Sound.ENTITY_FIREWORK_ROCKET_LARGE_BLAST, SoundCategory.HOSTILE, 0.5f, 2f);
								}

								if (mT % 10 == 0) {
									//Do damage here
									double percentDamage = mDelve ? 0.25 : 0.2;
									for (Player player : players) {
										if (box.overlaps(player.getBoundingBox()) && !mHitPlayers.contains(player)) {
											endLoc.getWorld().playSound(endLoc, Sound.BLOCK_FIRE_EXTINGUISH, SoundCategory.HOSTILE, 1, 1);
											BossUtils.bossDamagePercent(mBoss, player, percentDamage, "Flamethrower");
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
								blocked = !LocationUtils.hasLineOfSight(launLoc, tarLoc);
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
