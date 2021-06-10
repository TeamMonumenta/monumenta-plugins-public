package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class AvengerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_avenger";
	public static final String SPEED_MODIFIER = "AvengerBossSpeedModifier";

	public static class Parameters {
		public int DETECTION_RANGE = 15;

		public int MAX_STACKS = 10;
		public int RADIUS = 8;
		public double HEAL_PERCENT = .1;
		public double SPEED_PERCENT_INCREMENT = 0.06; // Capped at x1.6 Speed by default
		public double DAMAGE_PERCENT_INCREMENT = 0.2; // Capped at x3 Damage by deafult

		public Sound SOUND = Sound.ENTITY_ENDER_DRAGON_GROWL; // Plays when nearby mob dies
		public Particle DEATH_PARTICLE = Particle.SPELL_WITCH; // Plays when nearby mob dies
		public Particle VECTOR_PARTICLE = Particle.VILLAGER_HAPPY; // Moves from death location to boss in a line
		public Particle BOSS_PARTICLE = Particle.CRIT_MAGIC; // Plays at boss after VECTOR_PARTICLE is over
		public Particle HEAL_PARTICLE = Particle.HEART; // Plays with BOSS_PARTICLE if boss heals
		public int VECTOR_TICKS = 12; // Ticks between death and BOSS_PARTICLE
	}

	private int mStacks = 0;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AvengerBoss(plugin, boss);
	}

	public AvengerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		final Parameters p = BossUtils.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(null, null, p.DETECTION_RANGE, null);
	}

	// Use this instead of the attribute to catch ability damage, ranged damage, etc.
	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (mStacks > 0) {
			final Parameters p = BossUtils.getParameters(mBoss, identityTag, new Parameters());
			event.setDamage(EntityUtils.getDamageApproximation(event, 1 + mStacks * p.DAMAGE_PERCENT_INCREMENT));
		}
	}

	@Override
	public boolean hasNearbyEntityDeathTrigger() {
		return true;
	}

	@Override
	public void nearbyEntityDeath(EntityDeathEvent event) {
		if (!event.isCancelled() && mBoss != null) {
			final Parameters p = BossUtils.getParameters(mBoss, identityTag, new Parameters());

			Entity entity = event.getEntity();

			Location deadLoc = entity.getLocation();
			Location bossLoc = mBoss.getLocation();
			World world = mBoss.getWorld();

			// Only trigger when the player kills a mob within range
			if (entity instanceof LivingEntity && ((LivingEntity) entity).getKiller() != null
					&& entity.getLocation().distance(bossLoc) < p.RADIUS) {
				world.playSound(bossLoc, p.SOUND, 0.1f, 0.8f);
				world.spawnParticle(p.DEATH_PARTICLE, bossLoc.add(0, mBoss.getHeight() / 2, 0), 20, 0.25, 0.45, 0.25, 1);

				new BukkitRunnable() {
					int mCount = 0;
					final int mMaxCount = p.VECTOR_TICKS;
					public void run() {
						if (mCount >= mMaxCount) {
							world.spawnParticle(p.BOSS_PARTICLE, mBoss.getLocation(), 20, 1.5, 1.5, 1.5);
							if (p.HEAL_PERCENT > 0) {
								world.spawnParticle(p.HEAL_PARTICLE, mBoss.getLocation().add(0, 0.5, 1), 3, 1, 1, 1);
							}
							this.cancel();
						}

						Location bossEyeLoc = mBoss.getEyeLocation();
						Vector particleVector = bossEyeLoc.subtract(deadLoc).toVector().multiply((double) mCount / mMaxCount);
						world.spawnParticle(p.VECTOR_PARTICLE, deadLoc.add(particleVector), 3);

						mCount++;
					}
				}.runTaskTimer(mPlugin, 0, 1);

				// Heal the mob
				double maxHealth = mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				mBoss.setHealth(Math.min(maxHealth, mBoss.getHealth() + maxHealth * p.HEAL_PERCENT));

				// Increment stacks, and if cap not hit, increase stats
				if (mStacks < p.MAX_STACKS) {
					mStacks++;

					AttributeInstance speed = mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
					if (speed != null) {
						AttributeModifier modifier = new AttributeModifier(SPEED_MODIFIER, p.SPEED_PERCENT_INCREMENT, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
						speed.addModifier(modifier);
					}
				}
			}
		}
	}
}
