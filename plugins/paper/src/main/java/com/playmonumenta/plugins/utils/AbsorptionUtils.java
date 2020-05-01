package com.playmonumenta.plugins.utils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;

public class AbsorptionUtils {

	private static class AbsorptionInstances {
		final SortedMap<Float, Integer> mAbsorptionInstances = new TreeMap<Float, Integer>();

		private void addAbsorptionInstance(float amount, int duration) {
			Integer currentDuration = mAbsorptionInstances.get(amount);
			if (currentDuration == null || currentDuration < duration) {
				mAbsorptionInstances.put(amount, duration);
			}
		}

		// Removes expired absorption instances and returns the next maximum absorption amount
		private float elapse(int ticks) {
			Iterator<SortedMap.Entry<Float, Integer>> iter = mAbsorptionInstances.entrySet().iterator();
			while (iter.hasNext()) {
				SortedMap.Entry<Float, Integer> entry = iter.next();
				float amount = entry.getKey();
				int newDuration = entry.getValue() - ticks;
				mAbsorptionInstances.put(amount, newDuration);

				if (newDuration <= 0) {
					iter.remove();
				}
			}

			return mAbsorptionInstances.isEmpty() ? 0 : mAbsorptionInstances.lastKey();
		}
	}

	private static final Map<LivingEntity, AbsorptionInstances> ABSORPTION_INFO_MAPPINGS = new HashMap<LivingEntity, AbsorptionInstances>();
	private static BukkitRunnable ABSORPTION_INFO_TRACKER;		// Effectively final

	private static final int TRACKER_PERIOD = 20;

	private static Method handleMethod;
	private static Method getAbsorptionMethod;
	private static Method setAbsorptionMethod;

	// Doesn't work for subtracting absorption because newAbsorption makes sure it never drops (in case absorption is higher than maxAmount)
	public static void addAbsorption(LivingEntity entity, float amount, float maxAmount, int duration) {
		float absorption = getAbsorption(entity);
		float newAbsorption = Math.min(absorption + amount, maxAmount);
		if (newAbsorption > absorption) {
			setAbsorption(entity, newAbsorption, duration);
		} else {
			// Even if we don't set absorption, update the tracker to get proper amount/duration stacking
			addAbsorptionInstance(entity, amount, duration);
		}
	}

	public static void subtractAbsorption(LivingEntity entity, float amount) {
		float absorption = getAbsorption(entity);
		float newAbsorption = Math.max(absorption - amount, 0);
		if (newAbsorption < absorption) {
			setAbsorption(entity, newAbsorption, -1);
		}
	}

	public static void setAbsorption(LivingEntity entity, float amount, int duration) {
		cacheReflectionMethods(entity);

		try {
			setAbsorptionMethod.invoke(handleMethod.invoke(entity), amount);
			addAbsorptionInstance(entity, amount, duration);
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
		}
	}

	public static float getAbsorption(LivingEntity entity) {
		cacheReflectionMethods(entity);

		try {
			return (Float) getAbsorptionMethod.invoke(handleMethod.invoke(entity));
		} catch (IllegalAccessException | InvocationTargetException e) {
			e.printStackTrace();
			return 0;
		}
	}

	public static void addAbsorptionInstance(LivingEntity entity, float amount, int duration) {
		if (duration >= 0) {
			initializeTracker();

			AbsorptionInstances absorptionInstances = ABSORPTION_INFO_MAPPINGS.get(entity);
			if (absorptionInstances == null) {
				absorptionInstances = new AbsorptionInstances();
				ABSORPTION_INFO_MAPPINGS.put(entity, absorptionInstances);
			}

			absorptionInstances.addAbsorptionInstance(amount, duration);
		}
	}

	private static void cacheReflectionMethods(LivingEntity entity) {
		if (handleMethod == null || getAbsorptionMethod == null || setAbsorptionMethod == null) {
			try {
				handleMethod = entity.getClass().getMethod("getHandle");
				Object handle = handleMethod.invoke(entity);
				getAbsorptionMethod = handle.getClass().getMethod("getAbsorptionHearts");
				setAbsorptionMethod = handle.getClass().getMethod("setAbsorptionHearts", float.class);
			} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				e.printStackTrace();
			}
		}
	}

	private static void initializeTracker() {
		if (ABSORPTION_INFO_TRACKER == null || ABSORPTION_INFO_TRACKER.isCancelled()) {
			ABSORPTION_INFO_TRACKER = new BukkitRunnable() {
				@Override
				public void run() {
					Iterator<Map.Entry<LivingEntity, AbsorptionInstances>> iter = ABSORPTION_INFO_MAPPINGS.entrySet().iterator();

					while (iter.hasNext()) {
						Map.Entry<LivingEntity, AbsorptionInstances> entry = iter.next();
						LivingEntity entity = entry.getKey();
						AbsorptionInstances absorptionInstances = entry.getValue();
						float newAmount = absorptionInstances.elapse(TRACKER_PERIOD);
						if (entity.hasPotionEffect(PotionEffectType.ABSORPTION)) {
							newAmount += (entity.getPotionEffect(PotionEffectType.ABSORPTION).getAmplifier() + 1) * 4;
						}

						if (newAmount < getAbsorption(entity)) {
							setAbsorption(entity, newAmount, -1);
						}

						if (entity.isDead() || !entity.isValid() || entity instanceof Player && !((Player) entity).isOnline()) {
							iter.remove();
						}
					}
				}
			};
			ABSORPTION_INFO_TRACKER.runTaskTimer(Plugin.getInstance(), 0, TRACKER_PERIOD);
		}
	}

}
