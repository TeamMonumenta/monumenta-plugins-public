package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellDash;
import java.util.Collections;
import java.util.List;
import org.bukkit.Color;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class DashBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_dash";

	public static class Parameters extends BossParameters {
		@BossParam(help = "The base cooldown between each swing. Default: 100")
		public int COOLDOWN = 100;
		@BossParam(help = "The delay between spawn and first attack. Default: 10")
		public int DELAY = 10;
		@BossParam(help = "The y velocity of the jump. Default: 1")
		public double JUMP_VELOCITY = 1;
		@BossParam(help = "The x/z velocity of the jump. Default: 1")
		public double VELOCITY = 1;
		@BossParam(help = "detection range")
		public int DETECTION = 30;
		@BossParam(help = "minimum range the boss should be to cast this ability")
		public double MIN_RANGE = 1.0;
		@BossParam(help = "targets of the spell")
		public EntityTargets TARGET = EntityTargets.GENERIC_PLAYER_TARGET;
		@BossParam(help = "prefers aggro target")
		public boolean PREFER_TARGET = true;
		@BossParam(help = "The sound of the jump. Default: Pillager Celebrate")
		public SoundsList SOUND_START = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_PILLAGER_CELEBRATE, 1.0f, 1.1f))
			.build();
		@BossParam(help = "The sound played during the flight. Default: Pillager Celebrate")
		public SoundsList SOUND_LAND = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_HORSE_GALLOP, 1.3f, 0.0f))
			.add(new SoundsList.CSound(Sound.ENTITY_HORSE_GALLOP, 2.0f, 1.25f))
			.build();
		@BossParam(help = "The particles displayed at the start of the jump. Default: Cloud")
		public ParticlesList PARTICLE_START = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CLOUD, 15, 1.0, 0.0, 1.0, 0.0))
			.build();
		@BossParam(help = "The particles displayed in the air, during the jump. Default: White Redstone")
		public ParticlesList PARTICLE_AIR = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.REDSTONE, 4, 0.5, 0.5, 0.5, 1.0, new Particle.DustOptions(Color.WHITE, 1.0f)))
			.build();
		@BossParam(help = "The particles displayed upon landing. Default: Cloud")
		public ParticlesList PARTICLE_LAND = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CLOUD, 1, 0.1, 0.1, 0.1, 0.1))
			.build();
	}

	public final Parameters mParams;

	public DashBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		mParams = Parameters.getParameters(boss, identityTag, new Parameters());
		if (mParams.TARGET == EntityTargets.GENERIC_PLAYER_TARGET) {
			mParams.TARGET = new EntityTargets(EntityTargets.TARGETS.PLAYER, mParams.MIN_RANGE, EntityTargets.Limit.DEFAULT, List.of(EntityTargets.PLAYERFILTER.HAS_LINEOFSIGHT, EntityTargets.PLAYERFILTER.NOT_STEALTHED));
		}
		SpellManager activeSpells = new SpellManager(List.of(
			new SpellDash(plugin, boss, mParams.COOLDOWN, mParams.MIN_RANGE, mParams.TARGET, mParams.PREFER_TARGET, mParams.JUMP_VELOCITY, mParams.VELOCITY, mParams.SOUND_START, mParams.SOUND_LAND, mParams.PARTICLE_START, mParams.PARTICLE_AIR, mParams.PARTICLE_LAND)
		));

		super.constructBoss(activeSpells, Collections.emptyList(), mParams.DETECTION, null, mParams.DELAY);
	}
}