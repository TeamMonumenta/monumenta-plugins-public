package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.parameters.EffectsList;
import com.playmonumenta.plugins.bosses.parameters.EntityTargets;
import com.playmonumenta.plugins.bosses.parameters.SoundsList;
import com.playmonumenta.plugins.bosses.spells.SpellVanguardChallenge;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class VanguardChallengeBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_vanguard_challenge";


	public static class Parameters extends BossParameters {
		@BossParam(help = "Delay before first cast")
		public int COOLDOWN = 20 * 20;
		@BossParam(help = "Delay before first cast")
		public int DELAY = 20;
		@BossParam(help = "Duration of force-look")
		public int DURATION = 4 * 20;
		@BossParam(help = "Ticks between force-looks")
		public int INTERVAL = 2;
		@BossParam(help = "Sound played on cast")
		public SoundsList SOUND = SoundsList.builder()
			.add(new SoundsList.CSound(Sound.ENTITY_ENDER_DRAGON_AMBIENT, 1.2f, 2.0f))
			.build();
		@BossParam(help = "Effects applied to affected targets")
		public EffectsList EFFECT_TARGET = EffectsList.EMPTY;
		@BossParam(help = "Effects applied to boss")
		public EffectsList EFFECT_BOSS = EffectsList.EMPTY;
		@BossParam(help = "not written")
		public EntityTargets TARGETS = EntityTargets.GENERIC_PLAYER_TARGET;
	}

	Parameters mParams = new Parameters();

	public VanguardChallengeBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParams = Parameters.getParameters(boss, identityTag, mParams);
		constructBoss(new SpellVanguardChallenge(plugin, boss, mParams), (int) mParams.TARGETS.getRange(), null, mParams.DELAY);
	}
}