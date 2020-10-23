package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class MistMob extends BossAbilityGroup {

	public static final int detectionRange = 20;
	public static final String identityTag = "MistMob";

	private LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) {
		return new MistMob(plugin, boss);
	}

	public MistMob(Plugin plugin, LivingEntity boss) {
		mBoss = boss;

		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, null);
	}

	@Override
	public void death(EntityDeathEvent event) {
		Location loc = event.getEntity().getLocation();
		Bukkit.getConsoleSender().getServer().dispatchCommand(Bukkit.getConsoleSender(), "execute positioned " + loc.getX() + " " + loc.getY() + " " + loc.getZ() + " run function monumenta:black_mist/mob_death");
	}
}
