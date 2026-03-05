package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellEruption;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;

public class EruptionBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_eruption";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Explosion radius of each projectile")
		public int EXPLOSION_RADIUS = 2;

		@BossParam(help = "not written")
		public int DELAY = 5 * 20;

		@BossParam(help = "damage that this spell deals to players")
		public int DAMAGE = 40;

		@BossParam(help = "effects given on hit")
		public EffectsList EFFECTS = EffectsList.EMPTY;

		@BossParam(help = "not written")
		public int COOLDOWN = 8 * 20;

		@BossParam(help = "how long the boss cahrges up for")
		public int CHARGE_DURATION = 2 * 20;

		@BossParam(help = "not written")
		public int DETECTION = 20;

		@BossParam(help = "how many projectiles are launched")
		public int PROJECTILE_COUNT = 3;

		@BossParam(help = "how many projectiles are launched")
		public double XZ_VELOCITY = 0.25;

		@BossParam(help = "how many projectiles are launched")
		public double Y_VELOCITY = 0.7;

		@BossParam(help = "base item that is thrown")
		public Material MATERIAL = Material.RED_STAINED_GLASS;

		@BossParam(help = "target of this spell")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET_LINE_OF_SIGHT;

		@BossParam(help = "Sound played at the targeted player when the boss starts charging the ability ability")
		public SoundsList SOUND_WARNING = SoundsList.fromString("[(ENTITY_ELDER_GUARDIAN_CURSE,5,0.75)]");

		@BossParam(help = "Sound played every tick at the caster while the spell is charging.")
		public SoundsList SOUND_CHARGE_BOSS = SoundsList.fromString("[(ENTITY_BLAZE_AMBIENT,1,0)]");

		@BossParam(help = "sound that plays when the boss explodes")
		public SoundsList SOUND_ERUPTION = SoundsList.fromString("[(ENTITY_GENERIC_EXPLODE,2,0.5)]");

		@BossParam(help = "sound played at each explosion")
		public SoundsList SOUND_EXPLOSIONS = SoundsList.fromString("[(ENTITY_GENERIC_EXPLODE,1.2,1.5)]");

		@BossParam(help = "Particles to spawn at the boss while the spell is charging.")
		public ParticlesList PARTICLES_CHARGE_BOSS = ParticlesList.fromString("[(LAVA,1,0.25,0.45,0.25,1)]");

		@BossParam(help = "Particles to spawn at the boss' feet as expanding circles. pick one with velocity!")
		public Particle PARTICLE_CHARGE_GROUND = Particle.FLAME;

		@BossParam(help = "Particles that travel along the projectile's trajectory")
		public ParticlesList PARTICLES_PROJECTILE = ParticlesList.fromString("[(FLAME,2,0.1,0.1,0.1,0.01),(SMALL_FLAME,1,0.1,0.1,0.1,0.01)]");

		@BossParam(help = "particles at the boss' head")
		public ParticlesList PARTICLES_EXPLOSION_BOSS = ParticlesList.fromString("[(LAVA,20,0.1,0.1,0.1,0)]");

		@BossParam(help = "radius indicator of each boom")
		public ParticlesList PARTICLES_EXPLOSION_BORDER = ParticlesList.fromString("[(SMOKE_NORMAL,1,0.1,0.1,0.1)]");

		@BossParam(help = "extra boom aesthetics played from the center")
		public ParticlesList PARTICLES_EXPLOSION = ParticlesList.fromString("[(LAVA,35,0.5,0.5,0.5)]");
	}

	public EruptionBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		Spell spell = new SpellEruption(plugin, boss, p);
		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
