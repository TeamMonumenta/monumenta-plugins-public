package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellPhalanx;
import org.bukkit.entity.LivingEntity;

public class PhalanxBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_phalanx";

	public static class Parameters extends BossParameters {
		@BossParam(help = "radius the phalanx is set off at")
		public int TRIGGER_RADIUS = 10;

		@BossParam(help = "not written")
		public int DELAY = 5 * 20;

		@BossParam(help = "Damage that this spell deals to players")
		public int DAMAGE = 20;

		@BossParam(help = "how big the arc that the phalanx goes over is")
		public int PHALANX_RADIUS = 2;

		@BossParam(help = "how many projectiles there will be")
		public int PROJ_COUNT = 4;

		@BossParam(help = "radius the phalanx is set off at")
		public int PHALANX_DURATION_MIN = 40;

		@BossParam(help = "phalanx expires after this time is passed")
		public int PHALANX_DURATION_MAX = 140;

		@BossParam(help = "angle between the projectiles")
		public int SPLIT_ANGLE = 30;

		@BossParam(help = "angle offset of the first projectile, from 0")
		public double START_ANGLE = 45;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		@BossParam(help = "speed of the projectiles")
		public double PROJECTILE_SPEED = 0.35;

		@BossParam(help = "homing aggression")
		public double TURN_SPEED = 0.02;

		@BossParam(help = "duration the projectile lasts before disappearing")
		public int MAX_LIFETIME = 4 * 20;

		@BossParam(help = "not written")
		public int COOLDOWN = 8 * 20;

		@BossParam(help = "not written")
		public int DETECTION = 20;

		@BossParam(help = "target of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT;

		@BossParam(help = "particles of the perched projectiles")
		public ParticlesList PARTICLES_PHALANX = ParticlesList.fromString("[(REDSTONE,1,0,0,0,0.1,#6acbff,1.0),(CRIT_MAGIC,1,0,0,0,0.1)]");

		@BossParam(help = "particles of the perched projectiles")
		public ParticlesList PARTICLE_PROJECTILE = ParticlesList.fromString("[(REDSTONE,1,0,0,0,0.1,#6acbff,1.0)]");

		@BossParam(help = "particles of the perched projectiles")
		public ParticlesList PARTICLE_HIT = ParticlesList.fromString("[(CRIT,20,0,0,0,0.1)]");

		@BossParam(help = "Sound played when phalanx forms")
		public SoundsList SOUND_PHALANX = SoundsList.fromString("[(BLOCK_BEACON_POWER_SELECT,1.2,1)]");

		@BossParam(help = "Sound played when shots are fired")
		public SoundsList SOUND_SHOOT = SoundsList.fromString("[(ENTITY_FIREWORK_ROCKET_LAUNCH,1,2)]");

		@BossParam(help = "Sound played when shot hits player")
		public SoundsList SOUND_HIT = SoundsList.fromString("[(ENTITY_ALLAY_HURT,1.2,0.67)]");

	}

	public PhalanxBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellPhalanx(plugin, boss, p);
		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
