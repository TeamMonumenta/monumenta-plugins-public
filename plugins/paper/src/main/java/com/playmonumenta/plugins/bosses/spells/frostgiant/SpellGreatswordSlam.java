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
import com.playmonumenta.plugins.utils.LocationUtils;
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

	public SpellGreatswordSlam(Plugin plugin, LivingEntity boss, int dur, double deg) {
		mPlugin = plugin;
		mBoss = boss;
		mDuration = dur;
		mDeg = deg;
	}

	@Override
	public void run() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_RAVAGER_ROAR, SoundCategory.HOSTILE, 10, 1);
		world.playSound(mBoss.getLocation(), Sound.ENTITY_BLAZE_SHOOT, SoundCategory.HOSTILE, 5, 1.5f);
		for (int deg = 0; deg < 360; deg += 5) {
			world.spawnParticle(Particle.REDSTONE, mBoss.getLocation().clone().add(3 * FastUtils.cos(deg), 0, 3 * FastUtils.sin(deg)), 1, 0.15, 0.15, 0.15, GRAY_COLOR);
		}
		Creature c = (Creature) mBoss;
		Pathfinder pathfinder = c.getPathfinder();

		pathfinder.stopPathfinding();

		Player target = null;
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), FrostGiant.fighterRange)) {
			c.setTarget(player);
			target = player;
			break;
		}

		if (target != null) {
			Vector dir = LocationUtils.getDirectionTo(target.getLocation(), mBoss.getLocation()).setY(0).normalize();
			mBoss.teleport(mBoss.getLocation().setDirection(dir));
		}

		//Saves locations for places to convert from frosted ice back to its original block
		Map<Location, Material> oldBlocks = new HashMap<>();
		Map<Location, BlockData> oldData = new HashMap<>();

		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {
				mT += 2;
				if (mT <= 20 && mT >= 10) {
					//Initiates the jump upwards
					mBoss.setVelocity(mBoss.getVelocity().setY(2));
				}
				if (mT >= 20) {
					if (!mBoss.isOnGround()) {
						//Initiates the slam down
						mBoss.setVelocity(mBoss.getVelocity().setY(-2));
					} else {
						//Creates the giant 30 degree cone rift of damage
						Location loc = mBoss.getLocation();
						world.playSound(loc, Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.HOSTILE, 1, 0);
						new BukkitRunnable() {
							int mRadius = 0;
							@Override
							public void run() {

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
									vec = VectorUtils.rotateXAxis(vec, 0);
									vec = VectorUtils.rotateYAxis(vec, loc.getYaw());

									//Also have to clone location because of use in HashMap, can not optimize
									Location l = loc.clone().add(vec).add(0, -1, 0);
									//Move down one block to not overshoot, sometimes boss can stand on a single block, affects location
									if (l.getBlock().getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR) {
										l.add(0, -1, 0);
									}
									//Once it leaves the arena, stop iterating
									if (l.getBlock().getRelative(BlockFace.UP).getType() == Material.AIR && l.getBlock().getRelative(BlockFace.DOWN).getType() == Material.AIR) {
										continue;
									}
									//If on bedrock or barriers, move up one to not replace that
									if (l.getBlock().getType() == Material.BEDROCK || l.getBlock().getType() == Material.BARRIER) {
										l.add(0, 1, 0);
									}
									Location tempLoc = l.clone();
									for (int y = 1; y <= 5; y++) {
										tempLoc.setY(l.getY() + y);
										tempLoc.getBlock().setType(Material.AIR);
									}
									if (l.getBlock().getType() != Material.FROSTED_ICE) {
										oldBlocks.put(l, l.getBlock().getType());
										oldData.put(l, l.getBlock().getBlockData());
									}
									l.getBlock().setType(Material.FROSTED_ICE);
									Ageable age = (Ageable) l.getBlock().getBlockData();
									age.setAge(1 + FastUtils.RANDOM.nextInt(3));
									l.getBlock().setBlockData(age);

									BoundingBox box = BoundingBox.of(l, 1, 1.65, 1);
									boxes.add(box);

									FallingBlock fallBlock = world.spawnFallingBlock(l, Bukkit.createBlockData(Material.BLUE_ICE));
									fallBlock.setDropItem(false);
									fallBlock.setVelocity(new Vector(0, 0.2, 0));
									fallBlock.setHurtEntities(false);

									world.spawnParticle(Particle.CLOUD, l, 2, 0.15, 0.15, 0.15, 0.125);
									world.spawnParticle(Particle.CRIT, l, 8, 0.15, 0.15, 0.15, 0.7);
									world.spawnParticle(Particle.REDSTONE, l, 8, 0.15, 0.15, 0.15, BLUE_COLOR);
									if (degree > 85 && degree < 95 && mRadius % 5 == 0) {
										world.playSound(l, Sound.BLOCK_GLASS_BREAK, SoundCategory.HOSTILE, 3, 0);
									}
								}
								for (Player player : PlayerUtils.playersInRange(loc, 40)) {
									for (BoundingBox box : boxes) {
										if (player.getBoundingBox().overlaps(box) &&
										    (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getType() != Material.AIR || player.getLocation().getBlock().getType() != Material.AIR)) {
											BossUtils.bossDamagePercent(mBoss, player, 0.4);
											AbilityUtils.silencePlayer(player, 20 * 5);
											MovementUtils.knockAway(loc, player, 3f, 1f, false);
											break;
										}
									}
								}
								mRadius++;
							}
						}.runTaskTimer(mPlugin, 0, 2);
						this.cancel();
					}
				}
			}
		}.runTaskTimer(mPlugin, 0, 2);


		//Revert frosted ice after 60 seconds, and also damage players that step on it during that
		new BukkitRunnable() {
			int mT = 0;
			@Override
			public void run() {

				//Stop running after duration seconds
				if (mT >= 20 * mDuration || mBoss.isDead() || !mBoss.isValid()) {
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
						BossUtils.bossDamage(mBoss, player, 12, null);
						player.setVelocity(vel);
					}
				}
				mT += 10;
			}
		}.runTaskTimer(mPlugin, 0, 10); //Every 0.5 seconds, check if player is on cone area damage

	}

	@Override
	public int duration() {
		return 20 * 8;
	}

}
