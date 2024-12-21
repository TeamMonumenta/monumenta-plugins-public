package com.playmonumenta.plugins.particle;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.playmonumenta.plugins.utils.MMLog;
import com.playmonumenta.plugins.utils.NmsUtils;
import com.playmonumenta.plugins.utils.ScoreboardUtils;
import java.lang.ref.WeakReference;
import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

public final class ParticleManager {
	private static final AtomicBoolean mIsTicking = new AtomicBoolean(false);
	private static final AtomicBoolean mIsFlushing = new AtomicBoolean(false);
	private static final AtomicBoolean mIsFlushQueued = new AtomicBoolean(false);
	private static final AtomicLong mLastTick = new AtomicLong(0);
	public static final int MAX_PARTIAL_PARTICLE_VALUE = 200;

	private static final ThreadFactory mThreadFactory = new ThreadFactoryBuilder().setNameFormat("PartialParticle Thread").setDaemon(true).build();
	public static final ExecutorService mParticleExecutor = Executors.newSingleThreadScheduledExecutor(mThreadFactory);
	private static final ScheduledExecutorService mFlushingExecutor = Executors.newSingleThreadScheduledExecutor(mThreadFactory);

	// Use this to force tasks to run off the main thread
	public static void runOffMainThread(Runnable runnable) {
		if (mParticleExecutor.isShutdown() || !Bukkit.isPrimaryThread()) {
			runnable.run();
			return;
		}
		mParticleExecutor.execute(new WrappedRunnable(runnable));
	}

	// Specifically to avoid allocatiing a new object
	public static <T> void runOffMainThread(Consumer<T> consumer, Function<Boolean, T> getter) {
		if (mParticleExecutor.isShutdown() || !Bukkit.isPrimaryThread()) {
			consumer.accept(getter.apply(false));
			return;
		}
		final T object = getter.apply(true);
		mParticleExecutor.execute(new WrappedRunnable(() -> consumer.accept(object)));
	}

	public static class WrappedRunnable implements Runnable {
		private final Runnable mTask;

		public WrappedRunnable(Runnable task) {
			this.mTask = task;
		}

		@Override
		public void run() {
			try {
				mTask.run();
			} catch (Exception ex) {
				MMLog.severe("Error executing particle task: ", ex);
			}
		}
	}

	private static void stopExecutors() {
		try {
			mParticleExecutor.shutdown();
			if (mParticleExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				mParticleExecutor.shutdownNow();
			}
		} catch (InterruptedException ex) {
			if (!mParticleExecutor.isShutdown()) {
				mParticleExecutor.shutdownNow();
			}
		}

		try {
			mFlushingExecutor.shutdown();
			if (mFlushingExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				mFlushingExecutor.shutdownNow();
			}
		} catch (InterruptedException ex) {
			if (!mFlushingExecutor.isShutdown()) {
				mFlushingExecutor.shutdownNow();
			}
		}
	}

	public static class ParticlePacket {
		public final WeakReference<Player> mPlayer;
		public final UUID mPlayerUUID;
		public final Particle mParticle;
		public final double mX;
		public final double mY;
		public final double mZ;
		public final int mCount;
		public final double mOffsetX;
		public final double mOffsetY;
		public final double mOffsetZ;
		public final double mExtra;
		@Nullable public final Object mData;
		public final boolean mForce;

		public ParticlePacket(Particle particle, Player player, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable Object data, boolean force) {
			mPlayer = new WeakReference<Player>(player);
			mPlayerUUID = player.getUniqueId();
			mParticle = particle;
			mX = x;
			mY = y;
			mZ = z;
			mCount = count;
			mOffsetX = offsetX;
			mOffsetY = offsetY;
			mOffsetZ = offsetZ;
			mExtra = extra;
			mData = data;
			mForce = force;
		}

		@Override
		public String toString() {
			return "ParticlePacket{" +
				"mPlayerUUID=" + mPlayerUUID +
				", mParticle=" + mParticle +
				", mX=" + mX +
				", mY=" + mY +
				", mZ=" + mZ +
				", mCount=" + mCount +
				", mOffsetX=" + mOffsetX +
				", mOffsetY=" + mOffsetY +
				", mOffsetZ=" + mOffsetZ +
				", mExtra=" + mExtra +
				", mData=" + mData +
				", mForce=" + mForce +
				'}';
		}
	}

	public static class ParticleSettings {
		public final Map<ParticleCategory, Double> mParticleMultipliers = new EnumMap<>(ParticleCategory.class);

