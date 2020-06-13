package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

public class AvengerBoss extends BossAbilityGroup implements Listener {
	public static final String identityTag = "boss_avenger";
	public static final int detectionRange = 15;
	public static final double radius = 8.0;
	public static final float HEALTH_HEALED = .1f;

	private final LivingEntity mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AvengerBoss(plugin, boss);
	}

	public AvengerBoss(Plugin plugin, LivingEntity boss) {
		mBoss = boss;
		super.constructBoss(plugin, identityTag, mBoss, null, null, detectionRange, null);
	}

	@Override
	public void nearbyEntityDeath(EntityDeathEvent event) {
		if (!event.isCancelled() && mBoss != null) {
			Entity entity = event.getEntity();

			if (entity.getLocation().distance(mBoss.getLocation()) < radius) {
				//Heal the mob
				if (mBoss.getHealth() + ((mBoss.getHealth() * HEALTH_HEALED)) <= mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue()) {
					mBoss.setHealth(mBoss.getHealth() + ((mBoss.getHealth() * HEALTH_HEALED)));
				} else {
					mBoss.setHealth(mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue());
				}

				//Raise movement speed
				mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue() * 1.1);

				//Raise the mobs attack damage
				mBoss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(mBoss.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).getBaseValue() * 1.1);

				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 0.8f);
				mBoss.getWorld().spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 30, 0.25, 0.45, 0.25, 1);
				mBoss.getWorld().spawnParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 25, 1.5, 1.5, 1.5);
			}
		}
	}
}
