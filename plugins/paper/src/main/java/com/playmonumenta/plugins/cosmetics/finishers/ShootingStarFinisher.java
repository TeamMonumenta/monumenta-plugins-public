package com.playmonumenta.plugins.cosmetics.finishers;

import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.DisplayEntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class ShootingStarFinisher implements EliteFinisher {

	public static final String NAME = "Shooting Star";
	private static final Particle.DustOptions YELLOW = new Particle.DustOptions(Color.fromRGB(237, 198, 26), 1.0f);

	@Override
	public void run(Player p, Entity killedMob, Location loc) {
		World world = p.getWorld();
		Location eyeLocation = p.getEyeLocation();
		Vector direction = eyeLocation.getDirection().setY(0).normalize();
		Vector right = direction.clone().crossProduct(new Vector(0, 1, 0)).normalize();
		int units = 15;
		Location[] starPoints = new Location[10];
		Location[] shootStart = new Location[5];
		Location[] shootEnd = new Location[5];
		Location[] current = new Location[5];
		Vector[] directionStar = new Vector[5];
		Vector[] increment = new Vector[5];
		int[] redStar = new int[5];
		int[] greenStar = new int[5];
		int[] blueStar = new int[5];

		//boolean check1 = Math.round(p.getLocation().getX()) % 10 == 0;

		// framework for more shooting stars later perhaps
		for (int i = 0; i < 9; i++) {
			starPoints[i] = newStarLocation(loc, direction, right);
		}
		shootStart[0] = newShootStart(loc, direction, right);
		shootEnd[0] = newShootEnd(loc, direction, right);
		for (int i = 1; i <= 4; i++) {
			shootStart[i] = shootStart[0];
			shootEnd[i] = shootEnd[0];
		}
		for (int i = 0; i < 5; i++) {
			current[i] = shootStart[i].clone();
			directionStar[i] = shootEnd[i].clone().toVector().subtract(shootStart[i].toVector());
			increment[i] = directionStar[i].divide(new Vector(units, units, units));
			redStar[i] = 79;
			greenStar[i] = 45;
			blueStar[i] = 200;
		}
		ItemStack shootingStar = DisplayEntityUtils.generateRPItem(Material.NETHER_STAR, "Shooting Star");
		ItemDisplay itemDisplay = (ItemDisplay) current[0].getWorld().spawnEntity(current[0], EntityType.ITEM_DISPLAY);

		new BukkitRunnable() {
			int mTicks = 0;
			final int mRStep = (223 - 79) / 15;
			final int mGStep = (252 - 45) / 15;
			final int mBStep = (252 - 200) / 15;

			@Override
			public void run() {

				if (mTicks >= 0 && mTicks < 20) {
					createStar(starPoints[0], right, p);
				} else if (mTicks >= 10 && mTicks < 30) {
					createStar(starPoints[1], right, p);
				} else if (mTicks >= 20 && mTicks < 40) {
					createStar(starPoints[2], right, p);
				} else if (mTicks >= 30 && mTicks < 50) {
					createStar(starPoints[3], right, p);
				} else if (mTicks >= 40 && mTicks < 60) {
					createStar(starPoints[4], right, p);
				}
				if (mTicks >= 15 && mTicks < 35) {
					createStar(starPoints[5], right, p);
				} else if (mTicks >= 25 && mTicks < 45) {
					createStar(starPoints[6], right, p);
				} else if (mTicks >= 35 && mTicks < 55) {
					createStar(starPoints[7], right, p);
				} else if (mTicks >= 45 && mTicks < 65) {
					createStar(starPoints[8], right, p);
				}
				if (mTicks == 20) {
					loc.getWorld().playSound(loc, Sound.ENTITY_FIREWORK_ROCKET_LAUNCH, SoundCategory.PLAYERS, 0.75f, 0.25f);
					itemDisplay.setItemStack(shootingStar);
					float yaw = (float) Math.toDegrees(Math.atan2(direction.getZ(), direction.getX())) - 90;
					float pitch = (float) Math.toDegrees(Math.atan2(direction.getY(), Math.sqrt(direction.getX() * direction.getX() + direction.getZ() * direction.getZ())));
					itemDisplay.setRotation(yaw, pitch);
				}
				if (mTicks >= 20 && mTicks < 36) {
					itemDisplay.teleport(current[0]);
					Particle.DustOptions mSTAR = new Particle.DustOptions(Color.fromRGB(redStar[0], greenStar[0], blueStar[0]), 5.0f);
					new PartialParticle(Particle.REDSTONE, current[0], 1, 0, 0, 0, 0).data(mSTAR).minimumCount(1).spawnAsPlayerActive(p);
					current[0].add(increment[0]);
					redStar[0] += mRStep;
					greenStar[0] += mGStep;
					blueStar[0] += mBStep;
				}
				if (mTicks == 36) {
					itemDisplay.remove();
				}

				if (mTicks < 65) {
					playMusic(mTicks, world, loc);
					new PartialParticle(Particle.ELECTRIC_SPARK, loc, 25, 6, 6, 6).spawnAsPlayerActive(p);
				}

				if (mTicks >= 65) {
					this.cancel();
				}
				mTicks++;
			}
		}.runTaskTimer(Plugin.getInstance(), 0, 1);
	}

	public static Location newStarLocation(Location loc, Vector direction, Vector right) {
		Location tempLoc = loc.clone();
		float starHeight = FastUtils.randomFloatInRange(3, 9);
		float starFar = FastUtils.randomFloatInRange(4, 10);
		float starOff = FastUtils.randomFloatInRange(-8, 8);
		tempLoc.add(0, starHeight, 0);
		tempLoc.add(direction.clone().multiply(starFar));
		tempLoc.add(right.clone().multiply(starOff));
		return tempLoc;
	}

	public static Location newShootStart(Location loc, Vector direction, Vector right) {
		Location tempLoc = loc.clone();
		float starHeight = FastUtils.randomFloatInRange(14, 16);
		float starFar = FastUtils.randomFloatInRange(4, 10);
		float starOff = FastUtils.randomFloatInRange(4, 6);
		tempLoc.add(0, starHeight, 0);
		tempLoc.add(direction.clone().multiply(starFar));
		tempLoc.add(right.clone().multiply(starOff));
		return tempLoc;
	}

	public static Location newShootEnd(Location loc, Vector direction, Vector right) {
		Location tempLoc = loc.clone();
		float starHeight = FastUtils.randomFloatInRange(5, 7);
		float starFar = FastUtils.randomFloatInRange(4, 10);
		float starOff = FastUtils.randomFloatInRange(-6, -4);
		tempLoc.add(0, starHeight, 0);
		tempLoc.add(direction.clone().multiply(starFar));
		tempLoc.add(right.clone().multiply(starOff));
		return tempLoc;
	}

	public static void createStar(Location starLoc, Vector right, Player p) {
		Location loc1 = starLoc.clone().add(0, 1.1, 0);
		new PartialParticle(Particle.REDSTONE, loc1, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc2 = starLoc.clone().add(0, 0.8, 0);
		loc2.add(right.clone().multiply(0.05));
		new PartialParticle(Particle.REDSTONE, loc2, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc3 = starLoc.clone().add(0, 0.8, 0);
		loc3.add(right.clone().multiply(-0.05));
		new PartialParticle(Particle.REDSTONE, loc3, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc4 = starLoc.clone().add(0, 0.6, 0);
		loc4.add(right.clone().multiply(0.1));
		new PartialParticle(Particle.REDSTONE, loc4, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc5 = starLoc.clone().add(0, 0.6, 0);
		loc5.add(right.clone().multiply(-0.1));
		new PartialParticle(Particle.REDSTONE, loc5, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc6 = starLoc.clone().add(0, 0.5, 0);
		loc6.add(right.clone().multiply(0.25));
		new PartialParticle(Particle.REDSTONE, loc6, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc7 = starLoc.clone().add(0, 0.5, 0);
		loc7.add(right.clone().multiply(-0.25));
		new PartialParticle(Particle.REDSTONE, loc7, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc8 = starLoc.clone().add(0, 0.4, 0);
		loc8.add(right.clone().multiply(0.1));
		new PartialParticle(Particle.REDSTONE, loc8, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc9 = starLoc.clone().add(0, 0.4, 0);
		loc9.add(right.clone().multiply(-0.1));
		new PartialParticle(Particle.REDSTONE, loc9, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc10 = starLoc.clone().add(0, 0.2, 0);
		loc10.add(right.clone().multiply(0.05));
		new PartialParticle(Particle.REDSTONE, loc10, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc11 = starLoc.clone().add(0, 0.2, 0);
		loc11.add(right.clone().multiply(-0.05));
		new PartialParticle(Particle.REDSTONE, loc11, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
		Location loc12 = starLoc.clone().add(0, -0.1, 0);
		new PartialParticle(Particle.REDSTONE, loc12, 1).data(YELLOW).minimumCount(1).spawnAsPlayerActive(p);
	}

	public static void playMusic(int ticks, World world, Location loc) {
		switch (ticks) {
			case 9:
				world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.CS19);
				world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.FS12);
				break;
			case 15, 35:
				world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.B17);
				break;
			case 21, 24, 41, 44:
				world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.A15);
				break;
			case 29, 58:
				world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.CS19);
				break;
			case 50:
				world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.CS19);
				world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D8);
				break;
			case 54:
				world.playSound(loc, Sound.BLOCK_NOTE_BLOCK_HARP, SoundCategory.PLAYERS, 1f, Constants.NotePitches.D20);
				break;
			default:
				break;
		}
	}

	@Override
	public Material getDisplayItem() {
		return Material.NETHER_STAR;
	}
}
