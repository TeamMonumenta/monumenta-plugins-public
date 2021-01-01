package com.playmonumenta.plugins.bosses.bosses;

import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.effects.PercentSpeed;
import com.playmonumenta.plugins.utils.EntityUtils;

public class CoordinatedAttackBoss extends BossAbilityGroup {

	public static final String identityTag = "boss_coordinatedattack";
	public static final int detectionRange = 24;

	private static final String PERCENT_SPEED_EFFECT_NAME = "CoordinatedAttackPercentSpeedEffect";
	private static final double PERCENT_SPEED_EFFECT = 0.3;
	private static final int PERCENT_SPEED_DURATION = 20 * 8;

	private static final int TARGET_RADIUS = 24;

	private final com.playmonumenta.plugins.Plugin mPlugin;
	private final LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new CoordinatedAttackBoss(plugin, boss);
	}

	public CoordinatedAttackBoss(Plugin plugin, LivingEntity boss) {
		mPlugin = com.playmonumenta.plugins.Plugin.getInstance();
		mBoss = boss;

		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, null);
	}

	@Override
	public void bossChangedTarget(EntityTargetEvent event) {
		Entity target = event.getTarget();

		if (target instanceof Player) {
			Player player = (Player) target;
			World world = mBoss.getWorld();
			Location locTarget = player.getLocation();

			for (LivingEntity mob : EntityUtils.getNearbyMobs(locTarget, TARGET_RADIUS)) {
				if (mob instanceof Mob && mob.hasLineOfSight(player)) {
					Set<String> tags = mob.getScoreboardTags();
					// Don't set target of mobs with this ability, or else infinite loop
					if (tags == null || !tags.contains(identityTag)) {
						((Mob) mob).setTarget(player);

						mPlugin.mEffectManager.addEffect(mob, PERCENT_SPEED_EFFECT_NAME,
								new PercentSpeed(PERCENT_SPEED_DURATION, PERCENT_SPEED_EFFECT, PERCENT_SPEED_EFFECT_NAME));

						Location loc = mob.getLocation();
						double distance = loc.distance(locTarget);
						Vector velocity = locTarget.clone().subtract(loc).toVector().multiply(0.19);
						velocity.setY(velocity.getY() * 0.5 + distance * 0.08);
						mob.setVelocity(velocity);

						world.spawnParticle(Particle.CLOUD, loc, 10, 0.1, 0.1, 0.1, 0.1);
						world.spawnParticle(Particle.VILLAGER_ANGRY, mob.getEyeLocation(), 5, 0.3, 0.3, 0.3, 0);
					}
				}
			}
		}
	}

}
