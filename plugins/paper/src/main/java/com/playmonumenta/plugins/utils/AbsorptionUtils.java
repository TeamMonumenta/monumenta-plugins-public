package com.playmonumenta.plugins.utils;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.effects.DisplayableEffect;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import javax.annotation.Nullable;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

public class AbsorptionUtils {

	private static class AbsorptionInstances {
		// Concurrent as the data is read from other threads than the main server thread. Only modified on the main thread.
		final ConcurrentNavigableMap<Double, Integer> mAbsorptionInstances = new ConcurrentSkipListMap<>();

		private void addAbsorptionInstance(double amount, int duration) {
			Integer currentDuration = mAbsorptionInstances.get(amount);
			if (currentDuration == null || currentDuration < duration) {
				mAbsorptionInstances.put(amount, duration);
			}
		}

		// Removes expired absorption instances and returns the next maximum absorption amount
		private double elapse(int ticks) {
			Iterator<Map.Entry<Double, Integer>> iter = mAbsorptionInstances.entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<Double, Integer> entry = iter.next();
				double amount = entry.getKey();
				int newDuration = entry.getValue() - ticks;
				mAbsorptionInstances.put(amount, newDuration);

				if (newDuration <= 0) {
					iter.remove();
				}
			}

			return mAbsorptionInstances.isEmpty() ? 0 : mAbsorptionInstances.lastKey();
		}
	}

	// Concurrent as the data is read from other threads than the main server thread. Only modified on the main thread.
	private static final ConcurrentMap<LivingEntity, AbsorptionInstances> ABSORPTION_INFO_MAPPINGS = new ConcurrentHashMap<>();
	private static @Nullable BukkitRunnable ABSORPTION_INFO_TRACKER; // Effectively final

	private static final int TRACKER_PERIOD = 20;

	// Doesn't work for subtracting absorption because newAbsorption makes sure it never drops (in case absorption is higher than maxAmount)
	public static void addAbsorption(LivingEntity entity, double amount, double maxAmount, int duration) {
		double absorption = getAbsorption(entity);
		double newAbsorption = Math.min(absorption + amount, maxAmount);
		if (newAbsorption > absorption) {
			setAbsorption(entity, newAbsorption, duration);
		} else {
			// Even if we don't set absorption, update the tracker to get proper amount/duration stacking
			addAbsorptionInstance(entity, amount, duration);
		}
	}

	public static void subtractAbsorption(LivingEntity entity, double amount) {
		double absorption = getAbsorption(entity);
		double newAbsorption = Math.max(absorption - amount, 0);
		if (newAbsorption < absorption) {

			setAbsorption(entity, newAbsorption, -1);
		}
	}

	public static void setAbsorption(LivingEntity entity, double amount, int duration) {
		entity.setAbsorptionAmount(amount);
		addAbsorptionInstance(entity, amount, duration);
	}

	public static double getAbsorption(LivingEntity entity) {
		return entity.getAbsorptionAmount();
	}

	public static void addAbsorptionInstance(LivingEntity entity, double amount, int duration) {
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
						double newAmount = absorptionInstances.elapse(TRACKER_PERIOD);
						if (entity.hasPotionEffect(PotionEffectType.ABSORPTION)) {
							newAmount += (entity.getPotionEffect(PotionEffectType.ABSORPTION).getAmplifier() + 1) * 4;
						}

						if (newAmount < getAbsorption(entity)) {
							setAbsorption(entity, newAmount, -1);
						}

						if (entity.isDead() || !entity.isValid() || (entity instanceof Player && !((Player) entity).isOnline())) {
							iter.remove();
						}
					}
				}
			};
			ABSORPTION_INFO_TRACKER.runTaskTimer(Plugin.getInstance(), 0, TRACKER_PERIOD);
		}
	}

	public static List<AbsorptionDisplayable> getAbsorptionDisplayables(LivingEntity entity) {
		List<AbsorptionDisplayable> displayables = new ArrayList<>();
		AbsorptionInstances absorptionInstances = ABSORPTION_INFO_MAPPINGS.get(entity);
		if (absorptionInstances != null) {
			absorptionInstances.mAbsorptionInstances.forEach((amount, duration) -> displayables.add(new AbsorptionDisplayable(amount, duration)));
		}
		List<AbsorptionDisplayable> filteredDisplayables = new ArrayList<>();
		displayables.forEach(d -> {
			// Add displayables if they are not eclipsed by any we've already added
			if (filteredDisplayables.stream().noneMatch(d::isEclipsedBy)) {
				// Remove any that have been added already that d eclipses
				// If o is equivalent to d, we would not get to this point
				filteredDisplayables.removeIf(o -> o.isEclipsedBy(d));

				filteredDisplayables.add(d);
			}
		});

		return filteredDisplayables;
	}

	// this does not do any tracking, it is purely for display & sorting
	public static class AbsorptionDisplayable implements DisplayableEffect {

		private final double mAmount;
		private final int mDuration;

		public AbsorptionDisplayable(double amount, int duration) {
			mAmount = amount;
			mDuration = duration;
		}

		@Override
		public int getDisplayPriority() {
			return mDuration;
		}

		@Override
		public String getDisplay() {
			return ChatColor.YELLOW + "" + StringUtils.to2DP(mAmount) + " Absorption " + ChatColor.GRAY + "" + StringUtils.intToMinuteAndSeconds(mDuration / 20);
		}


		public boolean isEclipsedBy(AbsorptionDisplayable other) {
			return this.mAmount <= other.mAmount && this.mDuration <= other.mDuration;
		}
	}

}
