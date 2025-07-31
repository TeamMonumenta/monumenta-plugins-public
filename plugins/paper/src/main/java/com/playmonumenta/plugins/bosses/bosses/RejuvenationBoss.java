package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellMobHealAoE;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;

/* TODO: Merge this with NovaBoss */
public class RejuvenationBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_rejuvenation";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int HEAL = 25;
		@BossParam(help = "not written")
		public int RANGE = 8;
		@BossParam(help = "not written")
		public int DURATION = 80;
		@BossParam(help = "not written")
		public int DETECTION = 20;
		@BossParam(help = "not written")
		public int DELAY = 5 * 20;
		@BossParam(help = "not written")
		public int COOLDOWN = 15 * 20;

		@BossParam(help = "not written")
		public double PARTICLE_RADIUS = 8;
		@BossParam(help = "not written")
		public boolean CAN_MOVE = true;
		@BossParam(help = "not written")
		public boolean OVERHEAL = true;

		@BossParam(help = "Targets of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_MOB_TARGET.clone().setFilters(List.of(EntityTargets.PLAYERFILTER.NOT_STEALTHED));


		public ParticlesList PARTICLE_CHARGE_AIR = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_INSTANT, 3, 0.0, 0.0, 0.0, 0.0))
			.build();

		public ParticlesList PARTICLE_CHARGE_CIRCLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_INSTANT, 3, 0.0, 0.0, 0.0, 0.0))
			.build();

		public SoundsList SOUND_CHARGE = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ITEM_TRIDENT_RETURN, 0.8f, 1.0f))
			.build();

		public SoundsList SOUND_OUTBURST_CIRCLE = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ILLUSIONER_CAST_SPELL, 3.0f, 1.25f))
			.add(new SoundsList.CSound(Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 3.0f, 2.0f))
			.build();

		public ParticlesList PARTICLE_OUTBURST_AIR = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FIREWORKS_SPARK, 3, 0.0, 0.0, 0.0, 0.0))
			.add(new ParticlesList.CParticle(Particle.VILLAGER_HAPPY, 3, 3.5, 3.5, 3.5, 0.5))
			.build();

		public ParticlesList PARTICLE_OUTBURST_CIRCLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT_MAGIC, 3, 0.25, 0.25, 0.25, 0.35))
			.add(new ParticlesList.CParticle(Particle.FIREWORKS_SPARK, 2, 0.25, 0.25, 0.25, 0.15))
			.build();

		public ParticlesList PARTICLE_HEAL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FIREWORKS_SPARK, 3, 0.25, 0.5, 0.25, 0.3))
			.add(new ParticlesList.CParticle(Particle.HEART, 3, 0.4, 0.5, 0.4, 0.0))
			.build();

	}

	public RejuvenationBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		if (p.TARGETS == EntityTargets.GENERIC_MOB_TARGET) {
			//probably a mob from an older version.
			p.TARGETS = new EntityTargets(EntityTargets.TARGETS.MOB, p.RANGE);
			p.PARTICLE_RADIUS = p.RANGE;
		}

		Spell spell = new SpellMobHealAoE(plugin, boss, p);

		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}