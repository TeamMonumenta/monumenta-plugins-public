package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Transformation;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public class DisplayEntityUtils {

	public static BukkitTask groundBlockQuake(Location center, double radius, List<Material> possibleMaterials, Display.@Nullable Brightness brightness) {
		return groundBlockQuake(center, radius, possibleMaterials, brightness, 0.06);
	}

	public static BukkitTask groundBlockQuake(Location center, double radius, List<Material> possibleMaterials, Display.@Nullable Brightness brightness, double blockDensity) {
		BukkitRunnable runnable = new BukkitRunnable() {
			int mTicks = 0;
			final Map<Integer, ArrayList<Location>> mLocationDelays = new HashMap<>();
			final List<BlockDisplay> mAllDisplays = new ArrayList<>();

			@Override
			public void run() {
				for (int currRadius = 1; currRadius <= radius; currRadius++) {
					for (int blockCounter = 0; blockCounter < currRadius * 2 * Math.PI * blockDensity; blockCounter++) {
						int appearDelay = FastUtils.randomIntInRange(0, 3) * 2 + currRadius;
						double theta = FastUtils.randomDoubleInRange(0, Math.PI * 2);
						double finalRadius = currRadius + FastUtils.randomDoubleInRange(-0.5, 0.5);
						Location finalBlockLocation = center.clone().add(FastUtils.cos(theta) * finalRadius, 0, FastUtils.sin(theta) * finalRadius).toCenterLocation().add(0, -1.4, 0);
						ArrayList<Location> blocksThisTick = mLocationDelays.computeIfAbsent(appearDelay, key -> new ArrayList<>());
						if (!blocksThisTick.contains(finalBlockLocation)) {
							blocksThisTick.add(finalBlockLocation);
						}
					}
				}

				if (mLocationDelays.containsKey(mTicks)) {
					mLocationDelays.get(mTicks).forEach(l -> {
						BlockDisplay blockDisplay = center.getWorld().spawn(l.clone().add(-0.5, -0.3, -0.5), BlockDisplay.class);
						blockDisplay.setBlock(possibleMaterials.get(FastUtils.randomIntInRange(0, possibleMaterials.size() - 1)).createBlockData());
						if (brightness != null) {
							blockDisplay.setBrightness(new Display.Brightness(15, 15));
						}
						blockDisplay.setTransformation(new Transformation(new Vector3f(), new Quaternionf(), new Vector3f(1.0f, 1.0f, 1.0f), new Quaternionf()));
						blockDisplay.setInterpolationDuration(2);
						mAllDisplays.add(blockDisplay);

						BukkitRunnable runnable = new BukkitRunnable() {
							int mTicks = 0;
							final double mMaxHeight = FastUtils.randomDoubleInRange(0.5, 0.8);

							@Override
							public void run() {
								double currentHeight = mMaxHeight * (-0.04 * ((mTicks - 5) * (mTicks - 5)) + 1);
								blockDisplay.setTransformation(new Transformation(new Vector3f(0, (float)currentHeight, 0), blockDisplay.getTransformation().getLeftRotation(), blockDisplay.getTransformation().getScale(), blockDisplay.getTransformation().getRightRotation()));
								blockDisplay.setInterpolationDelay(-1);

								mTicks++;
								if (mTicks > 10) {
									this.cancel();
								}
							}

							@Override
							public synchronized void cancel() throws IllegalStateException {
								super.cancel();

								blockDisplay.remove();
							}
						};
						runnable.runTaskTimer(Plugin.getInstance(), 0, 1);
					});
				}

				mTicks++;
				if (mTicks > (radius + 1) * 2 + 8) {
					this.cancel();
				}
			}

			@Override
			public synchronized void cancel() throws IllegalStateException {
				super.cancel();

				mAllDisplays.forEach(Entity::remove);
			}
		};
		return runnable.runTaskTimer(Plugin.getInstance(), 0, 1);
	}
}
