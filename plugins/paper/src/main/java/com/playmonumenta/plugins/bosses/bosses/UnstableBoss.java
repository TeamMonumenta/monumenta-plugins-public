package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class UnstableBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_unstable";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new UnstableBoss(plugin, boss);
	}

	public UnstableBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		// Boss effectively does nothing
		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		mBoss.getLocation().getWorld().createExplosion(mBoss, 4, false, true);
	}
}
