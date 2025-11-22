package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellStealth;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class StealthBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_stealth";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Particles spawned when the boss goes into stealth")
		public ParticlesList PARTICLE_STEALTH = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SMOKE_LARGE, 20, 0.5, 0.5, 0.5, 0.2))
			.build();
		@BossParam(help = "Particles spawned when the boss goes out of stealth")
		public ParticlesList PARTICLE_EXIT = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SMOKE_LARGE, 15, 0.5, 0.5, 0.5, 0.2))
			.build();
		@BossParam(help = "Particles spawned when the boss hits a player and exits stealth")
		public ParticlesList PARTICLE_HIT = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SMOKE_LARGE, 15, 0.5, 0.5, 0.5, 0.2))
			.build();
		@BossParam(help = "Sound played when the boss goes into stealth")
		public SoundsList SOUND_STEALTH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ILLUSIONER_CAST_SPELL, 1.2f, 0.5f))
			.build();
		@BossParam(help = "Sound played when the boss goes out of stealth")
		public SoundsList SOUND_EXIT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_BLAZE_SHOOT, 1.6f, 0.5f))
			.build();
		@BossParam(help = "Sound played when the boss hits a player and exits stealth")
		public SoundsList SOUND_HIT = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_BLAZE_SHOOT, 1.6f, 0.5f))
			.build();
		@BossParam(help = "Effects given to the boss when it enters stealth")
		public EffectsList EFFECTS_STEALTH = EffectsList.EMPTY;
		@BossParam(help = "Effects given to the player when the boss hits a player and exits stealth")
		public EffectsList EFFECTS_HIT = EffectsList.EMPTY;
		@BossParam(help = "Stealth time in ticks, set to -1 to have infinite stealth")
		public int DURATION = 10 * 20;
		@BossParam(help = "Damage to the entity when the boss hits a player and exits stealth")
		public int DAMAGE = 20;
		@BossParam(help = "Delay before the boss can cast")
		public int DELAY = 2 * 20;
		@BossParam(help = "Cooldown in between spells")
		public int COOLDOWN = 10 * 20;
		@BossParam(help = "Stop glowing temporarily when in stealth")
		public boolean HIDE_GLOWING = true;
		@BossParam(help = "Detection range for going into stealth")
		public int DETECTION = 32;
		@BossParam(help = "Spell Name (also used for the damage it does)")
		public String SPELL_NAME = "Stealth";
		@BossParam(help = "Whether to exit Stealth when near a player")
		public boolean PROXIMITY_CHECK = false;
		@BossParam(help = "Distance from player before automatically exiting Stealth (requires PROXIMITY_CHECK)")
		public int PROXIMITY = 6;
		@BossParam(help = "Whether to Stealth all entities riding this boss")
		public boolean STEALTH_PASSENGERS = true;
	}

	Parameters mParameters = new Parameters();

	public StealthBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		BossParameters.getParameters(boss, identityTag, mParameters);
		constructBoss(new SpellStealth(plugin, boss, mParameters), mParameters.DETECTION, null, mParameters.DELAY);
	}
}
