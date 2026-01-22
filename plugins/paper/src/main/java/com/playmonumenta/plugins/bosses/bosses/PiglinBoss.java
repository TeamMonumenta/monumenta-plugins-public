package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import org.bukkit.entity.LivingEntity;

public class PiglinBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_piglin";

	// This exist for autofill purposes
	@SuppressWarnings("unused")
	public static class Parameters extends BossParameters {

		@BossParam(help = "If holding a crossbow, the minimum distance to strafe away from the target.")
		public int MIN_RANGED_DISTANCE = 5;

		@BossParam(help = "If holding a crossbow, the maximum distance before moving closer to the target.")
		public int MAX_RANGED_DISTANCE = 5;

		@BossParam(help = "The minimum time in ticks to load the crossbow.")
		public int MIN_SHOT_DELAY = 20;

		@BossParam(help = "The maximum time in ticks before loading the crossbow.")
		public int MAX_SHOT_DELAY = 40;

		@BossParam(help = "Attack speed of the Piglin in ticks. Applicable to Brutes.")
		public int MELEE_ATTACK_SPEED = 20;
	}

	public PiglinBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
	}
}
