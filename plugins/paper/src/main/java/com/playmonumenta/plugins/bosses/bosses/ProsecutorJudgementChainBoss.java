package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.ParticlesList;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellProsecutorJudgementChain;
import com.playmonumenta.plugins.events.DamageEvent;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class ProsecutorJudgementChainBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_prosecutor_judgement_chain";


	public static class Parameters extends BossParameters {
		@BossParam(help = "")
		public String ABILITY_NAME = "Judgement Chain";
		@BossParam(help = "Velocity applied to the players each pull")
		public double PULL_VELOCITY = 0.2;
		@BossParam(help = "Cooldown in between casts")
		public int COOLDOWN = 30 * 20;
		@BossParam(help = "Delay before first cast")
		public int DELAY = 20;
		@BossParam(help = "How long the chain lasts for")
		public int PULL_TIME = 8 * 20;
		@BossParam(help = "The number of times another player has to run through a chain to break it")
		public int BREAK_REQUIREMENT = 1;
		@BossParam(help = "Ticks in between pulls")
		public int PULL_FREQUENCY = 2;
		@BossParam(help = "Whether it should cancel on damage")
		public boolean CANCEL_ON_DAMAGE = false;
		@BossParam(help = "What damage type it should cancel on.")
		public DamageEvent.DamageType DAMAGE_TYPE = DamageEvent.DamageType.MELEE;
		@BossParam(help = "Particles of the chain")
		public ParticlesList PARTICLE_CHAIN = ParticlesList.builder()
			.add(new ParticlesList.CParticle(Particle.CRIT_MAGIC, 50))
			.build();
		@BossParam(help = "Sound played each pull")
		public SoundsList SOUND_PULL = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_CHAIN_BREAK, 2.0f, 0.5f))
			.build();
		@BossParam(help = "Sound played each break")
		public SoundsList SOUND_CHIP = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ITEM_AXE_SCRAPE, 0.5f, 1.2f))
			.build();
		@BossParam(help = "Sound played each break")
		public SoundsList SOUND_BREAK = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.BLOCK_RESPAWN_ANCHOR_DEPLETE, 1.2f, 2.0f))
			.build();
		@BossParam(help = "not written")
		public EntityTargets TARGETS = EntityTargets.GENERIC_ONE_PLAYER_CLOSER_TARGET;
	}

	Parameters mParams = new Parameters();

	public ProsecutorJudgementChainBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters.getParameters(boss, identityTag, mParams);
		constructBoss(new SpellProsecutorJudgementChain(plugin, boss, mParams), (int) mParams.TARGETS.getRange(), null, mParams.DELAY);
	}
}