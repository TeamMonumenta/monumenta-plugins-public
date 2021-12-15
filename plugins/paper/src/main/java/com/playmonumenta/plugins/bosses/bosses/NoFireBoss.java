package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.Fireball;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;

public class NoFireBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_nofire";
	public static final int detectionRange = 30;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new NoFireBoss(plugin, boss);
	}

	public NoFireBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		// Little changes to boss
		super.constructBoss(null, null, detectionRange, null);
	}

	//Prevents fireballs from setting fire
	@Override
	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {
		if (event.getEntity() instanceof Fireball ball) {
			ball.setIsIncendiary(false);
			ball.setYield(0f);
			ball.setFireTicks(0);
		}
	}
}
