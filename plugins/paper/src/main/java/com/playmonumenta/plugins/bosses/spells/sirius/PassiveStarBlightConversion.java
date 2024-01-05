package com.playmonumenta.plugins.bosses.spells.sirius;


import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.playmonumenta.plugins.bosses.bosses.sirius.Sirius;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.particle.PPLine;
import com.playmonumenta.plugins.utils.FastUtils;
import com.playmonumenta.plugins.utils.FileUtils;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.*;
import java.util.logging.Level;
import javax.annotation.Nullable;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class PassiveStarBlightConversion extends Spell {
	private boolean mOnCooldown;
	private Sirius mSirius;
	public double mDeltaX;
	public double mDeltaY;
	public double mDeltaZ;
	public boolean[][] mBlighted;
	public Location mCornerOne;
	public Location mCornerTwo;
	private Map<String, List<BlockData>> mRestore;
	private @Nullable Map<String, List<BlockData>> mBlight;
	private boolean mLoaded;
	private static final EnumSet<Material> IGNORED_MATS = EnumSet.of(
		Material.COMMAND_BLOCK,
		Material.CHAIN_COMMAND_BLOCK,
		Material.REPEATING_COMMAND_BLOCK,
		Material.BEDROCK
	);
	private static final int COOLDOWN = 10 * 20;

	public PassiveStarBlightConversion(Sirius sirius) {
		mSirius = sirius;
		mCornerOne = mSirius.mBoss.getLocation().add(43, 49, 58);
		mCornerTwo = mSirius.mBoss.getLocation().subtract(75, 15, 60);
		mDeltaX = Math.abs(mCornerOne.getX() - mCornerTwo.getX());
		mDeltaY = Math.abs(mCornerOne.getY() - mCornerTwo.getY());
		mDeltaZ = Math.abs(mCornerOne.getZ() - mCornerTwo.getZ());
		mRestore = new HashMap<>();
		mBlighted = new boolean[(int) mDeltaX + 1][(int) mDeltaZ + 1];
		mOnCooldown = false;
		mBlight = null;
		//gets every blocks state and stores it
		for (double x = mCornerTwo.getX(); x < mCornerOne.getX(); x++) {
			for (double z = mCornerTwo.getZ(); z < mCornerOne.getZ(); z++) {
				List<BlockData> blockData = new ArrayList<>();
				for (double y = mCornerTwo.getY(); y < mCornerOne.getY(); y++) {
					Location loc = new Location(mSirius.mBoss.getWorld(), x, y, z);
					if (!IGNORED_MATS.contains(loc.getBlock().getType())) {
						blockData.add(loc.getBlock().getBlockData());
					}
				}
				double mRealX = mCornerOne.getX() - x;
				double mRealZ = mCornerOne.getZ() - z;
				mRestore.put("x" + mRealX + "z" + mRealZ, blockData);
			}
		}
		mLoaded = false;
		try {
			//ascyn inside to stop lagging the shard
			createmBlight();
		} catch (Exception e) {
			MMLog.severe("Sirius failed to find starblight arena blockstates. Please report this.");
		}
	}

	@Override
	public void run() {
		if (!mOnCooldown) {
			//random seed so it follows a different pattern.
			mOnCooldown = true;
			Bukkit.getScheduler().runTaskLater(com.playmonumenta.plugins.Plugin.getInstance(), () -> mOnCooldown = false, COOLDOWN);
			convertBehind();
			mSirius.updateCollisionBox(0);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

	public void convertColumn(double x, double z) {
		if (mLoaded && mBlight != null) {
			double realX = Math.abs(mCornerOne.x() - x);
			double realZ = Math.abs(mCornerOne.z() - z);
			if (realX >= 0 && realZ >= 0) {
				x = realX;
				z = realZ;
			}
			x = Math.floor(x);
			z = Math.floor(z);
			if (x < mDeltaX && z < mDeltaZ) {
				mBlighted[(int) x][(int) z] = true;
				List<BlockData> mBlightData = mBlight.get("x" + x + "z" + z);
				if (mBlightData != null) {
					int blightCount = mBlightData.size() - 1;
					for (int i = 0; i < mCornerOne.getY() - mCornerTwo.getY() && !mBlightData.isEmpty(); i++) {
						Location loc = new Location(mSirius.mBoss.getWorld(), mCornerOne.getX() - x, mCornerOne.getY() - i - 2, mCornerOne.getZ() - z);
						if (!IGNORED_MATS.contains(loc.getBlock().getType()) && blightCount >= 0) {
							loc.getBlock().setBlockData(mBlightData.get(blightCount));
							loc.getBlock().getState().update();
							blightCount--;
						}
					}
				}
			}
		}
	}

	public void convertBehind() {
		if (!mSirius.mBoss.isDead() && !mSirius.mDamagePhase) {
			for (double x = mCornerOne.getX(); x > mSirius.mBoss.getLocation().getX(); x--) {
				for (double z = mCornerOne.getZ(); z > mCornerTwo.getZ(); z--) {
					convertColumn(x, z);
				}
			}
			mSirius.updateCollisionBox(0);
		}
	}


	public void restoreAll() {
		for (double x = mCornerTwo.getX(); x < mCornerOne.getX(); x++) {
			restoreLine((int) (mCornerOne.getX() - x));
		}
	}

	public void restoreLine(int relativeX) {
		double x = mCornerOne.getX() - relativeX;
		for (double z = mCornerTwo.getZ(); z < mCornerOne.getZ(); z++) {
			List<BlockData> blockData = mRestore.get("x" + ((double) relativeX) + "z" + (mCornerOne.getZ() - z));
			if (blockData != null) {
				int mCount = 0;
				for (double y = mCornerTwo.getY(); y < mCornerOne.getY(); y++) {
					Location loc = new Location(mSirius.mBoss.getWorld(), x, y, z);
					if (!IGNORED_MATS.contains(loc.getBlock().getType()) && mCount < blockData.size()) {
						loc.getBlock().setBlockData(blockData.get(mCount));
						loc.getBlock().getState().update();
						mBlighted[relativeX][(int) (mCornerOne.getZ() - z)] = false;
						mCount++;
					}
				}
			}
		}
	}

	public void restoreFullCircle(Location center, int radius) {
		for (int x = (int) Math.min(Math.abs((center.getX() - radius) - mCornerOne.getX()), mDeltaX);
			 x > Math.max(Math.abs((center.getX() + radius) - mCornerOne.getX()), 0); x--) {
			double mRealX = x;
			for (double z = Math.min(Math.abs((center.getZ() - radius)), mCornerTwo.getZ()); z < Math.max(Math.abs((center.getZ() + radius)), mCornerOne.getZ()); z++) {
				double mRealZ = mCornerOne.getZ() - z;
				List<BlockData> blockData = mRestore.get("x" + mRealX + "z" + mRealZ);
				if (blockData != null && mBlighted[x][(int) mRealZ]) {
					int mCount = 0;
					for (double y = mCornerTwo.getY(); y < mCornerOne.getY(); y++) {
						Location loc = new Location(mSirius.mBoss.getWorld(), mCornerOne.getX() - mRealX, y, z);
						if (!IGNORED_MATS.contains(loc.getBlock().getType()) && mCount < blockData.size()) {
							if (getHorizontalDistance(loc, center) <= radius * radius) {
								loc.getBlock().setBlockData(blockData.get(mCount));
								loc.getBlock().getState().update();
								mBlighted[x][(int) mRealZ] = false;
								mCount++;
							}
						}
						if (mCount == blockData.size()) {
							break;
						}
					}
				}
			}
		}

	}

	//get Horizontal distance from loc2 to loc1. The result is not square rooted
	private double getHorizontalDistance(Location loc1, Location loc2) {
		return Math.pow(loc1.getX() - loc2.getX(), 2) + Math.pow(loc1.getZ() - loc2.getZ(), 2);
	}

	public void convertSphere(int radius, Location center) {
		double mXVal = Math.min(center.getX() + radius, mCornerOne.getX());
		double mZVal = Math.max(center.getZ() - radius, mCornerTwo.getZ());
		for (double x = mXVal; x >= (center.getX() - radius) && center.getX() - radius > mCornerTwo.getX(); x--) {
			for (double z = mZVal; z < center.getZ() + radius && z > mCornerTwo.getZ(); z++) {
				if (getHorizontalDistance(center, new Location(mSirius.mBoss.getWorld(), x, center.y(), z)) < radius * radius) {
					convertColumn(x, z);
				}
			}
		}

	}


	public void convertHalfSphere(int radius) {
		double mXVal = Math.min(mSirius.mBoss.getLocation().getX() + radius, mCornerOne.getX());
		double mZVal = Math.max(mSirius.mBoss.getLocation().getZ() - radius, mCornerTwo.getZ());
		for (double x = mXVal; x >= (mSirius.mBoss.getLocation().getX() + 1); x--) {
			for (double z = mZVal; z < mSirius.mBoss.getLocation().getZ() + radius && z > mCornerTwo.getZ(); z++) {
				if (getHorizontalDistance(mSirius.mBoss.getLocation(), new Location(mSirius.mBoss.getWorld(), x, mSirius.mBoss.getLocation().y(), z)) < radius * radius) {
					convertColumn(x, z);
				}
			}
		}
	}

	public void convertPartialSphere(int radius, Location center) {
		double mXVal = Math.min(center.getX() + radius, mCornerOne.getX());
		double mZVal = Math.max(center.getZ() - radius, mCornerTwo.getZ());
		for (double x = mXVal; x >= (center.getX() - radius) && center.getX() - radius > mCornerTwo.getX(); x--) {
			for (double z = mZVal; z < center.getZ() + radius && z > mCornerTwo.getZ(); z++) {
				double horizontalDistance = getHorizontalDistance(center, new Location(mSirius.mBoss.getWorld(), x, center.y(), z));
				if (horizontalDistance < radius * radius) {
					//decrease the odds of it blighting as it expands
					if (FastUtils.randomDoubleInRange(0, 1) < (1 - (horizontalDistance / ((radius + 1) * (radius + 1))))) {
						convertColumn(x, z);
					}
				}
			}
		}

	}

	public void convertLine(double xOne, double xTwo, double middleZ, int width, Location orbLoc) {
		World world = orbLoc.getWorld();
		world.playSound(orbLoc, Sound.ENTITY_GUARDIAN_HURT, SoundCategory.HOSTILE, 2, 1.4f);
		world.playSound(orbLoc, Sound.ENTITY_ENDER_DRAGON_HURT, SoundCategory.HOSTILE, 0.7f, 0.4f);
		world.playSound(orbLoc, Sound.ENTITY_WARDEN_SONIC_BOOM, SoundCategory.HOSTILE, 0.7f, 1.4f);
		world.playSound(orbLoc, Sound.ENTITY_ALLAY_HURT, SoundCategory.HOSTILE, 0.7f, 0.1f);
		world.playSound(orbLoc, Sound.BLOCK_RESPAWN_ANCHOR_CHARGE, SoundCategory.HOSTILE, 0.9f, 0.1f);
		world.playSound(orbLoc, Sound.ITEM_TRIDENT_RETURN, SoundCategory.HOSTILE, 2, 0.4f);
		for (double x = xOne; x > xTwo && xTwo > mCornerTwo.getX(); x--) {
			for (double z = middleZ - width / 2.0; z < middleZ + width / 2.0; z++) {
				//reduce particle count while achieving the same effect.
				if (FastUtils.randomIntInRange(0, 1) == 0) {
					//top
					for (double y = mCornerOne.getY(); y > mCornerTwo.getY(); y -= 1) {
						Location loc = new Location(mSirius.mBoss.getWorld(), x, y, z);
						if (loc.getBlock().isSolid() && !loc.clone().subtract(0, 1, 0).getBlock().isSolid()) {
							new PPLine(Particle.REDSTONE, orbLoc, loc).data(new Particle.DustOptions(Color.fromRGB(0, 128, 128), 1.75f)).countPerMeter(0.3).delta(0.25).spawnAsBoss();
							break;
						}
					}
					//bottom
					for (double y = mCornerTwo.getY(); y < mCornerOne.getY(); y += 1) {
						Location loc = new Location(mSirius.mBoss.getWorld(), x, y, z);
						if (loc.getBlock().isSolid() && !loc.clone().add(0, 1, 0).getBlock().isSolid()) {
							new PPLine(Particle.REDSTONE, orbLoc, loc).data(new Particle.DustOptions(Color.fromRGB(0, 128, 128), 1.75f)).countPerMeter(0.3).delta(0.25).spawnAsBoss();
							break;
						}
					}
				}
				convertColumn(x, z);
			}
		}
	}

	public void blightArena(List<Location> safezone, double safezoneradius, int timetoconvertarena, int timetoconvertoutside, Plugin plugin) {
		Location mLineTwo = mSirius.mBoss.getLocation().clone();
		double linesperconversion = ((Math.abs(mSirius.mBoss.getLocation().getX() - mSirius.mSpawnCornerTwo.getX()) / ((double) timetoconvertarena)) * 10.0);
		double linesperoutsideconversion = (Math.abs(mSirius.mSpawnCornerTwo.getX() - mCornerTwo.getX()) / ((double) timetoconvertoutside) * 10.0);

		new BukkitRunnable() {
			int mTicks = 0;
			boolean mExtraBlight = false;

			@Override
			public void run() {
				for (Player p : mSirius.getPlayersInArena(false)) {
					p.playSound(p, Sound.ENTITY_WARDEN_AMBIENT, SoundCategory.HOSTILE, 0.6f, 2f);
					p.playSound(p, Sound.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, SoundCategory.HOSTILE, 0.4f, 1.2f);
				}
				for (double x = mLineTwo.getX(); x > mLineTwo.getX() - linesperconversion && mLineTwo.getX() > mCornerTwo.getX(); x--) {
					for (double z = mCornerOne.getZ(); z > mCornerTwo.getZ(); z--) {
						Location loc = new Location(mSirius.mBoss.getWorld(), x, mSirius.mBoss.getLocation().getY(), z);
						boolean safeloc = false;
						for (Location safe : safezone) {
							if (getHorizontalDistance(safe, loc) <= safezoneradius * safezoneradius) {
								safeloc = true;
							}
						}
						if (!safeloc) {
							convertColumn(x, z);
						}

					}
				}
				if (mTicks < timetoconvertarena) {
					mLineTwo.subtract(linesperconversion, 0, 0);
				}
				if (mTicks >= timetoconvertarena && mExtraBlight) {
					mLineTwo.subtract(linesperoutsideconversion, 0, 0);
				}
				if (mTicks >= timetoconvertarena && !mExtraBlight) {
					mExtraBlight = true;
					for (double x = mLineTwo.getX(); x > mSirius.mCornerTwo.getX(); x--) {
						for (double z = mCornerOne.getZ(); z > mCornerTwo.getZ(); z--) {
							Location loc = new Location(mSirius.mBoss.getWorld(), mLineTwo.getX(), mLineTwo.getY(), z);
							boolean safeloc = false;
							for (Location safe : safezone) {
								if (getHorizontalDistance(loc, safe) <= safezoneradius * safezoneradius) {
									safeloc = true;
								}
							}
							if (!safeloc) {
								convertColumn(x, z);
							}
						}
					}
					mLineTwo.setX(mSirius.mCornerTwo.getX() - linesperoutsideconversion);
				}
				if (mTicks >= timetoconvertarena + timetoconvertoutside) {
					//make sure its all nice and blighted
					if (mDeltaX - 1 % 2 != 0) {
						for (double x = mLineTwo.getX(); x > mCornerTwo.getX(); x--) {
							for (double z = mCornerOne.getZ(); z > mCornerTwo.getZ(); z--) {
								Location loc = new Location(mSirius.mBoss.getWorld(), mLineTwo.getX(), mLineTwo.getY(), z);
								boolean safeloc = false;
								for (Location safe : safezone) {
									if (getHorizontalDistance(loc, safe) <= safezoneradius * safezoneradius) {
										safeloc = true;
									}
								}
								if (!safeloc) {
									convertColumn(x, z);
								}
							}
						}
					}
					this.cancel();
				}
				mTicks += 10;
			}
		}.runTaskTimer(plugin, 0, 10);

	}

	//loads from a file
	private void createmBlight() {
		Bukkit.getScheduler().runTaskAsynchronously(com.playmonumenta.plugins.Plugin.getInstance(), () -> {
			mBlight = new HashMap<>();
			String blight = null;
			try {
				blight = FileUtils.readFile(com.playmonumenta.plugins.Plugin.getInstance().getDataFolder().getPath() + "/SiriusBlightArena.json");
				Gson gson = new Gson();
				JsonObject data = gson.fromJson(blight, JsonObject.class);
				JsonArray blockstateparse = data.get("blight").getAsJsonArray();
				for (JsonElement list : blockstateparse) {
					JsonObject toParse = list.getAsJsonObject();
					for (String key : toParse.keySet()) {
						List<BlockData> blockdata = new ArrayList<>();
						String line = toParse.get(key).getAsString();
						for (String s : line.split(", ")) {
							blockdata.add(Bukkit.createBlockData(s));
						}
						mBlight.put(key, blockdata);
					}
				}
				mLoaded = true;
			} catch (Exception e) {
				com.playmonumenta.plugins.Plugin.getInstance().getLogger().log(Level.FINER, "StarblightConversion: File failed to be found" + e.toString());
			}

		});
	}

}
