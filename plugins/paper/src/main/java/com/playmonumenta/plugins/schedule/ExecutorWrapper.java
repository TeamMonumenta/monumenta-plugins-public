package com.playmonumenta.plugins.schedule;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.playmonumenta.plugins.utils.MMLog;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

// Entire purpose of this class is to mimic a Bukkit/Bungee main "thread"
public class ExecutorWrapper {
	private final ScheduledExecutorService mExecutor;
	private final String mName;

	public ExecutorWrapper(String name) {
		mName = name;
		mExecutor = Executors.newSingleThreadScheduledExecutor(new ThreadFactoryBuilder().setNameFormat(name).setDaemon(true).build());
	}

	public void stop() {
		mExecutor.shutdown();
		try {
			if (mExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
				mExecutor.shutdownNow();
			}
		} catch (InterruptedException ex) {
			if (!mExecutor.isShutdown()) {
				mExecutor.shutdownNow();
			}
		}
	}

	public void schedule(Runnable runnable) {
		if (mExecutor.isShutdown()) {
			return;
		}
		schedule(new WrappedRunnable(runnable), 0, TimeUnit.MILLISECONDS);
	}

	public void schedule(Runnable runnable, long delay, TimeUnit unit) {
		if (mExecutor.isShutdown()) {
			return;
		}
		schedule(new WrappedRunnable(runnable), delay, unit);
	}

	public void schedule(WrappedRunnable runnable, long delay, TimeUnit unit) {
		if (mExecutor.isShutdown()) {
			return;
		}
		mExecutor.schedule(runnable, delay, unit);
	}

	public void scheduleRepeatingTask(Runnable runnable, long delay, long period, TimeUnit unit) {
		if (mExecutor.isShutdown()) {
			return;
		}
		mExecutor.scheduleAtFixedRate(new WrappedRunnable(runnable), delay, period, unit);
	}

	public void scheduleRepeatingTask(WrappedRunnable runnable, long delay, long period, TimeUnit unit) {
		if (mExecutor.isShutdown()) {
			return;
		}
		mExecutor.scheduleAtFixedRate(runnable, delay, period, unit);
	}

	public class WrappedRunnable implements Runnable {
		private final Runnable mTask;

		public WrappedRunnable(Runnable task) {
			this.mTask = task;
		}

		@Override
		public void run() {
			try {
				mTask.run();
			} catch (Exception ex) {
				MMLog.severe("Error executing task in ExecutorWrapper: " + mName, ex);
				ex.printStackTrace();
			}
		}
	}


}
