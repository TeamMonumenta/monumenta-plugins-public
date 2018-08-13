package com.playmonumenta.bossfights.spells;

import org.bukkit.plugin.Plugin;

/*
 * Super simple Spell which just runs in-line function
 * code from the caller
 */
public class SpellRunAction implements Spell
{
	@FunctionalInterface
	public interface Action
	{
		/**
		 * Action to run
		 */
		void run();
	}

	private Plugin mPlugin;
	private Action mAction;

	public SpellRunAction(Plugin plugin, Action action)
	{
		mPlugin = plugin;
		mAction = action;
	}

	@Override
	public void run()
	{
		if (mAction != null)
			mAction.run();
	}
}
