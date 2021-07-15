package com.playmonumenta.plugins.bosses.bosses;

import java.util.List;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.scheduler.BukkitRunnable;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.utils.EntityUtils;

public class DummyDecoyBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_dummydecoy";
	public static final int detectionRange = 30;
	public static final int DURATION = 30;
	public static final int LIFETIME = 20 * 10;

	public static final double RADIUS = 4.0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DummyDecoyBoss(plugin, boss);
	}

	public DummyDecoyBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		// Boss effectively does nothing
		super.constructBoss(null, null, detectionRange, null);

		//Explode after a certain amount of time
		new BukkitRunnable() {

			@Override
			public void run() {
				explode();
				mBoss.remove();
				this.cancel();
			}
		}.runTaskLater(mPlugin, LIFETIME);
	}

	@Override
	public void death(EntityDeathEvent event) {
		if (event != null) {
			explode();
		}
	}

	private void explode() {
		World world = mBoss.getWorld();
		world.playSound(mBoss.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5f, 0.7f);
		world.spawnParticle(Particle.EXPLOSION_LARGE, mBoss.getLocation(), 10, 0.5, 1, 0.5, 0.05);
		List<LivingEntity> mobsToStun = EntityUtils.getNearbyMobs(mBoss.getLocation(), RADIUS);
		for (LivingEntity le : mobsToStun) {
			EntityUtils.applyStun(Plugin.getInstance(), DURATION, le);
		}
	}
}
