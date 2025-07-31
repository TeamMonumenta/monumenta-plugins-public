package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellThrowSummon;
import java.util.List;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class ThrowSummonBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_throwsummon";

	public static class Parameters extends BossParameters {
		@BossParam(help = "not written")
		public int LOBS = 1;

		@BossParam(help = "not written", deprecated = true)
		public double RADIUS = 8;
		@BossParam(help = "targets of the ability")
		public EntityTargets TARGETS = EntityTargets.GENERIC_ONE_PLAYER_CLOSER_TARGET.clone().setFilters(List.of(EntityTargets.PLAYERFILTER.NOT_STEALTHED)).setRange(8);

		@BossParam(help = "toggle for requiring line of sight")
		public boolean LINE_OF_SIGHT = false;

		@BossParam(help = "not written")
		public int DETECTION = 20;

		@BossParam(help = "not written")
		public int COOLDOWN = 160;

		@BossParam(help = "not written")
		public String SPAWNEDMOB = "LostSoul";

		@BossParam(help = "Is the spawnedmob from a pool?")
		public boolean POOL = false;

		@BossParam(help = "Delay of the spell")
		public int DELAY = 100;

		@BossParam(help = "Delay between each mob throw, in ticks")
		public int LOB_DELAY = 15;

		@BossParam(help = "y-offset of where the mob will be thrown from")
		public double HEIGHT_OFFSET = 0;

		@BossParam(help = "Y-velocity of the thrown mob")
		public float Y_VELOCITY = 0.7f;

		@BossParam(help = "Variance of where the mob will be thrown")
		public double THROW_VARIANCE = 0.0;
		@BossParam(help = "Variance of how high the mob will be thrown")
		public double THROW_Y_VARIANCE = 0.0;

		@BossParam(help = "Scalar of how far the mob will be thrown (helpful for low grav entities)")
		public double DISTANCE_SCALAR = 1.0;

		@BossParam(help = "don't throw more mobs if there are at least this many mobs nearby in a [mobcaprange] range already")
		public int MOB_CAP_RANGE = 10;
		@BossParam(help = "don't throw more mobs if there are at least [mobcap] many mobs nearby already")
		public int MOB_CAP = 15;
		@BossParam(help = "whether or not to cap mobs by name (only for mobs, not pools of mobs")
		public boolean CAP_MOBS_BY_NAME = false;
		@BossParam(help = "name of the mob that the cap checks for. Spaces included!")
		public String MOB_CAP_NAME = "";

		@BossParam(help = "whether or not to remove any summoned entities when the boss dies")
		public boolean REMOVE_ON_DEATH = false;

		@BossParam(help = "Particles played when the boss throws a mob")
		public ParticlesList THROW_PARTICLE = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.FLAME, 10, 0.2, 0.2, 0.2, 0.1))
			.build();

		@BossParam(help = "Sound played when the boss throws a mob")
		public SoundsList THROW_SOUND = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_SHULKER_SHOOT, 1.0f, 1.0f))
			.build();
	}

	public ThrowSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		EntityTargets targets = p.TARGETS;

		//Super hacky fix since trying to do it on the target part of the tag didn't work. no idea why
		if (p.LINE_OF_SIGHT) {
			targets = targets.setFilters(List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT));
		}

		//This used to overwrite any targeting info if the range wasn't 8. Wack
		if (p.RADIUS != 8) {
			targets = targets.setFilters(List.of(EntityTargets.PLAYERFILTER.NOT_STEALTHED)).setRange(p.RADIUS);
		}

		Spell spell = new SpellThrowSummon(plugin, boss, targets, p.LOBS, p.COOLDOWN, p.SPAWNEDMOB, p.POOL, p.LOB_DELAY,
			p.HEIGHT_OFFSET, p.Y_VELOCITY, p.THROW_VARIANCE, p.THROW_Y_VARIANCE, p.DISTANCE_SCALAR, p.MOB_CAP_RANGE, p.MOB_CAP, p.CAP_MOBS_BY_NAME, p.MOB_CAP_NAME, p.REMOVE_ON_DEATH,
			p.THROW_PARTICLE, p.THROW_SOUND);


		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}