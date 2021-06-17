package com.playmonumenta.plugins.bosses.spells.frostgiant;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Creature;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;

public class SpellGreatswordSlam extends Spell {

	private Plugin mPlugin;
	private LivingEntity mBoss;
	private double mDeg;
	//Number of sec. the cracked ice lasts
	private int mDuration;
	private static final Particle.DustOptions BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(66, 185, 245), 1.0f);
	private static final Particle.DustOptions GRAY_COLOR = new Particle.DustOptions(Color.fromRGB(156, 156, 156), 1.0f);

	private Location mStartLoc;

	//Starts deleting ice immediately when this is true
	private boolean mDeleteIce = false;

	public SpellGreatswordSlam(Plugin plugin, LivingEntity boss, int dur, double deg, Location startLoc) {
		mPlugin = plugin;
		mBoss = boss;
		mDuration = dur;
		mDeg = deg;
		mStartLoc = startLoc;
	}

	@Override
	public void run() {
		FrostGiant.freezeGolems(mBoss);
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 10, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 5, 1.5f);
		for (int deg = 0; deg < 360; deg += 5) {
			world.spawnParticle(Particle.REDSTONE, mBoss.getLocation().clone().add(3 * FastUtils.cos(deg), 0, 3 * FastUtils.sin(deg)), 1, 0.15, 0.15, 0.15, GRAY_COLOR);
		}
		Creature c = (Creature) mBoss;
		Pathfinder pathfinder = c.getPathfinder();

		pathfinder.stopPathfinding();

		Vector bossDir = mBoss.getLocation().getDirection();

		//Saves locations for places to convert from frosted ice back to its original block
		Map<Location, Material> oldBlocks = new HashMap<>();
		Map<Location, BlockData> oldData = new HashMap<>();

		Location loc = mBoss.getLocation();

		BukkitRunnable runnable1 = new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {

				mT += 10;
				if (mT > 20 * 3.5) {
					this.cancel();
				}

				for (int r = 0; r < 30; r += 2) {
					for (double degree = 90 - mDeg/2; degree <= 90 + mDeg/2; degree += 5) {
						double radian1 = Math.toRadians(degree);
						Vector vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
						vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

						Location l = loc.clone().add(vec);
						while (l.getBlock().getType() != Material.AIR && l.getBlockY() <= mStartLoc.getBlockY() + 3) {
							l.add(0, 1, 0);
						}
						world.spawnParticle(Particle.SPELL_WITCH, l, 1, 0.25, 0.25, 0.25, 0);
						world.spawnParticle(Particle.END_ROD, l, 1, 0.25, 0.25, 0.25, 0);
					}
				}

				if (mT <= 70) {
					mBoss.teleport(mBoss.getLocation().setDirection(bossDir));
				}
			}
		};
		runnable1.runTaskTimer(mPlugin, 0, 10);
		mActiveRunnables.add(runnable1);

		BukkitRunnable runnable2 = new BukkitRunnable() {
			int mT = 0;
			List<Player> mHitPlayers = new ArrayList<>();
			@Override
			public void run() {
				mT += 2;

				if (mT <= 30 && mT >= 20) {
					//Initiates the jump upwards
					mBoss.setVelocity(new Vector(0, 1.5, 0));
				} else if (mT >= 30) {
					if (!mBoss.isOnGround()) {
						//Initiates the slam down
						mBoss.setVelocity(new Vector(0, -1.5, 0));
					} else {
						//Creates the giant 30 degree cone rift of damage
						world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1, 0);
						BukkitRunnable runnable = new BukkitRunnable() {
							int mRadius = 0;
							@Override
							public void run() {

								mBoss.setVelocity(new Vector(0, 0, 0));
								pathfinder.stopPathfinding();

								if (mRadius >= 30) {
									this.cancel();
								}

								//In the current radius, makes a cone of frostsed ice and various other particles
								//If player is in trajectory (in bounding box), damage them and knock back
								Vector vec;
								List<BoundingBox> boxes = new ArrayList<BoundingBox>();
								for (double degree = 90 - mDeg/2; degree <= 90 + mDeg/2; degree += 5) {

									double radian1 = Math.toRadians(degree);
									vec = new Vector(FastUtils.cos(radian1) * mRadius, 0, FastUtils.sin(radian1) * mRadius);
									vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

									//Also have to clone location because of use in HashMap, can not optimize
									Location l = loc.clone().add(vec).add(0, -1, 0);
									//Move down one block to not overshoot, sometimes boss can stand on a single block, affects location
									if (l.getBlock().getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
										l.add(0, -1, 0);
									}
									//Once it leaves the arena, stop iterating
									if (l.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR
											|| l.distance(mStartLoc) > FrostGiant.fighterRange) {
										continue;
									}
									//If on bedrock or barriers, move up one to not replace that
									if (l.getBlock().getType() == Material.BEDROCK || l.getBlock().getType() == Material.BARRIER) {
										l.add(0, 1, 0);
									}

									//Put less frosted ice than the entire cone
									if (degree % 10 == 0) {
										if (l.getBlock().getType() != Material.FROSTED_ICE) {
											oldBlocks.put(l, l.getBlock().getType());
											oldData.put(l, l.getBlock().getBlockData());
										}
										l.getBlock().setType(Material.FROSTED_ICE);
										Ageable age = (Ageable) l.getBlock().getBlockData();
										age.setAge(1 + FastUtils.RANDOM.nextInt(3));
										l.getBlock().setBlockData(age);
									}

									//15 -> 3.65 lol
									BoundingBox box = BoundingBox.of(l, 1, 3.65, 1);
									boxes.add(box);

									FallingBlock fallBlock = world.spawnFallingBlock(l.add(0, 0.4, 0), Bukkit.createBlockData(Material.BLUE_ICE));
									fallBlock.setDropItem(false);
									fallBlock.setVelocity(new Vector(0, 0.4, 0));
									fallBlock.setHurtEntities(false);

									new BukkitRunnable() {
										@Override
										public void run() {
											if (!fallBlock.isDead() || fallBlock.isValid()) {
												fallBlock.remove();
												if (fallBlock.getLocation().getBlock().getType() == Material.BLUE_ICE) {
													fallBlock.getLocation().getBlock().setType(Material.AIR);
												}
											}
										}
									}.runTaskLater(mPlugin, 10);

									world.spawnParticle(Particle.CLOUD, l, 2, 0.15, 0.15, 0.15, 0.125);
									world.spawnParticle(Particle.CRIT, l, 8, 0.15, 0.15, 0.15, 0.7);
									world.spawnParticle(Particle.REDSTONE, l, 8, 0.15, 0.15, 0.15, BLUE_COLOR);
									if (degree > 85 && degree < 95 && mRadius % 5 == 0) {
										world.playSound(l, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0);
									}
								}
								for (Player player : PlayerUtils.playersInRange(loc, 40, true)) {
									if (player.getLocation().distance(mStartLoc) > FrostGiant.fighterRange) {
										continue;
									}

									for (BoundingBox box : boxes) {
										if (player.getBoundingBox().overlaps(box) && !mHitPlayers.contains(player)) {
											BossUtils.bossDamage(mBoss, player, 18, null);
											AbilityUtils.silencePlayer(player, 20 * 5);
											MovementUtils.knockAway(loc, player, 0f, 1.5f, false);
											mHitPlayers.add(player);
											break;
										}
									}
								}
								mRadius++;
							}
						};
						runnable.runTaskTimer(mPlugin, 0, 1);
						mActiveRunnables.add(runnable);

						FrostGiant.unfreezeGolems(mBoss);
						this.cancel();
					}
				} else {
					mBoss.setVelocity(new Vector(0, 0, 0));
					pathfinder.stopPathfinding();
				}
			}
		};
		runnable2.runTaskTimer(mPlugin, 0, 2);
		mActiveRunnables.add(runnable2);


		//Revert frosted ice after 60 seconds, and also damage players that step on it during that
		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {

				//Stop running after duration seconds
				if (mT >= 20 * mDuration || mBoss.isDead() || !mBoss.isValid() || mDeleteIce) {
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
				mT += 10;
			}
		}.runTaskTimer(mPlugin, 0, 10); //Every 0.5 seconds, check if player is on cone area damage
	}

	@Override
	public void cancel() {
		super.cancel();

		mDeleteIce = true;
	}

	@Override
	public int cooldownTicks() {
		return 7 * 20;
	}

}
