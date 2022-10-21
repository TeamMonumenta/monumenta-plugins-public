package com.playmonumenta.plugins.bosses.spells.imperialconstruct;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class SpellSlice extends Spell {

	private LivingEntity mBoss;
	private Plugin mPlugin;
	private Location mCurrentLoc;

	//Each ring layer removed, increment to keep track of location
	private int mRingIncrement = 0;

	//Keeps track of which quadrants have already been destroyed
	private List<Integer> mDirs = new ArrayList<>();

	//True to destroy in rings, false for quadrants
	private boolean mRingMode = false;

	public SpellSlice(LivingEntity boss, Plugin plugin, Location currentLoc) {
		mBoss = boss;
		mPlugin = plugin;
		mCurrentLoc = currentLoc;
	}

	@Override
	public void run() {
		BukkitRunnable runnable;
		if (mRingMode) {
			runnable = new BukkitRunnable() {
				private int mTicks = 0;
				@Override
				public void run() {
					if (mTicks % 10 == 0) {
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 50f, 0f);

						Vector vec;
						Location l = mCurrentLoc.clone();
						//The degree range is 60 degrees for 30 blocks radius
						for (double degree = 0; degree < 360; degree += 2) {
							for (double r = 16 - mRingIncrement; r < 24 - mRingIncrement; r++) {
								double radian1 = Math.toRadians(degree);
								vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);

								//Deletes rings/layers (like an onion)
								l.set(mCurrentLoc.getX(), mCurrentLoc.getY(), mCurrentLoc.getZ()).add(vec);
								Location tempL = l.clone();
								for (int y = -8; y < 0; y++) {
									tempL.set(l.getX(), l.getY() + y, l.getZ());
									Block b = tempL.getBlock();
									if (mTicks == 20) {
										if (b.getType() == Material.COBBLESTONE && FastUtils.RANDOM.nextInt(3) == 0) {
											int temp = FastUtils.RANDOM.nextInt(1);
											if (temp == 0) {
												b.setType(Material.MAGMA_BLOCK);
											} else if (temp == 1) {
												b.setType(Material.COBBLESTONE);
											}
										}
									} else if (mTicks >= 20 * 3) {
										if (b.getType() == Material.COBBLESTONE || b.getType() == Material.MAGMA_BLOCK || b.getType() == Material.TUFF) {
											int temp = FastUtils.RANDOM.nextInt(1);
											if (temp == 0) {
												b.setType(Material.MAGMA_BLOCK);
											} else if (temp == 1) {
												b.setType(Material.COBBLESTONE);
											}
										}
									}
								}
							}
						}

						if (mTicks >= 20 * 3) {
							mRingIncrement += 4;
							this.cancel();
						}
					}
					mTicks += 2;
				}
			};
		} else {
			int xMin = 0;
			int xMax = 0;
			int zMin = 0;
			int zMax = 0;

			if (mDirs.size() >= 4) {
				return;
			}

			int random = FastUtils.RANDOM.nextInt(4);
			while (mDirs.contains(random)) {
				random = FastUtils.RANDOM.nextInt(4);
			}

			mDirs.add(random);

			switch (random) {
				default:
				case 0:
					xMin = -25;
					zMin = -25;
					break;
				case 1:
					xMax = 25;
					zMin = -25;
					break;
				case 2:
					xMin = -25;
					zMax = 25;
					break;
				case 3:
					xMax = 25;
					zMax = 25;
					break;
			}

			int finalXMin = xMin;
			int finalXMax = xMax;
			int finalZMax = zMax;
			int finalZMin = zMin;
			runnable = new BukkitRunnable() {
				private int mTicks = 0;
				@Override
				public void run() {
					if (mTicks % 10 == 0) {
						Location tempLoc = mCurrentLoc.clone();
						mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, SoundCategory.HOSTILE, 50f, 0f);

						for (int x = finalXMin; x <= finalXMax; x++) {
							for (int z = finalZMin; z <= finalZMax; z++) {
								for (int y = 0; y >= -8; y--) {
									tempLoc.set(mCurrentLoc.getX() + x, mCurrentLoc.getY() + y, mCurrentLoc.getZ() + z);
									Block b = tempLoc.getBlock();
									if (mTicks == 20) {
										if (b.getType() == Material.COBBLESTONE && FastUtils.RANDOM.nextInt(3) == 0) {
											b.setType(Material.MAGMA_BLOCK);
										}
									} else if (mTicks >= 20 * 4) {
										if (b.getType() == Material.COBBLESTONE || b.getType() == Material.MAGMA_BLOCK || b.getType() == Material.TUFF) {
											b.setType(Material.AIR);
										}
										this.cancel();
									}
								}
							}
						}
					}

					mTicks += 2;
				}
			};
		}
		runnable.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public void setLocation(Location loc) {
		mCurrentLoc = loc;
		mDirs.clear();
		for (BukkitRunnable r : mActiveRunnables) {
			r.cancel();
		}
	}

	public void setRingMode(boolean ringMode) {
		mRingMode = ringMode;
	}
}
