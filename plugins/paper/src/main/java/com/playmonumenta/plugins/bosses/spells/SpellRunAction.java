package com.playmonumenta.plugins.bosses.spells;

/*
 * Super simple Spell which just runs in-line function
 * code from the caller
 */
public class SpellRunAction extends Spell {
	@FunctionalInterface
	public interface Action {
		/**
		 * Action to run
		 */
		void run();
	}

	private final Action mAction;
	private final int mDuration;
	private final boolean mBypassSilence;

	public SpellRunAction(Action action) {
		this(action, 1, false);
	}

	public SpellRunAction(Action action, int duration) {
		this(action, duration, false);
	}

	public SpellRunAction(Action action, int duration, boolean bypassSilence) {
		mAction = action;
		mDuration = duration;
		mBypassSilence = bypassSilence;
	}

	@Override
	public void run() {
		if (mAction != null) {
			mAction.run();
		}
	}

	@Override
	public int cooldownTicks() {
		return mDuration;
	}

	@Override
	public boolean bypassSilence() {
		return mBypassSilence;
	}
}
