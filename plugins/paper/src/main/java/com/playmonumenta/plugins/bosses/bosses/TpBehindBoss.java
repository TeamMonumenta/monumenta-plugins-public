package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;

public final class TpBehindBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_tpbehind";

	public static class Parameters extends BossParameters {
		public int STUN = 10;
		public int RANGE = 80;
		public int DELAY = 50;
		public int DETECTION = 20;

		/*Choose if the player should chosen random, or the mob target */
		public boolean RANDOM = true;

		public int COOLDOWN = 12 * 20;

	}

	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TpBehindBoss(plugin, boss);
	}

	public TpBehindBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellTpBehindPlayer(plugin, boss, p.COOLDOWN, p.RANGE, p.DELAY, p.STUN, p.RANDOM)
		));


		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}
