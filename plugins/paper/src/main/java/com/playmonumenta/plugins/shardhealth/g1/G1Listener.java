package com.playmonumenta.plugins.shardhealth.g1;

import com.google.common.collect.EvictingQueue;
import com.playmonumenta.plugins.Constants;
import com.playmonumenta.plugins.utils.MMLog;
import com.sun.management.GarbageCollectionNotificationInfo;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.management.NotificationBroadcaster;
import javax.management.openmbean.CompositeData;
import org.bukkit.Bukkit;

public class G1Listener {
	private static final String G1_OLDGEN = "G1 Old Gen";
	private static final String G1_SURVIVOR = "G1 Survivor Space";
	private static final String G1_EDEN = "G1 Eden Space";
	private static final int OLD_GEN_TICK_WINDOW = Constants.TICKS_PER_MINUTE * 20;
	private static final int YOUNG_GEN_TICK_WINDOW = Constants.TICKS_PER_MINUTE;
	private static final int CONCURRENT_TICK_WINDOW = Constants.TICKS_PER_MINUTE;
	private static final int OVERALL_TICK_WINDOW = Constants.TICKS_PER_MINUTE;

	private static class GcTracker {
		private final List<Integer> mCycleTicks = new ArrayList<>();
		private final EvictingQueue<Long> mFreedCount;
		private final EvictingQueue<Long> mTime;
		private final int mWindow;

		public GcTracker(int size, int window) {
			mFreedCount = EvictingQueue.create(size);
			mTime = EvictingQueue.create(size);
			this.mWindow = window;
		}

		public void tick() {
			final var currTick = Bukkit.getCurrentTick();
			mCycleTicks.removeIf(tickId -> tickId + mWindow <= currTick);
		}

		public void handleEvent(G1GcCycleInfo info) {
			final var currTick = Bukkit.getCurrentTick();
			mCycleTicks.add(currTick);
			mFreedCount.add(info.totalMemoryFreed());
			mTime.add(info.duration());
		}

		public int cyclesInWindow() {
			return mCycleTicks.size();
		}

		public double averageFreedCount() {
			return mFreedCount.stream().mapToLong(a -> a).average().orElse(0);
		}

		public double averageTime() {
			return mTime.stream().mapToLong(a -> a).average().orElse(0);
		}
	}

	// old gen stats, they should be relatively infrequent, but we want to keep track of when they occur
	private final GcTracker mOldGenTracker = new GcTracker(5, OLD_GEN_TICK_WINDOW);
	private final GcTracker mYoungGenTracker = new GcTracker(30, YOUNG_GEN_TICK_WINDOW);
	private final GcTracker mConcurrentTracker = new GcTracker(30, CONCURRENT_TICK_WINDOW);
	private final GcTracker mOverallTracker = new GcTracker(30, OVERALL_TICK_WINDOW);
	private final List<GcTracker> mAllTrackers = List.of(mOldGenTracker, mYoungGenTracker, mConcurrentTracker, mOverallTracker);

	private final ConcurrentLinkedQueue<G1GcCycleInfo> mEventQueue = new ConcurrentLinkedQueue<>();

	public void tick() {
		// evict old stuff
		mAllTrackers.forEach(GcTracker::tick);

		G1GcCycleInfo info;
		while ((info = mEventQueue.poll()) != null) {
			switch (info.type()) {
				case OLD -> mOldGenTracker.handleEvent(info);
				case YOUNG -> mYoungGenTracker.handleEvent(info);
				case CONCURRENT -> mConcurrentTracker.handleEvent(info);
				default -> {
				}
			}

			mOverallTracker.handleEvent(info);
		}
	}

	public G1GcHealth getHealth() {
		return new G1GcHealth(
			mOldGenTracker.cyclesInWindow(),
			mOldGenTracker.averageFreedCount(),
			mOldGenTracker.averageTime(),
			mYoungGenTracker.cyclesInWindow(),
			mYoungGenTracker.averageFreedCount(),
			mYoungGenTracker.averageTime(),
			mConcurrentTracker.cyclesInWindow(),
			mConcurrentTracker.averageFreedCount(),
			mConcurrentTracker.averageTime(),
			mOverallTracker.cyclesInWindow(),
			mOverallTracker.averageFreedCount(),
			mOverallTracker.averageTime()
		);
	}

	private static long getUsed(Map<String, MemoryUsage> map, String pool) {
		final var e = map.get(pool);
		if (e == null) {
			return 0;
		}
		return e.getUsed();
	}

	public boolean beginListen() {
		final var gcBean = ManagementFactory.getGarbageCollectorMXBeans();
		if (gcBean.stream().anyMatch(x -> !x.getName().startsWith("G1"))) {
			return false;
		}

		for (final var bean : gcBean) {
			((NotificationBroadcaster) bean).addNotificationListener((notification, o) -> {
				final var info = GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
				final var type = G1GcCycleInfo.G1GcCycleType.getType(info.getGcName());

				if (type.isEmpty()) {
					MMLog.warning("Unknown GC cycle type: " + info.getGcName());
				} else {
					final var preEden = getUsed(info.getGcInfo().getMemoryUsageBeforeGc(), G1_EDEN);
					final var preSurvivor = getUsed(info.getGcInfo().getMemoryUsageBeforeGc(), G1_SURVIVOR);
					final var preOld = getUsed(info.getGcInfo().getMemoryUsageBeforeGc(), G1_OLDGEN);

					final var postEden = getUsed(info.getGcInfo().getMemoryUsageAfterGc(), G1_EDEN);
					final var postSurvivor = getUsed(info.getGcInfo().getMemoryUsageAfterGc(), G1_SURVIVOR);
					final var postOld = getUsed(info.getGcInfo().getMemoryUsageAfterGc(), G1_OLDGEN);

					final var wrappedInfo = new G1GcCycleInfo(
						type.get(),
						(int) info.getGcInfo().get("GcThreadCount"),
						info.getGcInfo().getDuration(),
						preOld - postOld,
						preSurvivor - postSurvivor,
						preEden - postEden
					);

					MMLog.info("Recorded gc event: " + wrappedInfo);

					mEventQueue.add(wrappedInfo);
				}
			}, notification -> true, null);
		}

		return true;
	}
}
