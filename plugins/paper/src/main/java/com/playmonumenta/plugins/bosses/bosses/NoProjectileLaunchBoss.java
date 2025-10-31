package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import java.util.Collections;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.plugin.Plugin;

public class NoProjectileLaunchBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_no_projectile_launch";

	public NoProjectileLaunchBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), 40, null);
	}

	@Override
	public void bossLaunchedProjectile(ProjectileLaunchEvent event) {
		event.setCancelled(true);
	}
}
