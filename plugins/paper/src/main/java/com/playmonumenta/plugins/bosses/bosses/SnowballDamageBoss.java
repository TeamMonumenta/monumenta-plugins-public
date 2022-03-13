package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.BossUtils;
import java.util.Collections;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowman;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;


public class SnowballDamageBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_snowballdamage";

	public static class Parameters extends BossParameters {
		public int DETECTION = 50;
		public int DAMAGE = 8;
	}

	private final Parameters mParams;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new SnowballDamageBoss(plugin, boss);
	}

	public SnowballDamageBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);

		if (!(boss instanceof Snowman)) {
			throw new Exception("boss_snowballdamage only works on snowmen!");
		}

		mParams = BossParameters.getParameters(boss, identityTag, new Parameters());

		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParams.DETECTION, null);
	}


	@Override
	public void bossProjectileHit(ProjectileHitEvent event) {
		if (event.getHitEntity() != null && event.getHitEntity() instanceof Player player) {
			if ((player.getGameMode().equals(GameMode.SURVIVAL) || player.getGameMode().equals(GameMode.ADVENTURE)) && !player.isDead() && player.getHealth() > 0) {
				BossUtils.blockableDamage(mBoss, player, DamageType.PROJECTILE, mParams.DAMAGE);
			}
		}
	}
}
