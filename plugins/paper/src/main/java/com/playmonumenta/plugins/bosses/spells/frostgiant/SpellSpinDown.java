package com.playmonumenta.plugins.bosses.spells.frostgiant;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Creature;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellSpinDown extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private Location mStartLoc;

	private boolean mCooldown = false;

	public SpellSpinDown(Plugin plugin, LivingEntity boss, Location loc) {
		mPlugin = plugin;
		mBoss = boss;
		mStartLoc = loc;
	}

	@Override
	public void run() {
		mCooldown = true;
		new BukkitRunnable() {

			@Override
			public void run() {
				mCooldown = false;
			}

		}.runTaskLater(mPlugin, 20 * 30);

		//Same as teleport method from the Frost Giant class, teleport with particle effect/sounds
		Location bossLoc = mBoss.getLocation();
		Location secondLoc = bossLoc.clone().add(0, 1, 0);
		World world = mStartLoc.getWorld();
		world.playSound(bossLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		world.spawnParticle(Particle.FIREWORKS_SPARK, secondLoc, 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.CLOUD, secondLoc, 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, bossLoc, 25, 0.2, 0, 0.2, 0.1);
		mBoss.teleport(mStartLoc.clone().add(0, 1, 0));
		world.playSound(bossLoc, Sound.ENTITY_WITHER_SHOOT, SoundCategory.HOSTILE, 1, 0f);
		world.spawnParticle(Particle.FIREWORKS_SPARK, secondLoc, 70, 0.25, 0.45, 0.25, 0.15);
		world.spawnParticle(Particle.SMOKE_LARGE, secondLoc, 35, 0.1, 0.45, 0.1, 0.15);
		world.spawnParticle(Particle.EXPLOSION_NORMAL, bossLoc, 25, 0.2, 0, 0.2, 0.1);

		Map<Location, Material> oldBlocks = new HashMap<>();
		Map<Location, BlockData> oldData = new HashMap<>();

		if (FrostGiant.mInstance != null) {
			FrostGiant.mInstance.mPreventTargetting = true;
		}

		final float vel = 45f;
		new BukkitRunnable() {
			int mTicks = 0;
			float mPitch = 0;

			@Override
			public void run() {
				//Spin boss using yaw teleport (may be laggy)
				World world = mBoss.getWorld();

				Creature c = (Creature) mBoss;
				Pathfinder pathfinder = c.getPathfinder();

				c.setTarget(null);
				pathfinder.stopPathfinding();

				Location loc = mBoss.getLocation();
				loc.setYaw(loc.getYaw() + vel);
				mBoss.teleport(loc);

				if (mTicks % 2 == 0 && mTicks <= 20 * 3) {
					world.playSound(mBoss.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, SoundCategory.HOSTILE, 4, mPitch);
					mPitch += 0.01f;

					world.spawnParticle(Particle.BLOCK_DUST, mBoss.getLocation().add(0, 2, 0), 5, 1, 0.35, 1, 0.25, Material.FROSTED_ICE.createBlockData());
				}

				//Shoots out ice blocks every third tick after 3 seconds
				if (mTicks >= 20 * 3 && mTicks % 3 == 0) {
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 4, 0.5f);

					//Velocity randomized of the frosted ice as a falling block
					FallingBlock block = world.spawnFallingBlock(loc, Bukkit.createBlockData(Material.FROSTED_ICE));
					block.setVelocity(new Vector(FastUtils.randomDoubleInRange(-1, 1), FastUtils.randomDoubleInRange(0.1, 0.75), FastUtils.randomDoubleInRange(-1, 1)));

					new BukkitRunnable() {
						int mTicks = 0;
						@Override
						public void run() {
							//Once the ice touches the ground or after 5 seconds, create a 4*4 square of damaging frosted ice (cracked)
							if (mTicks >= 20 * 5 || block.isOnGround()) {
								Location bLoc = block.getLocation();
								Material groundMat = bLoc.getBlock().getRelative(BlockFace.DOWN).getType();
								if (groundMat != Material.BEDROCK && groundMat != Material.AIR && groundMat != Material.BARRIER) {
									for (int r = 0; r <= 2; r++) {
										for (int x = -r; x < r; x++) {
											for (int z = -r; z < r; z++) {
												//Have to clone location becuase of use in HashMap
												Block b = bLoc.clone().add(x, -1, z).getBlock();
												Location l = b.getLocation();

												if (b.getType() == Material.FROSTED_ICE) {
													continue;
												}

												oldBlocks.put(l, b.getType());
												oldData.put(l, b.getBlockData());

												b.setType(Material.FROSTED_ICE);
												Ageable age = (Ageable) b.getBlockData();
												age.setAge(1 + FastUtils.RANDOM.nextInt(3));
												b.setBlockData(age);
											}
										}
									}
								}
								block.getLocation().getBlock().setType(Material.AIR);
								block.remove();

								this.cancel();
							}

							mTicks += 2;
						}
					}.runTaskTimer(mPlugin, 0, 2);
				}


				if (mTicks >= 20 * 6) {
					if (FrostGiant.mInstance != null) {
						FrostGiant.mInstance.mPreventTargetting = false;
					}

					this.cancel();
				}

				mTicks += 1;
			}
		}.runTaskTimer(mPlugin, 0, 1);

		//Revert frosted ice after 60 seconds, and also damage players that step on it during that
		new BukkitRunnable() {
			int mTicks = 0;
			@Override
			public void run() {

				//Stop running after 30 seconds
				if (mTicks >= 20 * 30 || mBoss.isDead() || !mBoss.isValid()) {
					new BukkitRunnable() {
						int mTicks = 0;
						Iterator<Map.Entry<Location, Material>> mBlocks = oldBlocks.entrySet().iterator();

						@Override
						public void run() {
							mTicks++;

							if (mTicks >= 20 * 3 || !mBlocks.hasNext()) {
								while (mBlocks.hasNext()) {
									Map.Entry<Location, Material> e = mBlocks.next();
									if (e.getKey().getBlock().getType() == Material.FROSTED_ICE) {
										e.getKey().getBlock().setType(e.getValue());
										if (oldData.containsKey(e.getKey())) {
											e.getKey().getBlock().setBlockData(oldData.get(e.getKey()));
										}
									}
									mBlocks.remove();
								}

								this.cancel();
							} else {
								//Remove 50 blocks per tick
								for (int i = 0; i < 50; i++) {
									if (!mBlocks.hasNext()) {
										break;
									}
									Map.Entry<Location, Material> e = mBlocks.next();
									//If doing shatter, wait for another tick
									if (e.getKey().getBlock().getType() == Material.CRIMSON_HYPHAE) {
										break;
									}
									if (e.getKey().getBlock().getType() == Material.FROSTED_ICE) {
										e.getKey().getBlock().setType(e.getValue());
										if (oldData.containsKey(e.getKey())) {
											e.getKey().getBlock().setBlockData(oldData.get(e.getKey()));
										}
									}
									mBlocks.remove();
								}
							}
						}
					}.runTaskTimer(mPlugin, 0, 1);

					this.cancel();
				}
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 40)) {
					if ((player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR || player.getLocation().getBlock().getType() != Material.AIR)
					    && (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == Material.FROSTED_ICE || player.getLocation().getBlock().getType() == Material.FROSTED_ICE)) {
						Vector vel = player.getVelocity();
						BossUtils.bossDamage(mBoss, player, 18, null);
						player.setVelocity(vel);
					}
				}
				mTicks += 10;
			}
		}.runTaskTimer(mPlugin, 0, 10); //Every 0.5 seconds, check if player is on cone area damage
	}

	@Override
	public int duration() {
		return 20 * 8;
	}

	@Override
	public boolean canRun() {
		return !mCooldown;
	}
}
