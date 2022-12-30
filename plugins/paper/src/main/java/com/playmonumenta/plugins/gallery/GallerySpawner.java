package com.playmonumenta.plugins.gallery;

import com.playmonumenta.libraryofsouls.Soul;
import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.BossManager;
import com.playmonumenta.plugins.gallery.bosses.GenericGalleryMobBoss;
import com.playmonumenta.plugins.integrations.LibraryOfSoulsIntegration;
import java.util.HashSet;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class GallerySpawner {
	public static final String TAG_STRING = "GallerySpawner";

	private final @NotNull Entity mArmorStandSpawner;
	private final @NotNull SpawnerDirection mSpawningDirection;
	private final @NotNull Location mSpawningLocation;

	private final @NotNull String mName;
	private boolean mActivated;

	private int mLastTimeSpawned = 0;

	public GallerySpawner(@NotNull Entity armorStand, @NotNull SpawnerDirection direction, @NotNull String name, boolean active) {
		mArmorStandSpawner = armorStand;
		mSpawningDirection = direction;
		mSpawningLocation = armorStand.getLocation();
		mName = name;
		mActivated = active;
	}


	public String getName() {
		return mName;
	}

	public Location getLocation() {
		return mSpawningLocation;
	}

	public void setActive(boolean active) {
		mActivated = active;
		save();
	}

	public boolean isActive() {
		return mActivated;
	}

	public @Nullable LivingEntity spawn(String pool, float velocity, boolean shouldGiveTag) {
		final Location loc = mSpawningLocation.clone().add(mSpawningDirection.getOffset().clone().multiply(3));
		LivingEntity mobSpawned = null;
		Map<Soul, Integer> mobsPool = LibraryOfSoulsIntegration.getPool(pool);
		if (mobsPool != null) {
			for (Map.Entry<Soul, Integer> mob : mobsPool.entrySet()) {
				mobSpawned = (LivingEntity) mob.getKey().summon(loc);
				mobSpawned.setAI(false);
				if (shouldGiveTag) {
					mobSpawned.addScoreboardTag(GalleryManager.MOB_TAG_FROM_SPAWNER);
				}
				break;
			}
		}
		if (mobSpawned == null) {
			return null;
		}
		LivingEntity finalMobSpawned = mobSpawned;
		int delay = 0;
		while (mLastTimeSpawned >= GalleryManager.ticks + delay) {
			delay += 15;
		}
		mLastTimeSpawned = GalleryManager.ticks + delay;
		new BukkitRunnable() {
			final Location mLoc = loc;
			final double mDistancePerTick = mSpawningLocation.distance(mLoc) / ((20 * 1.5) / velocity) * 1.5;
			final double mMaxTicks = ((20 * 1.5) / velocity);
			int mTimes = 0;

			@Override
			public void run() {
				mLoc.add(mSpawningDirection.getOffset().clone().multiply(-mDistancePerTick));
				mLoc.setDirection(mSpawningDirection.getOffset().clone().multiply(-1));
				finalMobSpawned.teleport(mLoc);

				if (mTimes >= mMaxTicks) {
					finalMobSpawned.setAI(true);
					try {
						BossManager.getInstance().createBossInternal(finalMobSpawned, new GenericGalleryMobBoss(GalleryManager.mPlugin, finalMobSpawned));
					} catch (Exception e) {
						//this SHOULD NEVER happen
						GalleryUtils.printDebugMessage("Catch an exception while creating GenericGalleryMobBoss. Reason: " + e.getMessage());
						e.printStackTrace();
					}
					cancel();
				}
				mTimes++;
			}
		}.runTaskTimer(Plugin.getInstance(), delay, 1);

		return mobSpawned;
	}



	public void save() {
		new HashSet<>(mArmorStandSpawner.getScoreboardTags()).forEach(s -> {
			if (s.startsWith(TAG_STRING + "-")) {
				mArmorStandSpawner.removeScoreboardTag(s);
			}
		});
		//TAG_STRING-name-direction-Active/Deactivated
		mArmorStandSpawner.addScoreboardTag(TAG_STRING);
		mArmorStandSpawner.addScoreboardTag(TAG_STRING + "-" + mName + "-" + mSpawningDirection.name() + "-" + (mActivated ? "Active" : "Deactivated"));

	}


	public static @Nullable GallerySpawner fromEntity(Entity entity) {
		try {
			for (String tag : entity.getScoreboardTags()) {
				if (tag.startsWith(TAG_STRING + "-")) {
					String[] split = tag.split("-");
					String name = split[1];
					SpawnerDirection direction = SpawnerDirection.valueOf(split[2]);
					boolean active = split[3].equalsIgnoreCase("active");
					return new GallerySpawner(entity, direction, name, active);
				}
			}
		} catch (Exception e) {
			GalleryUtils.printDebugMessage("Catch an exception while converting spawner to Object. Reason " + e.getMessage());
			e.printStackTrace();
		}

		return null;
	}


	public enum SpawnerDirection {
		UP(new Vector(0, -1, 0)), DOWN(new Vector(0, 1, 0)), NORTH(new Vector(0, 0, 1)), SOUTH(new Vector(0, 0, -1)), WEST(new Vector(1, 0, 0)), EAST(new Vector(-1, 0, 0));

		private final Vector mOffset;

		SpawnerDirection(Vector vector) {
			mOffset = vector;
		}

		public Vector getOffset() {
			return mOffset;
		}

	}
}
