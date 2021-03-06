package com.playmonumenta.plugins.bosses.bosses;

import org.bukkit.Location;
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

import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.utils.BossUtils;
import com.playmonumenta.plugins.utils.EntityUtils;

public class AvengerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_avenger";
	public static final String SPEED_MODIFIER = "AvengerBossSpeedModifier";

	public static class Parameters {
		public int DETECTION = 15;

		public int MAX_STACKS = 10;
		public int RADIUS = 8;
		public double HEAL_PERCENT = .1;
		public double SPEED_PERCENT_INCREMENT = 0.06; // Capped at x1.6 Speed by default
		public double DAMAGE_PERCENT_INCREMENT = 0.2; // Capped at x3 Damage by deafult

		/** Sounds summoned when a nearby mob dies */
		public SoundsList SOUND_DEATH = SoundsList.fromString("[(ENTITY_ENDER_DRAGON_GROWL,0.1,0.8)]");
		/** Particle summon when a nearby mob dies */
		public ParticlesList PARTICLE_DEATH = ParticlesList.fromString("[(SPELL_WITCH,20)]");
		/** Particle that moves from death location to boss in a line */
		public ParticlesList PARTICLE_VECTOR = ParticlesList.fromString("[(VILLAGER_HAPPY,3)]");
		/** Particle summon at bos when PARTICLE_VECTOR is over */
		public ParticlesList PARTICLE_BOSS = ParticlesList.fromString("[(CRIT_MAGIC,20)]");
		/** Particle summon with PARTICLE_BOSS if boss heals */
		public ParticlesList PARTICLE_HEAL = ParticlesList.fromString("[(HEART,3)]");
		/** Ticks between death and PARTICLE_BOSS */
		public int VECTOR_TICKS = 12;
	}

	private int mStacks = 0;
	private final Parameters mParam;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new AvengerBoss(plugin, boss);
	}

	public AvengerBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = BossUtils.getParameters(boss, identityTag, new Parameters());
		super.constructBoss(null, null, mParam.DETECTION, null);
	}

	// Use this instead of the attribute to catch ability damage, ranged damage, etc.
	@Override
	public void bossDamagedEntity(EntityDamageByEntityEvent event) {
		if (mStacks > 0) {
			event.setDamage(EntityUtils.getDamageApproximation(event, 1 + mStacks * mParam.DAMAGE_PERCENT_INCREMENT));
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

			Location deadLoc = entity.getLocation();
			Location bossLoc = mBoss.getLocation();

			// Only trigger when the player kills a mob within range
			if (entity instanceof LivingEntity && ((LivingEntity) entity).getKiller() != null
					&& entity.getLocation().distance(bossLoc) < mParam.RADIUS) {
				mParam.SOUND_DEATH.play(bossLoc, 0.1f, 0.8f);
				mParam.PARTICLE_DEATH.spawn(bossLoc.clone().add(0, mBoss.getHeight() / 2, 0), 0.25, 0.45, 0.25, 1);

				new BukkitRunnable() {
					int mCount = 0;
					final int mMaxCount = mParam.VECTOR_TICKS;
					public void run() {
						if (mCount >= mMaxCount) {
							mParam.PARTICLE_BOSS.spawn(bossLoc, 20, 1.5, 1.5, 1.5);
							if (mParam.HEAL_PERCENT > 0) {
								mParam.PARTICLE_HEAL.spawn(bossLoc.clone().add(0, 0.5, 1), 1, 1, 1);
							}
							this.cancel();
						}

						Location bossEyeLoc = mBoss.getEyeLocation();
						Vector particleVector = bossEyeLoc.subtract(deadLoc).toVector().multiply((double) mCount / mMaxCount);
						mParam.PARTICLE_VECTOR.spawn(deadLoc.add(particleVector));

						mCount++;
					}
				}.runTaskTimer(mPlugin, 0, 1);

				// Heal the mob
				double maxHealth = mBoss.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue();
				mBoss.setHealth(Math.min(maxHealth, mBoss.getHealth() + maxHealth * mParam.HEAL_PERCENT));

				// Increment stacks, and if cap not hit, increase stats
				if (mStacks < mParam.MAX_STACKS) {
					mStacks++;

					AttributeInstance speed = mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
					if (speed != null) {
						AttributeModifier modifier = new AttributeModifier(SPEED_MODIFIER, mParam.SPEED_PERCENT_INCREMENT, AttributeModifier.Operation.MULTIPLY_SCALAR_1);
						speed.addModifier(modifier);
					}
				}
			}
		}
	}
}
