package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.events.DamageEvent;
import com.playmonumenta.plugins.events.DamageEvent.DamageType;
import com.playmonumenta.plugins.utils.EntityUtils;
import com.playmonumenta.plugins.utils.LocationUtils;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

public final class AvengerBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_avenger";
	private static final String SPEED_MODIFIER = "AvengerBossSpeedModifier";

	@BossParam(help = "When a nearby living entity dies, the launcher heals and gains buffs to damage and movement speed")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Range in blocks that the launcher searches for players before this spell can run")
		public int DETECTION = 15;

		@BossParam(help = "Maximum amount of stacks the launcher can gain. Governs maximum damage and speed effect potency")
		public int MAX_STACKS = 10;

		@BossParam(help = "Radius in blocks in which dying entities heal and buff the launcher")
		public int RADIUS = 8;

		@BossParam(help = "Percent max health the launcher gains on nearby living entity death")
		public double HEAL_PERCENT = .1;

		@BossParam(help = "Movement speed effect potency per stack")
		public double SPEED_PERCENT_INCREMENT = 0.06;

		@BossParam(help = "Damage effect potency per stack. The buff applies to all damage dealt by this launcher")
		public double DAMAGE_PERCENT_INCREMENT = 0.2;

		@BossParam(help = "Sounds played when a nearby living entity dies")
		public SoundsList SOUND_DEATH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ENDER_DRAGON_GROWL, 0.1f, 0.8f))
			.build();

		@BossParam(help = "Particles summoned when a nearby living entity dies")
		public ParticlesList PARTICLE_DEATH = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_WITCH, 20, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Particles that traces a line between a death location and the launcher")
		public ParticlesList PARTICLE_VECTOR = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.VILLAGER_HAPPY, 3, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Particles summoned on the launcher when PARTICLE_VECTOR completes")
		public ParticlesList PARTICLE_BOSS = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT_MAGIC, 20, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Particles summoned with PARTICLE_BOSS if the launcher heals")
		public ParticlesList PARTICLE_HEAL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.HEART, 3, 0.0, 0.0, 0.0, 0.0))
			.build();

		@BossParam(help = "Time in ticks between a nearby living entity death and the launcher gaining health and buffs")
		public int VECTOR_TICKS = 12;
	}

	private final Parameters mParam;
	private int mStacks = 0;

	public AvengerBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = BossParameters.getParameters(mBoss, identityTag, new Parameters());
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), mParam.DETECTION, null);
	}

	// Use this instead of the attribute to catch ability damage, ranged damage, etc.
	@Override
	public void onDamage(final DamageEvent event, final LivingEntity damagee) {
		if (event.getType() == DamageType.TRUE) {
			return;
		}

		if (mStacks > 0) {
			event.setFlatDamage(event.getDamage() * (1 + mStacks * mParam.DAMAGE_PERCENT_INCREMENT));
		}
	}

	@Override
	public boolean hasNearbyEntityDeathTrigger() {
		return true;
	}

	@Override
	public double nearbyEntityDeathMaxRange() {
		return mParam.RADIUS;
	}

	@Override
	public void nearbyEntityDeath(final EntityDeathEvent event) {
		if (event.isCancelled() || !mBoss.isValid() || mBoss.isDead() || mStacks >= mParam.MAX_STACKS) {
			return;
		}

		// Only trigger when the player kills a mob within range
		final Location deadLoc = event.getEntity().getLocation();
		final Location bossLoc = mBoss.getLocation();
		if (event.getEntity().getKiller() == null || deadLoc.distanceSquared(bossLoc) > mParam.RADIUS * mParam.RADIUS) {
			return;
		}

		mParam.SOUND_DEATH.play(bossLoc, 0.1f, 0.8f);
		mParam.PARTICLE_DEATH.spawn(mBoss, LocationUtils.getEntityCenter(mBoss), 0.25, 0.45, 0.25, 1);

		new BukkitRunnable() {
			int mCount = 0;
			final int mMaxCount = mParam.VECTOR_TICKS;

			@Override
			public void run() {
				mCount++;
				final Vector particleVector = mBoss.getEyeLocation().subtract(deadLoc).toVector()
					.multiply((double) mCount / mMaxCount);
				mParam.PARTICLE_VECTOR.spawn(mBoss, deadLoc.add(particleVector));

				if (mCount >= mMaxCount) {
					// Increment stacks, and if cap not hit, increase stats and heal
					if (mStacks < mParam.MAX_STACKS) {
						mStacks++;

						mParam.PARTICLE_BOSS.spawn(mBoss, bossLoc, 20, 1.5, 1.5, 1.5);

						final AttributeInstance speed = mBoss.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
						if (speed != null) {
							speed.addModifier(new AttributeModifier(SPEED_MODIFIER, mParam.SPEED_PERCENT_INCREMENT,
								AttributeModifier.Operation.MULTIPLY_SCALAR_1));
						}

						if (mParam.HEAL_PERCENT > 0) {
							EntityUtils.healMob(mBoss, EntityUtils.getMaxHealth(mBoss) * mParam.HEAL_PERCENT);
							mParam.PARTICLE_HEAL.spawn(mBoss, bossLoc.clone().add(0, 0.5, 0), 1, 1, 1);
						}
					}
					this.cancel();
				}
			}
		}.runTaskTimer(mPlugin, 0, 1);
	}
}