package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.Collections;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

public class DodgeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_dodge";

	public static class Parameters extends BossParameters {

		public double TELEPORT_RANGE = 3;

		public long COOLDOWN = 20 * 3;
	}

	private final Parameters mParams;
	private boolean mDodge = true;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new DodgeBoss(plugin, boss);
	}

	public DodgeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParams = Parameters.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), -1, null);
	}

	@Override
	public void onHurtByEntityWithSource(DamageEvent event, Entity damager, LivingEntity source) {
		if (mDodge && !EntityUtils.isStunned(mBoss)) {
			dodge(event);
			mDodge = false;
			Bukkit.getScheduler().runTaskLater(mPlugin, () -> {
				mDodge = true;
			}, mParams.COOLDOWN);
		}
	}


	private void dodge(DamageEvent event) {
		event.setCancelled(true);
		World world = mBoss.getWorld();
		Location loc = mBoss.getLocation().add(0, 1, 0);
		Entity damager = event.getDamager();
		if (damager != null) {
			Vector direction = damager.getLocation().subtract(loc).toVector().setY(0).normalize();
			if (!Double.isFinite(direction.getX())) {
				direction = new Vector(0, 1, 0);
			}
			Vector sideways = new Vector(direction.getZ(), 0, -direction.getX());
			sideways.subtract(direction.multiply(0.25));
			if (FastUtils.RANDOM.nextBoolean()) {
				sideways.multiply(-1);
			}

			loc.add(sideways.multiply(mParams.TELEPORT_RANGE));
			for (int i = 0; i < mParams.TELEPORT_RANGE; i++) {
				if (loc.getBlock().isPassable()) {
					new PartialParticle(Particle.SMOKE_LARGE, loc, 10, 0, 0, 0, 0.5).spawnAsEntityActive(mBoss);
					world.playSound(loc, Sound.ENTITY_BLAZE_SHOOT, 0.5f, 0.5f);

					mBoss.teleport(loc);
					break;
				} else {
					loc.add(0, 1, 0);
				}
			}
		}
	}
}
