package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.utils.BossUtils;


public class SnowballDamageBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_snowballdamage";
	public static final int detectionRange = 50;

	private final LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SnowballDamageBoss(plugin, boss);
	}

	public SnowballDamageBoss(Plugin plugin, LivingEntity boss) throws Exception {
		mBoss = boss;

		if (!(boss instanceof Snowman)) {
			throw new Exception("boss_snowballdamage only works on snowmen!");
		}

		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, null);
	}


	public void bossProjectileHit(ProjectileHitEvent event) {
		if (event.getHitEntity() != null && event.getHitEntity() instanceof Player) {
			Player player = (Player)event.getHitEntity();
			if ((player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE)) && !player.isDead() && player.getHealth() > 0) {
				BossUtils.bossDamage(mBoss, player, 2);
			}
		}
	}
}
