package com.playmonumenta.bossfights.bosses;

import org.bukkit.entity.Creeper;
import org.bukkit.entity.Firework;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.plugin.Plugin;

public class VolatileBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_volatile";
	public static final int detectionRange = 20;

	private final Creeper mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		if (!(boss instanceof Creeper)) {
			throw new Exception("Attempted to give non-creeper the " + identityTag + " ability: " + boss.toString());
		}
		return new VolatileBoss(plugin, (Creeper)boss);
	}

	public VolatileBoss(Plugin plugin, LivingEntity boss) throws Exception {
		if (!(boss instanceof Creeper)) {
			throw new Exception("Attempted to give non-creeper the " + identityTag + " ability: " + boss.toString());
		}

		mBoss = (Creeper)boss;

		// Boss effectively does nothing
		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, null);
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (event.getCause().equals(DamageCause.ENTITY_EXPLOSION) && !(event.getDamager() instanceof Firework)) {
			mBoss.explode();
		}
	};
}
