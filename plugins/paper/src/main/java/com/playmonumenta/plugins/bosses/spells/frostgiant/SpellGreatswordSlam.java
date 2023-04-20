package com.playmonumenta.plugins.bosses.spells.frostgiant;

import com.destroystokyo.paper.entity.Pathfinder;
import com.playmonumenta.plugins.bosses.TemporaryBlockChangeManager;
import com.playmonumenta.plugins.bosses.bosses.FrostGiant;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.AbilityUtils;
import com.playmonumenta.plugins.utils.DamageUtils;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.MovementUtils;
import com.playmonumenta.plugins.utils.PlayerUtils;
import com.playmonumenta.plugins.utils.VectorUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Creature;
import org.bukkit.entity.FallingBlock;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

public class SpellGreatswordSlam extends Spell {

	private static final Material ICE_TYPE = Material.FROSTED_ICE;

	private final Plugin mPlugin;
	private final LivingEntity mBoss;
	private final double mDeg;
	//Number of sec. the cracked ice lasts
	private final int mDuration;
	private static final Particle.DustOptions BLUE_COLOR = new Particle.DustOptions(Color.fromRGB(66, 185, 245), 1.0f);
	private static final Particle.DustOptions GRAY_COLOR = new Particle.DustOptions(Color.fromRGB(156, 156, 156), 1.0f);

	private final Location mStartLoc;

	private final List<Block> mChangedBlocks = new ArrayList<>();

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
			new PartialParticle(Particle.REDSTONE, mBoss.getLocation().clone().add(3 * FastUtils.cos(deg), 0, 3 * FastUtils.sin(deg)), 1, 0.15, 0.15, 0.15, GRAY_COLOR).spawnAsEntityActive(mBoss);
		}
		Creature c = (Creature) mBoss;
		Pathfinder pathfinder = c.getPathfinder();

		pathfinder.stopPathfinding();

		Vector bossDir = mBoss.getLocation().getDirection();

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
					for (double degree = 90 - mDeg / 2; degree <= 90 + mDeg / 2; degree += 5) {
						double radian1 = Math.toRadians(degree);
						Vector vec = new Vector(FastUtils.cos(radian1) * r, 0, FastUtils.sin(radian1) * r);
						vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

						Location l = loc.clone().add(vec);
						while (l.getBlock().getType() != Material.AIR && l.getBlockY() <= mStartLoc.getBlockY() + 3) {
							l.add(0, 1, 0);
						}
						new PartialParticle(Particle.SPELL_WITCH, l, 1, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
						new PartialParticle(Particle.END_ROD, l, 1, 0.25, 0.25, 0.25, 0).spawnAsEntityActive(mBoss);
					}
				}

				if (mT <= 70) {
					mBoss.teleport(mBoss.getLocation().setDirection(bossDir));
				}
			}
		};
		runnable1.runTaskTimer(mPlugin, 0, 10);
		mActiveRunnables.add(runnable1);

		mChangedBlocks.clear();

		BukkitRunnable runnable2 = new BukkitRunnable() {
			int mT = 0;
			final List<Player> mHitPlayers = new ArrayList<>();

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

								//In the current radius, makes a cone of frosted ice and various other particles
								//If player is in trajectory (in bounding box), damage them and knock back
								Vector vec;
								List<BoundingBox> boxes = new ArrayList<>();
								for (double degree = 90 - mDeg / 2; degree <= 90 + mDeg / 2; degree += 5) {

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
									if ((l.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR)
										    || l.distance(mStartLoc) > FrostGiant.fighterRange) {
										continue;
									}
									//If on bedrock or barriers, move up one to not replace that
									if (l.getBlock().getType() == Material.BEDROCK || l.getBlock().getType() == Material.BARRIER) {
										l.add(0, 1, 0);
									}

									//Put less frosted ice than the entire cone
									if (degree % 10 == 0) {
										Block block = l.getBlock();
										if (block.getType() != SpellFrostRift.RIFT_BLOCK_TYPE
											    && TemporaryBlockChangeManager.INSTANCE.changeBlock(block, ICE_TYPE, 20 * mDuration - mRadius + FastUtils.randomIntInRange(0, 10))) {
											mChangedBlocks.add(block);
											Ageable age = (Ageable) block.getBlockData();
											age.setAge(1 + FastUtils.RANDOM.nextInt(3));
											block.setBlockData(age);
										}
									}

									//15 -> 3.65 lol
									BoundingBox box = BoundingBox.of(l, 1, 3.65, 1);
									boxes.add(box);

									FallingBlock fallBlock = world.spawnFallingBlock(l.add(0, 0.4, 0), Bukkit.createBlockData(Material.BLUE_ICE));
									fallBlock.setDropItem(false);
									EntityUtils.disableBlockPlacement(fallBlock);
									fallBlock.setVelocity(new Vector(0, 0.4, 0));
									fallBlock.setHurtEntities(false);

									new BukkitRunnable() {
										@Override
										public void run() {
											if (fallBlock.isValid()) {
												fallBlock.remove();
											}
										}
									}.runTaskLater(mPlugin, 20);

									new PartialParticle(Particle.CLOUD, l, 2, 0.15, 0.15, 0.15, 0.125).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.CRIT, l, 8, 0.15, 0.15, 0.15, 0.7).spawnAsEntityActive(mBoss);
									new PartialParticle(Particle.REDSTONE, l, 8, 0.15, 0.15, 0.15, BLUE_COLOR).spawnAsEntityActive(mBoss);
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
											DamageUtils.damage(mBoss, player, DamageType.MAGIC, 18, null, false, true, "Greatsword Slam");
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

		// Damage players standing on frosted ice for the duration
		mActiveTasks.add(new BukkitRunnable() {
			int mT = 0;

			@Override
			public void run() {

				//Stop running after duration seconds
				if (mT >= 20 * mDuration || mBoss.isDead() || !mBoss.isValid()) {
					this.cancel();
				}
				for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), 40, false)) {
					if ((player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR || player.getLocation().getBlock().getType() != Material.AIR)
						    && (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() == ICE_TYPE || player.getLocation().getBlock().getType() == ICE_TYPE)) {
						DamageUtils.damage(mBoss, player, DamageType.MAGIC, 18, null, false, false, "Frosted Ice");
					}
				}
				mT += 10;
			}
		}.runTaskTimer(mPlugin, 0, 10)); //Every 0.5 seconds, check if player is on cone area damage
	}

	@Override
	public void cancel() {
		super.cancel();

		TemporaryBlockChangeManager.INSTANCE.revertChangedBlocks(mChangedBlocks, ICE_TYPE);
		mChangedBlocks.clear();
	}

	@Override
	public int cooldownTicks() {
		return 7 * 20;
	}

}
