package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Location;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

public class SpellDelayedAction extends Spell {
	@FunctionalInterface
	public interface StartAction {
		/**
		 * Action called when first running
		 *
		 * @param loc Location specified at launch
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface WarningAction {
		/**
		 * Action to warn player of pending action
		 * Called every tick while delaying
		 *
		 * @param loc Location specified at launch
		 */
		void run(Location loc);
	}

	@FunctionalInterface
	public interface EndAction {
		/**
		 * Action to run at the end of the delay
		 *
		 * @param loc Location specified at launch
		 */
		void run(Location loc);
	}

	private Plugin mPlugin;
	private Location mLoc;
	private int mDelayTicks;
	private StartAction mStartAction;
	private WarningAction mWarningAction;
	private EndAction mEndAction;

	public SpellDelayedAction(Plugin plugin, Location loc, int delayTicks,
	                          StartAction start, WarningAction warning, EndAction end) {
		mPlugin = plugin;
		mLoc = loc;
		mDelayTicks = delayTicks;
		mStartAction = start;
		mWarningAction = warning;
		mEndAction = end;
	}

	@Override
	public void run() {
		BukkitRunnable runnable = new BukkitRunnable() {
			private int mTicks = 0;
			Location targetLoc;

			@Override
			public void run() {
				if (mTicks == 0) {
					if (mStartAction != null) {
						mStartAction.run(mLoc);
					}
				} else if (mTicks > 0 && mTicks < mDelayTicks) {
					if (mWarningAction != null) {
						mWarningAction.run(mLoc);
					}
				} else if (mTicks >= mDelayTicks) {
					if (mEndAction != null) {
						mEndAction.run(mLoc);
					}
					this.cancel();
					mActiveRunnables.remove(this);
				}

				mTicks++;
			}
		};

		runnable.runTaskTimer(mPlugin, 0, 1);
		mActiveRunnables.add(runnable);
	}

	@Override
	public int duration() {
		return mDelayTicks;
	}
}
