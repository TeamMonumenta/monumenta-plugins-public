package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.SpellHandSwap;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

public class HandSwapBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_handswap";

	@BossParam(help = "Swaps the bosses mainhand and offhand items periodically")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Detection range")
		public int DETECTION_RANGE = 35;
		@BossParam(help = "How often the swap is preformed")
		public int COOLDOWN = 140;
		@BossParam(help = "If the boss should swap weapon depending on distance to nearby players")
		public boolean SWAP_ON_RANGE = false;
		@BossParam(help = "The range which determines what weapon the boss uses, short range is default weapon")
		public int SWAP_RANGE = 5;
	}

	public HandSwapBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);

		HandSwapBoss.Parameters p = BossParameters.getParameters(boss, identityTag, new HandSwapBoss.Parameters());

		if (!(boss instanceof Mob mob)) {
			throw new Exception(identityTag + " only works on mobs! Entity name='" + boss.getName() + "', tags=[" + String.join(",", boss.getScoreboardTags()) + "]");
		}

		super.constructBoss(new SpellHandSwap(mob, p.COOLDOWN, p.SWAP_ON_RANGE, p.SWAP_RANGE), p.DETECTION_RANGE);
	}
}
