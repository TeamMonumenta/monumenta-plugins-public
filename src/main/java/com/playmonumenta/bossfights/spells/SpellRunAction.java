package com.playmonumenta.bossfights.spells;

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

	private Action mAction;

	public SpellRunAction(Action action) {
		mAction = action;
	}

	@Override
	public void run() {
		if (mAction != null) {
			mAction.run();
		}
	}

	@Override
	public int duration() {
		return 1;
	}
}