		public ParticleSettings(Player player) {
			for (ParticleCategory category : ParticleCategory.values()) {
				mParticleMultipliers.put(category, initParticleMultiplier(player, category));
			}
		}

		// this should only be called on the main thread
		private static double initParticleMultiplier(Player player, ParticleCategory category) {
			String objectiveName = category.mObjectiveName;
			if (objectiveName == null) {
				// Defaults to 100% when value is missing
				return 1;
			}
			int interpretedValue = ScoreboardUtils.getScoreboardValue(player, objectiveName).orElse(100);
			int clampedValue = Math.min(interpretedValue, MAX_PARTIAL_PARTICLE_VALUE);
			clampedValue = Math.max(clampedValue, 0);
			return clampedValue / 100d;
		}
	}

	private static final Map<UUID, ParticleSettings> mParticleSettings = new ConcurrentHashMap<>();
	private static final Queue<ParticlePacket> mPendingParticles = new ConcurrentLinkedQueue<>();
	// private static final Queue<PartialParticleBuilder> mPendingParticles = new ConcurrentLinkedQueue<>();


	private ParticleManager() {
	}

	// this should only be called on the main thread
	public static void updateParticleSettings(Player player) {
		mParticleSettings.put(player.getUniqueId(), new ParticleManager.ParticleSettings(player));
	}

	public static void removeParticleSettings(Player player) {
		mParticleSettings.remove(player.getUniqueId());
	}

	// This is thread safe
	public static double getParticleMultiplier(Player player, ParticleCategory category) {
		ParticleSettings settings = mParticleSettings.get(player.getUniqueId());
		if (settings == null) {
			return 1;
		}
		Double multiplier = settings.mParticleMultipliers.get(category);
		if (multiplier == null) {
			return 1;
		}
		return multiplier.doubleValue();
	}

	public static void init() {
		if (mIsTicking.get()) {
			return;
		}
		mIsTicking.set(true);
	}

	public static void shutdown() {
		stopExecutors();
		mIsTicking.set(false);
	}

	public static void tick() {
		flush();
	}

	public static void flush() {
		if (mIsFlushQueued.get() || mIsFlushing.get() || mPendingParticles.isEmpty()) {
			return;
		}
		long currentTime = System.nanoTime();
		if (currentTime - mLastTick.get() < TimeUnit.MILLISECONDS.toNanos(10)) {
			return;
		}
		mLastTick.set(currentTime);
		mIsFlushQueued.set(true);
		mFlushingExecutor.execute(ParticleManager::flushNow);
	}

	// Should only be called from an async thread (the flushing executor)
	private static void flushNow() {
		if (mIsFlushing.get()) {
			return;
		}
		mIsFlushing.set(true);
		ParticlePacket particle = null;
		try {
			while ((particle = mPendingParticles.poll()) != null) {
				Player player = particle.mPlayer.get();
				if (player == null) {
					continue;
				}
				sendParticle(particle.mParticle, player, particle.mX, particle.mY, particle.mZ, particle.mCount, particle.mOffsetX, particle.mOffsetY, particle.mOffsetZ, particle.mExtra, particle.mData, particle.mForce);
			}
		} catch (Exception ex) {
			if (particle != null) {
				MMLog.severe("Error flushing particles (potentially malformed particle?): \n" + particle.toString(), ex);
			} else {
				MMLog.severe("Error flushing particles: ", ex);
			}
		} finally {
			mPendingParticles.clear();
			mIsFlushing.set(false);
			mIsFlushQueued.set(false);
		}
	}

	public static void addParticleToQueue(PartialParticleBuilder builder) {
		builder.spawn();
		// mPendingParticles.add(builder);
	}

	public static <T> void addParticleToQueue(Particle particle, Player reciever, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable T data, boolean force) {
		mPendingParticles.add(new ParticlePacket(particle, reciever, x, y, z, count, offsetX, offsetY, offsetZ, extra, data, force));
		// ParticleManager.sendParticle(particle, reciever, x, y, z, count, offsetX, offsetY, offsetZ, extra, data, force);
	}

	public static <T> boolean sendParticle(Particle particle, Player reciever, double x, double y, double z, int count, double offsetX, double offsetY, double offsetZ, double extra, @Nullable T data, boolean force) {
		int num = NmsUtils.getVersionAdapter().sendParticle(particle, reciever, x, y, z, count, offsetX, offsetY, offsetZ, extra, data, force);
		return num == 0;
	}
}
