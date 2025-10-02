package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellBeam;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class BeamBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_beam";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int COOLDOWN = 60;

		@BossParam(help = "not written")
		public int DELAY = 20;

		@BossParam(help = "Telegraph duration of the spell")
		public int SPELL_DELAY = 30;

		@BossParam(help = "Should the beam be towards the mob's target?")
		public boolean PREFER_TARGET = false;

		@BossParam(help = "Should the beam stop at blocks?")
		public boolean STOP_AT_BLOCK = true;

		@BossParam(help = "Should the beam stop at entities?")
		public boolean STOP_AT_PLAYER = false;

		@BossParam(help = "Name of the beam attack")
		public String NAME = "";

		@BossParam(help = "not written")
		public int DAMAGE = 10;

		@BossParam(help = "The distance the beam covers")
		public int BEAM_RANGE = 30;

		@BossParam(help = "How long should the ray track the player")
		public int BEAM_TRACK = 0;

		@BossParam(help = "Duration of the root")
		public int ROOT_DURATION = 0;

		@BossParam(help = "Amount of projectiles to be launched in a flat plane")
		public int SPLIT = 1;

		@BossParam(help = "Angle in degrees between split projectiles")
		public double SPLIT_ANGLE = 30;

		@BossParam(help = "Should the beam face the same way as the mob?")
		public boolean FACE_TARGET = false;

		@BossParam(help = "Should the beam re-adjust to the y level of the player?")
		public boolean LOCK_PITCH = false;

		@BossParam(help = "Should the beam respect player's immunity frames?")
		public boolean RESPECT_IFRAMES = true;

		@BossParam(help = "Effects applied to the player when struck by the beam")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "Horizontal knockback velocity")
		public float KB_XZ = 0.35f;

		@BossParam(help = "Vertical knockback velocity")
		public float KB_Y = 0.25f;

		@BossParam(help = "Hitbox for the beam attack")
		public float HITBOX_SIZE = 0.5f;

		@BossParam(help = "Change the type of damage the beam is")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;

		@BossParam(help = "Percentage of health dealt as true damage")
		public double TRUE_DAMAGE_PERCENTAGE = 0;

		@BossParam(help = "Y offset of the beam")
		public double Y_OFFSET = 0;

		// Sounds
		@BossParam(help = "Sound played when the boss starts casting beam")
		public SoundsList SOUND_TELE = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_SONIC_CHARGE, 1.5f, 1.5f))
			.build();

		@BossParam(help = "Sound played when player is hit by the beam")
		public SoundsList SOUND_HIT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_GENERIC_EXPLODE, 1f, 1.5f))
			.build();

		@BossParam(help = "Sound played when the boss cast the beam")
		public SoundsList SOUND_CAST = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_WARDEN_SONIC_BOOM, 1.5f, 1.5f))
			.build();

		// Particles
		@BossParam(help = "Particles of the telegraph")
		public ParticlesList PARTICLE_TELE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT, 2, 0.1, 0.1, 0.1, 0))
			.build();

		@BossParam(help = "Particles as a helix")
		public ParticlesList PARTICLE_HELIX = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT_MAGIC, 8, 0.08, 0.08, 0.08, 0))
			.build();

		@BossParam(help = "Particles as a beam")
		public ParticlesList PARTICLE_BEAM = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FIREWORKS_SPARK, 6, 0.12, 0.12, 0.12, 0))
			.add(new ParticlesList.CParticle(Particle.ELECTRIC_SPARK, 10, 0.12, 0.12, 0.12, 0))
			.build();

		@BossParam(help = "Particles spawn at the boss")
		public ParticlesList PARTICLE_BOSS = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FLASH, 1, 0, 0, 0, 0))
			.build();

		@BossParam(help = "Particles as a circle")
		public ParticlesList PARTICLE_CIRCLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.END_ROD, 20, 0, 0, 0, 0.01))
			.build();

		@BossParam(help = "Interval of the Telegraph Particles")
		public int TELEGRAPH_INTERVAL = 4;

		@BossParam(help = "Size of the helix")
		public float HELIX_SIZE = 3f;

		@BossParam(help = "Targets for person targeted by beam")
		public EntityTargets TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, 30, new EntityTargets.Limit(1, EntityTargets.Limit.SORTING.FARTHER));
	}

	BeamBoss.Parameters mParameters = new BeamBoss.Parameters();

	public BeamBoss(Plugin plugin, LivingEntity mBoss) {
		super(plugin, identityTag, mBoss);
		Parameters.getParameters(mBoss, identityTag, mParameters);
		constructBoss(new SpellBeam(plugin, mBoss, mParameters), (int) mParameters.TARGETS.getRange(), null, mParameters.DELAY);
	}
}
