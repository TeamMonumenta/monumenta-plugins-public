package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellThrowSummon;
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
		public EntityTargets TARGETS = EntityTargets.GENERIC_ONE_PLAYER_CLOSER_TARGET.setOptional(false).setRange(8);

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

		@BossParam(help = "Particles played when the boss throws a mob")
		public ParticlesList THROW_PARTICLE = ParticlesList.fromString("[(FLAME,10,0.2,0.2,0.2,0.1)]");

		@BossParam(help = "Sound played when the boss throws a mob")
		public SoundsList THROW_SOUND = SoundsList.fromString("[(ENTITY_SHULKER_SHOOT,1,1)]");

	}

	public ThrowSummonBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		EntityTargets targets = p.TARGETS;
		if (p.RADIUS != 8) {
			targets = EntityTargets.GENERIC_ONE_PLAYER_CLOSER_TARGET.setOptional(false).setRange(p.RADIUS);
		}

		Spell spell = new SpellThrowSummon(plugin, boss, targets, p.LOBS, p.COOLDOWN, p.SPAWNEDMOB, p.POOL, p.LOB_DELAY,
			p.HEIGHT_OFFSET, p.Y_VELOCITY, p.THROW_VARIANCE, p.THROW_Y_VARIANCE, p.DISTANCE_SCALAR, p.MOB_CAP_RANGE, p.MOB_CAP,
			p.THROW_PARTICLE, p.THROW_SOUND);


		super.constructBoss(spell, p.DETECTION, null, p.DELAY);
	}
}
