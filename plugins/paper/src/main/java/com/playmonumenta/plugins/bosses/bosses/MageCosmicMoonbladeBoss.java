package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellMageCosmicMoonblade;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class MageCosmicMoonbladeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_mage_cosmic_moonblade";

	public static class Parameters extends BossParameters {
		public int COOLDOWN = 8 * 20;
		public int SWINGS = 3;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";
		public int DAMAGE = 0;
		public double DAMAGE_PERCENTAGE = 0;
		public EffectsList EFFECTS = EffectsList.EMPTY;

		public int SPELL_DELAY = 20;
		public int SWINGS_DELAY = 10;
		public int DELAY = 4 * 20;
		public int START_ANGLE = 45;
		public int END_ANGLE = 135;
		public int RANGE = 6;
		public int DEGREE_INCREMENT = 30;
		public EntityTargets TARGETS_DIRECTION = EntityTargets.GENERIC_ONE_PLAYER_CLOSER_TARGET;
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET.clone().setRange(6);

		public ParticlesList PARTICLE_TELL = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.1, new Particle.DustOptions(Color.WHITE, 1.0f)))
			.build();

		public SoundsList SOUND_TELL = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_EVOKER_PREPARE_ATTACK, 1.0f, 2.0f))
			.build();

		public ParticlesList PARTICLE_SWING = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.1, new Particle.DustOptions(Color.fromRGB(0x6acbff), 1.0f)))
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 1, 0.0, 0.0, 0.0, 0.1, new Particle.DustOptions(Color.fromRGB(0xa8e2ff), 1.0f)))
			.build();

		public SoundsList SOUND_SWING = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_PLAYER_ATTACK_SWEEP, 0.75f, 0.8f))
			.add(new SoundsList.CSound(Sound.ENTITY_WITHER_SHOOT, 0.75f, 0.0f))
			.build();
	}

	public final Parameters mParams;

	public MageCosmicMoonbladeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		mParams = p;

		Spell spell = new SpellMageCosmicMoonblade(plugin, boss, p);

		super.constructBoss(spell, (int) p.TARGETS.getRange(), null, p.DELAY);
	}
}
