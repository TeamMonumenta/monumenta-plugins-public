package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public class PunchResistBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_punchresist";
	public static final int detectionRange = 100;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new PunchResistBoss(plugin, boss);
	}

	public PunchResistBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, detectionRange, null);
	}

	@Override
	public void bossHitByProjectile(ProjectileHitEvent event) {
		mBoss.setVelocity(new Vector(0,0,0));
	}

	@Override
	public void bossDamagedByEntity(EntityDamageByEntityEvent event) {
		if (event.getDamager() instanceof Projectile) {
			new BukkitRunnable() {
				int mTicks = 0;
				@Override
				public void run() {
					mBoss.setVelocity(new Vector(0,0,0));
					mTicks += 1;
					if (mTicks > 2) {
						this.cancel();
					}
				}
			}.runTaskTimer(mPlugin, 0, 1);
		}
	}
}
