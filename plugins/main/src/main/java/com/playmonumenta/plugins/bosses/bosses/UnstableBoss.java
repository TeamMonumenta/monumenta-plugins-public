package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class UnstableBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_unstable";
	public static final int detectionRange = 20;

	LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new UnstableBoss(plugin, boss);
	}

	public UnstableBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		// Boss effectively does nothing
		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, null);
	}

	@Override
	public void death() {
		Location loc = mBoss.getLocation();
		loc.getWorld().createExplosion(loc, 3F);
	}
}
