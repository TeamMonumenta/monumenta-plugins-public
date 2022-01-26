package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.List;

public class UltimateSeismicRuin extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private World mWorld;

	//Lists which directions have already been used
	private List<Character> mDirections;

	private LivingEntity mNorthStand;
	private LivingEntity mEastStand;
	private LivingEntity mSouthStand;
	private LivingEntity mWestStand;

	public UltimateSeismicRuin(Plugin plugin, LivingEntity boss, List<Character> dirs, LivingEntity n, LivingEntity e, LivingEntity s, LivingEntity w) {
		mPlugin = plugin;
		mBoss = boss;
		mWorld = boss.getWorld();

		mNorthStand = n;
		mEastStand = e;
		mSouthStand = s;
		mWestStand = w;

		mDirections = dirs;
	}

	@Override
	public void run() {
		if (mDirections.size() > 0) {
			int random = FastUtils.RANDOM.nextInt(mDirections.size());
			char dir = mDirections.get(random);
			mDirections.remove(random);
			switch (dir) {
			default:
				break;
			case 'n':
				ruinNorth();
				break;
			case 'e':
				ruinEast();
				break;
			case 's':
				ruinSouth();
				break;
			case 'w':
				ruinWest();
				break;
			}
		} else {
			return;
		}

		mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ILLUSIONER_PREPARE_BLINDNESS, SoundCategory.HOSTILE, 5, 0.5f);
	}

	@Override
	public int cooldownTicks() {
		return 20 * 8;
	}

	private void ruinNorth() {
		Location loc = mNorthStand.getLocation();

		Location l = loc.clone();

		int randZ = (int) FastUtils.randomDoubleInRange(9, 12);

		for (int x = -20; x <= 20; x++) {
			for (int z = -4; z <= randZ; z++) {
				l.set(loc.getX() + x, loc.getY() - 1, loc.getZ() + z);
				if (l.getBlock().getType() != Material.AIR) {
					int rand = FastUtils.RANDOM.nextInt(6);
					switch (rand) {
					default:
					case 0:
					case 1:
					case 2:
						break;
					case 3:
					case 4:
						l.getBlock().setType(Material.MAGMA_BLOCK);
						break;
					case 5:
						l.getBlock().setType(Material.HONEY_BLOCK);
						break;
					}
				}
			}
		}

		new BukkitRunnable() {
			int mT = 0;
			int mY = 12;
			float mPitch = 0;
			@Override
			public void run() {
				if (mT % 10 == 0) {
					mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 2, mPitch);
				}
				mPitch += 0.025f;

				if (mT >= 20 * 4) {
					mWorld.playSound(loc.clone().add(0, 0, 5), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1);
					mY -= 2;
				}

				if (mT >= 20 * 5) {

					mWorld.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0);

					new BukkitRunnable() {
						int mTicks = 0;
						int mX = 0;
						int mZ = -6;
						@Override
						public void run() {
							mTicks++;

							Location l = loc.clone();

							if (mTicks % 10 == 0) {
								mWorld.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5, 0);
								for (int i = -18; i <= 18; i += 2) {
									l.set(loc.getX() + i, loc.getY(), loc.getZ() + mZ);
									mWorld.spawnParticle(Particle.EXPLOSION_HUGE, l, 1, 1, 1, 1);
								}
							}

							l.set(loc.getX(), loc.getY(), loc.getZ());

							//Slowly "decay" blocks in both directions of the platform
							for (int y = -10; y <= 0; y++) {
								for (int i = -(mX + 8); i <= mX + 8 && i <= 28; i++) {
									l.set(loc.getX(), loc.getY(), loc.getZ());
									l.add(i, y, mZ);
									l.getBlock().setType(Material.AIR);
								}
							}

							if (mTicks >= 20 * 3 || mZ >= randZ) {

								//Clean up un-decayed blocks
								for (int i = mZ; i <= randZ; i++) {
									for (int y = -10; y <= 0; y++) {
										for (int j = -28; j <= 28; j++) {
											l.set(loc.getX() + j, loc.getY() + y, loc.getZ() + i);
											l.getBlock().setType(Material.AIR);
										}
									}
								}

								//Randomized edges
								for (int z = randZ + 1; z <= randZ + 3; z++) {
									for (int y = -10; y <= 0; y++) {
										for (int x = -28; x <= 28; x++) {
											if (FastUtils.RANDOM.nextInt(2) == 0) {
												l.set(loc.getX() + x, loc.getY() + y, loc.getZ() + z);
												l.getBlock().setType(Material.AIR);
											}
										}
									}
								}

								this.cancel();
							}

							mX += 8;
							if (mX >= 28) {
								mZ++;
								mX = 0;
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);

					this.cancel();
				} else {
					//Particles are -2 z from north stand
					for (int x = -18; x <= 18; x += 2) {
						Location l = loc.clone().add(x, 0, -2);
						int count = 1;

						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(0, 0, -1), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(0, 0, 1), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(0, 0, 9), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(0, 0, 5), count, 1, 0.15, 1, 0.25);

						if (x % 4 == 0) {
							mWorld.spawnParticle(Particle.EXPLOSION_LARGE, l.clone().add(0, mY, 5), count / 3, 0.15, 0.15, 0.15, 0.05);
						}
					}
				}
				mT += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	private void ruinEast() {
		Location loc = mEastStand.getLocation().clone();

		int randX = (int) FastUtils.randomDoubleInRange(-12, -9);

		for (int z = -20; z <= 20; z++) {
			for (int x = randX; x <= 4; x++) {
				Location l = loc.clone().add(x, -1, z);
				if (l.getBlock().getType() != Material.AIR) {
					int rand = FastUtils.RANDOM.nextInt(6);
					switch (rand) {
					default:
					case 0:
					case 1:
					case 2:
						break;
					case 3:
					case 4:
						l.getBlock().setType(Material.MAGMA_BLOCK);
						break;
					case 5:
						l.getBlock().setType(Material.HONEY_BLOCK);
						break;
					}
				}
			}
		}

		new BukkitRunnable() {
			int mT = 0;
			int mY = 12;
			float mPitch = 0;
			@Override
			public void run() {
				if (mT % 10 == 0) {
					mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 2, mPitch);
				}
				mPitch += 0.025f;

				if (mT >= 20 * 4) {
					mWorld.playSound(loc.clone().add(-5, 0, 0), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1);
					mY -= 2;
				}

				if (mT >= 20 * 5) {

					mWorld.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0);

					new BukkitRunnable() {
						int mTicks = 0;
						int mX = 6;
						int mZ = 0;
						@Override
						public void run() {
							mTicks++;

							Location l = loc.clone();

							if (mTicks % 10 == 0) {
								mWorld.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5, 0);
								for (int i = -18; i <= 18; i += 2) {
									l.set(loc.getX() + mX, loc.getY(), loc.getZ() + i);
									mWorld.spawnParticle(Particle.EXPLOSION_HUGE, l, 1, 1, 1, 1);
								}
							}

							l.set(loc.getX(), loc.getY(), loc.getZ());

							//Slowly "decay" blocks in both directions of the platform
							for (int y = -10; y <= 0; y++) {
								for (int i = -(mZ + 8); i <= mZ + 8 && i <= 28; i++) {
									l.set(loc.getX(), loc.getY(), loc.getZ());
									l.add(mX, y, i);
									l.getBlock().setType(Material.AIR);
								}
							}

							if (mTicks >= 20 * 3 || mX <= randX) {

								//Clean up un-decayed blocks
								for (int i = mX; i >= randX; i--) {
									for (int y = -10; y <= 0; y++) {
										for (int j = -28; j <= 28; j++) {
											l.set(loc.getX() + i, loc.getY() + y, loc.getZ() + j);
											l.getBlock().setType(Material.AIR);
										}
									}
								}

								//Randomized edges
								for (int x = randX - 3; x <= randX - 1; x++) {
									for (int y = -10; y <= 0; y++) {
										for (int z = -28; z <= 28; z++) {
											if (FastUtils.RANDOM.nextInt(2) == 0) {
												l.set(loc.getX() + x, loc.getY() + y, loc.getZ() + z);
												l.getBlock().setType(Material.AIR);
											}
										}
									}
								}

								this.cancel();
							}

							mZ += 8;
							if (mZ >= 28) {
								mX--;
								mZ = 0;
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);

					this.cancel();
				} else {
					//Particles are -2 z from north stand
					for (int z = -18; z <= 18; z += 2) {
						Location l = loc.clone().add(2, 0, z);

						int count = 1;

						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(-9, 0, 0), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(1, 0, 0), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(-1, 0, 0), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(-5, 0, 0), count, 1, 0.15, 1, 0.25);

						if (z % 4 == 0) {
							mWorld.spawnParticle(Particle.EXPLOSION_LARGE, l.clone().add(-5, mY, 0), count / 3, 0.15, 0.15, 0.15, 0.05);
						}
					}
				}
				mT += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	private void ruinSouth() {
		Location loc = mSouthStand.getLocation().clone();

		int randZ = (int) FastUtils.randomDoubleInRange(-12, -9);

		for (int x = -20; x <= 20; x++) {
			for (int z = randZ; z <= 4; z++) {
				Location l = loc.clone().add(x, -1, z);
				if (l.getBlock().getType() != Material.AIR) {
					int rand = FastUtils.RANDOM.nextInt(6);
					switch (rand) {
					default:
					case 0:
					case 1:
					case 2:
						break;
					case 3:
					case 4:
						l.getBlock().setType(Material.MAGMA_BLOCK);
						break;
					case 5:
						l.getBlock().setType(Material.HONEY_BLOCK);
						break;
					}
				}
			}
		}

		new BukkitRunnable() {
			int mT = 0;
			int mY = 12;
			float mPitch = 0;
			@Override
			public void run() {
				if (mT % 10 == 0) {
					mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 2, mPitch);
				}
				mPitch += 0.025f;

				if (mT >= 20 * 4) {
					mWorld.playSound(loc.clone().add(0, 0, -5), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1);
					mY -= 2;
				}

				if (mT >= 20 * 5) {

					mWorld.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0);

					new BukkitRunnable() {
						int mTicks = 0;
						int mX = 0;
						int mZ = 6;
						@Override
						public void run() {
							mTicks++;

							Location l = loc.clone();

							if (mTicks % 10 == 0) {
								mWorld.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5, 0);
								for (int i = -18; i <= 18; i += 2) {
									l.set(loc.getX() + i, loc.getY(), loc.getZ() + mZ);
									mWorld.spawnParticle(Particle.EXPLOSION_HUGE, l, 1, 1, 1, 1);
								}
							}

							l.set(loc.getX(), loc.getY(), loc.getZ());

							//Slowly "decay" blocks in both directions of the platform
							for (int y = -10; y <= 0; y++) {
								for (int i = -(mX + 8); i <= mX + 8 && i <= 28; i++) {
									l.set(loc.getX(), loc.getY(), loc.getZ());
									l.add(i, y, mZ);
									l.getBlock().setType(Material.AIR);
								}
							}

							if (mTicks >= 20 * 3 || mZ <= randZ) {

								//Clean up un-decayed blocks
								for (int i = mZ; i >= randZ; i--) {
									for (int y = -10; y <= 0; y++) {
										for (int j = -28; j <= 28; j++) {
											l.set(loc.getX() + j, loc.getY() + y, loc.getZ() + i);
											l.getBlock().setType(Material.AIR);
										}
									}
								}

								//Randomized edges
								for (int z = randZ - 3; z <= randZ - 1; z++) {
									for (int y = -10; y <= 0; y++) {
										for (int x = -28; x <= 28; x++) {
											if (FastUtils.RANDOM.nextInt(2) == 0) {
												l.set(loc.getX() + x, loc.getY() + y, loc.getZ() + z);
												l.getBlock().setType(Material.AIR);
											}
										}
									}
								}

								this.cancel();
							}

							mX += 8;
							if (mX >= 28) {
								mZ--;
								mX = 0;
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);

					this.cancel();
				} else {
					//Particles are -2 z from north stand
					for (int x = -18; x <= 18; x += 2) {
						Location l = loc.clone().add(x, 0, 2);

						int count = 1;

						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(0, 0, 1), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(0, 0, -1), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(0, 0, -9), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(0, 0, -5), count, 1, 0.15, 1, 0.25);

						if (x % 4 == 0) {
							mWorld.spawnParticle(Particle.EXPLOSION_LARGE, l.clone().add(0, mY, -5), count / 3, 0.15, 0.15, 0.15, 0.05);
						}
					}
				}
				mT += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

	private void ruinWest() {
		Location loc = mWestStand.getLocation().clone();

		int randX = (int) FastUtils.randomDoubleInRange(9, 12);

		for (int z = -20; z <= 20; z++) {
			for (int x = -4; x <= randX; x++) {
				Location l = loc.clone().add(x, -1, z);
				if (l.getBlock().getType() != Material.AIR) {
					int rand = FastUtils.RANDOM.nextInt(6);
					switch (rand) {
					default:
					case 0:
					case 1:
					case 2:
						break;
					case 3:
					case 4:
						l.getBlock().setType(Material.MAGMA_BLOCK);
						break;
					case 5:
						l.getBlock().setType(Material.HONEY_BLOCK);
						break;
					}
				}
			}
		}

		new BukkitRunnable() {
			int mT = 0;
			int mY = 12;
			float mPitch = 0;
			@Override
			public void run() {
				if (mT % 10 == 0) {
					mWorld.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 2, mPitch);
				}
				mPitch += 0.025f;

				if (mT >= 20 * 4) {
					mWorld.playSound(loc.clone().add(5, 0, 0), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 3, 1);
					mY -= 2;
				}

				if (mT >= 20 * 5) {

					mWorld.playSound(loc, Sound.ENTITY_ENDER_DRAGON_GROWL, SoundCategory.HOSTILE, 3, 0);

					new BukkitRunnable() {
						int mTicks = 0;
						int mX = -6;
						int mZ = 0;
						@Override
						public void run() {
							mTicks++;

							Location l = loc.clone();

							if (mTicks % 10 == 0) {
								mWorld.playSound(l, Sound.ENTITY_GENERIC_EXPLODE, SoundCategory.HOSTILE, 5, 0);
								for (int i = -18; i <= 18; i += 2) {
									l.set(loc.getX() + mX, loc.getY(), loc.getZ() + i);
									mWorld.spawnParticle(Particle.EXPLOSION_HUGE, l, 1, 1, 1, 1);
								}
							}

							l.set(loc.getX(), loc.getY(), loc.getZ());

							//Slowly "decay" blocks in both directions of the platform
							for (int y = -10; y <= 0; y++) {
								for (int i = -(mZ + 8); i <= mZ + 8 && i <= 28; i++) {
									l.set(loc.getX(), loc.getY(), loc.getZ());
									l.add(mX, y, i);
									l.getBlock().setType(Material.AIR);
								}
							}

							if (mTicks >= 20 * 3 || mX >= randX) {

								//Clean up un-decayed blocks
								for (int i = mX; i <= randX; i++) {
									for (int y = -10; y <= 0; y++) {
										for (int j = -28; j <= 28; j++) {
											l.set(loc.getX() + i, loc.getY() + y, loc.getZ() + j);
											l.getBlock().setType(Material.AIR);
										}
									}
								}

								//Randomized edges
								for (int x = randX + 1; x <= randX + 3; x++) {
									for (int y = -10; y <= 0; y++) {
										for (int z = -28; z <= 28; z++) {
											if (FastUtils.RANDOM.nextInt(2) == 0) {
												l.set(loc.getX() + x, loc.getY() + y, loc.getZ() + z);
												l.getBlock().setType(Material.AIR);
											}
										}
									}
								}

								this.cancel();
							}

							mZ += 8;
							if (mZ >= 28) {
								mX++;
								mZ = 0;
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);

					this.cancel();
				} else {
					//Particles are -2 z from north stand
					for (int z = -18; z <= 18; z += 2) {
						Location l = loc.clone().add(-2, 0, z);

						int count = 1;

						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(-1, 0, 0), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(1, 0, 0), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(9, 0, 0), count, 1, 0.15, 1, 0.25);
						mWorld.spawnParticle(Particle.DAMAGE_INDICATOR, l.clone().add(5, 0, 0), count, 1, 0.15, 1, 0.25);

						if (z % 4 == 0) {
							mWorld.spawnParticle(Particle.EXPLOSION_LARGE, l.clone().add(5, mY, 0), count / 3, 0.15, 0.15, 0.15, 0.05);
						}
					}
				}
				mT += 5;
			}
		}.runTaskTimer(mPlugin, 0, 5);
	}

}
