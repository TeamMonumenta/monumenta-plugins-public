package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import org.bukkit.entity.LivingEntity;

public class InvisibleBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_invisible";
	public static final int detectionRange = 100;

	public InvisibleBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		boss.setInvisible(true);
	}

	@Override
	public void unload() {
		if (!mBoss.isDead()) {
			mBoss.setInvisible(false);
		}
	}
}
