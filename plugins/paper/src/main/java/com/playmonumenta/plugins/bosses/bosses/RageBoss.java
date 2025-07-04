package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellRage;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class RageBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_rage";
	public static final int detectionRange = 20;

	public static class Parameters extends BossParameters {
		@BossParam(help = "Size of Rage Buff. Default 12 blocks.")
		public int RADIUS = 12;
		@BossParam(help = "Amount of Ticks until takes effect. Default 30 ticks.")
		public int DELAY = 30;
		@BossParam(help = "Amount of Ticks between Rage casts. Default 160 ticks (8 seconds)")
		public int COOLDOWN = 8 * 20;

		@BossParam(help = "Duration of Buffs in ticks. Default 120 ticks (6 seconds)")
		public int BUFF_DURATION = 6 * 20;
		@BossParam(help = "Power of strength modifier. Default 0.2 (+20% damage)")
		public double STRENGTH_POWER = 0.2;
		@BossParam(help = "Power of speed modifier. Default 0.2 (+20% speed)")
		public double SPEED_POWER = 0.2;
		@BossParam(help = "Power of KBResist. Default 0.0 (0 KBR)")
		public double KBR_POWER = 0.0;

		@BossParam(help = "Buff mobs with CCImmune. Defaults false.")
		public boolean CC_IMMUNE_BUFF = false;
		@BossParam(help = "whether the mob can move while charging the nova or not")
		public boolean CAN_MOVE = false;

		@BossParam(help = "Sounds played when the boss begins charging the ability.")
		public SoundsList SOUND_CHARGE = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ZOMBIE_VILLAGER_CONVERTED, 1.0f, 0.6f))
			.build();
		@BossParam(help = "Particles summoned in the air as the boss is charging the ability.")
		public ParticlesList PARTICLE_CHARGE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_WITCH, 1, 0.0, 0.0, 0.0, 0.0))
			.build();
		@BossParam(help = "Particles summoned in a circle around the boss as the ability is charging.")
		public ParticlesList PARTICLE_CHARGE_CIRCLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT_MAGIC, 12, 0.25, 0.25, 0.25, 0.1))
			.build();
		@BossParam(help = "Sounds played when the boss finishes casting the ability.")
		public SoundsList SOUND_FINISH = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_BEACON_ACTIVATE, 1.5f, 1.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_RAVAGER_HURT, 1.5f, 0.5f))
			.add(new SoundsList.CSound(Sound.ENTITY_BLAZE_SHOOT, 1.5f, 0.75f))
			.build();
		@BossParam(help = "Particles summoned on the boss when the boss finishes casting the ability.")
		public ParticlesList PARTICLE_FINISH = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_WITCH, 15, 0.25, 0.45, 0.25, 1.0))
			.add(new ParticlesList.CParticle(Particle.VILLAGER_ANGRY, 5, 0.35, 0.5, 0.35, 0.0))
			.build();
		@BossParam(help = "Particles summoned that explode in a circle when the boss finishes casting the ability.")
		public ParticlesList PARTICLE_FINISH_CIRCLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.SPELL_WITCH, 24, 0.1, 0.1, 0.1, 0.3))
			.add(new ParticlesList.CParticle(Particle.BUBBLE_POP, 48, 0.25, 0.25, 0.25, 0.1))
			.build();
	}

	public RageBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellRage(plugin, boss, p.RADIUS, p.DELAY, p.COOLDOWN, p.BUFF_DURATION, p.STRENGTH_POWER, p.SPEED_POWER, p.KBR_POWER, p.CC_IMMUNE_BUFF, p.CAN_MOVE, p.SOUND_CHARGE, p.PARTICLE_CHARGE, p.PARTICLE_CHARGE_CIRCLE, p.SOUND_FINISH, p.PARTICLE_FINISH, p.PARTICLE_FINISH_CIRCLE);

		super.constructBoss(spell, detectionRange);
	}
}