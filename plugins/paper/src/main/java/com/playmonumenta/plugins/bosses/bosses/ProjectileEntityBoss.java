package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.LoSPool;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellProjectileEntity;
import com.playmonumenta.plugins.events.DamageEvent;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class ProjectileEntityBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_projectile_entity";

	public enum Type {
		PERSIST,
		REMOVE,
		KILL
	}

	public static class Parameters extends BossParameters {

		// Standard parameters of a projectile

		@BossParam(help = "Damage to apply on hit entity")
		public double DAMAGE = 0;

		@BossParam(help = "Type of damage")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MAGIC;

		@BossParam(help = "Percentage of health done as true damage")
		public double TRUE_DAMAGE_PERCENTAGE = 0;

		@BossParam(help = "Should the true damage percentage be blockable?")
		public boolean TRUE_DAMAGE_BLOCK = false;

		@BossParam(help = "The spell name shown when a player is killed by this skill")
		public String SPELL_NAME = "";

		@BossParam(help = "Effects applied to hit entities")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "Projectile speed in blocks per tick")
		public double SPEED = 0.4;

		@BossParam(help = "Radius in blocks that this boss will check before doing anything")
		public int DETECTION = 24;

		@BossParam(help = "Initial delay in ticks for the first cast of this spell")
		public int DELAY = 20 * 2;

		@BossParam(help = "Duration of the Telegraph")
		public int SPELL_DELAY = 20 * 2;

		@BossParam(help = "Period in ticks between the start of the last charge and next start.")
		public int COOLDOWN = 20 * 5;

		@BossParam(help = "Angular velocity (in radians) of the projectile while tracking a target")
		public double TURN_RADIUS = Math.PI / 30;

		@BossParam(help = "Length in blocks of the projectile hitbox")
		public double HITBOX_LENGTH = 0.5;

		@BossParam(help = "Force launch at a yaw degree offset from boss' sight. [-180, 180] is valid.")
		public double FIX_YAW = 200.0;

		@BossParam(help = "Force launch at a fixed pitch degree. [-90, 90] is valid.")
		public double FIX_PITCH = 100.0;

		@BossParam(help = "Left offset from mob's eye to projectile start point")
		public double OFFSET_LEFT = 0;

		@BossParam(help = "Up offset for projectile start point. If entity is empty, offset starts at half height, otherwise from eyes.")
		public double OFFSET_UP = 0;

		@BossParam(help = "Front offset from mob's eye to projectile start point")
		public double OFFSET_FRONT = 0;

		@BossParam(help = "Mob glowing color")
		public String COLOR = "red";

		@BossParam(help = "Should the projectile stop at blocks?")
		public boolean COLLIDES_WITH_BLOCKS = true;

		@BossParam(help = "Percent speed applied to the projectile while traveling in solid or liquid blocks")
		public double SPEED_BLOCK = 1;

		@BossParam(help = "Conditions to choose valid entities of this spell")
		public EntityTargets TARGETS = new EntityTargets(EntityTargets.TARGETS.PLAYER, 40, new EntityTargets.Limit(EntityTargets.Limit.LIMITSENUM.ALL), List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT, EntityTargets.PLAYERFILTER.NOT_STEALTHED));

		@BossParam(help = "If the casting boss gets interrupted (ie. death, stunned), should the projectile linger?")
		public boolean LINGERS = true;

		// Standard Sound & Particles

		@BossParam(help = "Sound played at the start")
		public SoundsList SOUND_START = SoundsList.EMPTY;

		@BossParam(help = "Particle used when launching the projectile")
		public ParticlesList PARTICLE_LAUNCH = ParticlesList.EMPTY;

		@BossParam(help = "Sound used when launching the projectile")
		public SoundsList SOUND_LAUNCH = SoundsList.EMPTY;

		@BossParam(help = "Particle used for the projectile")
		public ParticlesList PARTICLE_PROJECTILE = ParticlesList.EMPTY;

		@BossParam(help = "Sound summoned every 2 sec on the projectile location")
		public SoundsList SOUND_PROJECTILE = SoundsList.EMPTY;

		@BossParam(help = "Particle used when the projectile hit something")
		public ParticlesList PARTICLE_HIT = ParticlesList.EMPTY;

		@BossParam(help = "Sound used when the projectile hit something")
		public SoundsList SOUND_HIT = SoundsList.EMPTY;

		// Unique parameters for a "entity projectile"
		@BossParam(help = "Maximum duration the projectile can exist (In ticks)")
		public int DURATION = 20 * 20;

		@BossParam(help = "The entity that rides the projectile. If empty, the boss becomes the projectile")
		public LoSPool ENTITY = LoSPool.LibraryPool.EMPTY;

		@BossParam(help = "Up offset of the entity riding the projectile")
		public double ENTITY_OFFSET = 0;

		@BossParam(help = "Up offset of the projectile aiming for the target")
		public double AIM_OFFSET = 0;

		@BossParam(help = "Sends a bosstag phase trigger to the entity riding the projectile")
		public String CUSTOM = "";

		@BossParam(help = "Percent speed applied to the projectile without having a target")
		public double SPEED_LINGER = 1;

		@BossParam(help = "Should the entity's AI be toggled off while riding the projectile?")
		public boolean TOGGLE_AI = true;

		@BossParam(help = "Should the boss shoot at the aggro'd target instead?")
		public boolean PREFER_TARGET = false;

		@BossParam(help = "If applicable, should the entity face the trajectory of the projectile?")
		public boolean FACE = true;

		@BossParam(help = "Should crowd control effects interrupt the projectile?")
		public boolean CC_INTERRUPT = true;

		@BossParam(help = "If the projectile ends, should the mob persist, be removed or be killed?")
		public Type TYPE = Type.PERSIST;

		@BossParam(help = "Should the projectile hit players?")
		public boolean COLLIDES_WITH_PLAYERS = true;

		@BossParam(help = "If player collision is enabled, should the projectile hit other players?")
		public boolean COLLIDE_OTHER_PLAYERS = false;

	}

	Parameters mParameters = new Parameters();

	public ProjectileEntityBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters.getParameters(boss, identityTag, mParameters);
		constructBoss(new SpellProjectileEntity(plugin, boss, mParameters), mParameters.DETECTION, null, mParameters.DELAY);
	}
}
