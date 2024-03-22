package com.playmonumenta.plugins.abilities;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;

public class IndependentIframeTracker {
	private final Map<UUID, Integer> mMobsIframeMap;
	private static int mIframeDuration;


	public IndependentIframeTracker(int iframeDuration) {
		mMobsIframeMap = new HashMap<>();
		mIframeDuration = iframeDuration;
	}

	public boolean damage(LivingEntity target, Runnable damageAction) {
		mMobsIframeMap.values().removeIf(tick -> tick + mIframeDuration < Bukkit.getServer().getCurrentTick());

		if (mMobsIframeMap.containsKey(target.getUniqueId())) {
			return false;
		}

		double beforeHealth = target.getHealth();
		damageAction.run();
		double healthDelta = beforeHealth - target.getHealth();

		if (healthDelta > 0) {
			mMobsIframeMap.put(target.getUniqueId(), Bukkit.getServer().getCurrentTick());
		}

		return true;
	}
}
