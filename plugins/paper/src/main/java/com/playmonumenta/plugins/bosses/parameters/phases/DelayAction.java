package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class DelayAction implements Action {
	public static final String IDENTIFIER = "DELAY_ACTION";

	private final Action mAction;
	private final int mDelay;

	public DelayAction(int delay, Action action) {
		mAction = action;
		mDelay = delay;
	}


	@Override
	public void runAction(LivingEntity boss) {
		Bukkit.getScheduler().runTaskLater(
			Plugin.getInstance(),
			() -> {
				mAction.runAction(boss);
			},
			mDelay);
	}


}
