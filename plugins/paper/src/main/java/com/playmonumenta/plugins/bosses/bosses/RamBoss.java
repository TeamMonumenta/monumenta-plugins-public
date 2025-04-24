package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellRam;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class RamBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_ram";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Particles spawned when the boss casts ram")
		public ParticlesList PARTICLE_LAUNCH = new ParticlesList(List.of(
			new ParticlesList.CParticle(Particle.EXPLOSION_HUGE, 1, 2, 2, 2)
		));
		@BossParam(help = "Particles spawned for the telegraph")
		public ParticlesList PARTICLE_TEL = new ParticlesList(List.of(
			new ParticlesList.CParticle(Particle.WAX_OFF, 1)
		));
		@BossParam(help = "Particles spawned for the telegraph")
		public ParticlesList PARTICLE_END = new ParticlesList(List.of(
			new ParticlesList.CParticle(Particle.CLOUD, 10, 1, 1, 1, 1)
		));
		@BossParam(help = "Sound played when the boss telegraphs the direction of the ram")
		public SoundsList SOUND_TEL = new SoundsList(List.of(
			new SoundsList.CSound(Sound.ENTITY_RAVAGER_ROAR, 5.0f, 1.0f)
		));
		@BossParam(help = "Sound played when the boss rams")
		public SoundsList SOUND_LAUNCH = new SoundsList(List.of(
			new SoundsList.CSound(Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 0.9f)
		));
		@BossParam(help = "Sound played when the boss finishes ramming")
		public SoundsList SOUND_END = new SoundsList(List.of(
			new SoundsList.CSound(Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 0.9f)
		));
		@BossParam(help = "Sound played when the boss ramming with a frequency")
		public SoundsList SOUND_RAM_TICK = new SoundsList(List.of(
			new SoundsList.CSound(Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 0.3f, 0.9f)
		));
		@BossParam(help = "Ticks between each sound_ram_tick played")
		public int SOUND__INTERVAL = 4;
		@BossParam(help = "Effects given to the player when the ram hits a target")
		public EffectsList EFFECTS_HIT = EffectsList.EMPTY;
		@BossParam(help = "Spell name")
		public String SPELL_NAME = "Ram";
		@BossParam(help = "Velocity of the ram")
		public float VELOCITY = 0.5f;
		@BossParam(help = "Damage for any player hit by the ram")
		public float DAMAGE = 20;
		@BossParam(help = "Damage type for the damage")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;
		@BossParam(help = "Horizontal knockback velocity")
		public float KB_XZ = 1.0f;
		@BossParam(help = "Vertical knockback velocity")
		public float KB_Y = 0.25f;
		@BossParam(help = "Extra range from the boss' hitbox for detecting players hit by the ram")
		public float HITBOX_EXPAND = 1.5f;
		@BossParam(help = "Delay before the boss can cast")
		public int DELAY = 2 * 20;
		@BossParam(help = "Cooldown in between spells")
		public int COOLDOWN = 10 * 20;
		@BossParam(help = "Telegraph duration")
		public int TELEGRAPH_DURATION = 2 * 20;
		@BossParam(help = "Maximum time the ram lasts.")
		public int RAM_DURATION = 7 * 20;
		@BossParam(help = "Interval between new telegraph lines being made.")
		public int TELEGRAPH_INTERVAL = 10;
		@BossParam(help = "Cancels when damaging an enemy")
		public boolean CANCEL_ON_DAMAGE = false;
		@BossParam(help = "Distance to the target location which would automatically cancel the spell")
		public double END_DISTANCE_THRESHOLD = 2.5;
		@BossParam(help = "Targets for person targeted by ram")
		public EntityTargets TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, 30, false, new EntityTargets.Limit(1, EntityTargets.Limit.SORTING.FARTHER));
	}

	Parameters mParameters = new Parameters();

	public RamBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters.getParameters(boss, identityTag, mParameters);
		constructBoss(new SpellRam(plugin, boss, mParameters), (int) mParameters.TARGETS.getRange(), null, mParameters.DELAY);
	}
}
