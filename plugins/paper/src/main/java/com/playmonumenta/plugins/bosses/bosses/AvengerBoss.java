package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.utils.EntityUtils;

public class AvengerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_avenger";
	public static final int detectionRange = 15;

	private static final String SPEED_MODIFIER = "AvengerBossSpeedModifier";
	private static final int MAX_STACKS = 10;
	private static final int RADIUS = 8;
	private static final double HEAL_PERCENT = .1;
	private static final double SPEED_PERCENT_INCREMENT = 0.06; // Capped at x1.6 Speed
	private static final double DAMAGE_PERCENT_INCREMENT = 0.2; // Capped at x3 Damage

	private int mStacks = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AvengerBoss(plugin, boss);
	}

	public AvengerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		super.constructBoss(null, null, detectionRange, null);
	}

	// Use this instead of the attribute to catch ability damage, ranged damage, etc.
	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (mStacks > 0) {
			event.setDamage(EntityUtils.getDamageApproximation(event, 1 + mStacks * DAMAGE_PERCENT_INCREMENT));
		}
	}

	@Override
	public boolean hasNearbyEntityDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyEntityDeath(EntityDeathEvent event) {
		if (!event.isCancelled() && mBoss != null) {
			Entity entity = event.getEntity();

			// Only trigger when the player kills a mob within range
			if (entity instanceof LivingEntity && ((LivingEntity) entity).getKiller() != null
					&& entity.getLocation().distance(mBoss.getLocation()) < RADIUS) {
				mBoss.getWorld().playSound(mBoss.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 0.1f, 0.8f);
				mBoss.getWorld().spawnParticle(Particle.SPELL_WITCH, mBoss.getLocation().add(0, mBoss.getHeight() / 2, 0), 20, 0.25, 0.45, 0.25, 1);
				mBoss.getWorld().spawnParticle(Particle.CRIT_MAGIC, mBoss.getLocation(), 20, 1.5, 1.5, 1.5);

				// Heal the mob
				double maxHealth = mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				mBoss.setHealth(Math.min(maxHealth, mBoss.getHealth() + maxHealth * HEAL_PERCENT));

				// Increment stacks, and if cap not hit, increase stats
				if (mStacks < MAX_STACKS) {
					mStacks++;

					AttributeInstance speed = mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
					if (speed != null) {
						AttributeModifier modifier = new AttributeModifier(SPEED_MODIFIER, SPEED_PERCENT_INCREMENT, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
						speed.addModifier(modifier);
					}
				}
			}
		}
	}
}
