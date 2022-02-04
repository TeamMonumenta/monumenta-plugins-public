package com.playmonumenta.plugins.bosses.bosses;

import java.util.Collections;
import java.util.List;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellTpBehindPlayer;

import com.playmonumenta.plugins.utils.EntityUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

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


	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new TpBehindBoss(plugin, boss);
	}

	public TpBehindBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells;

		if (EntityUtils.isFlyingMob(mBoss)) {
			//Flying mobs can't use this ability
			activeSpells = SpellManager.EMPTY;
		} else {
			activeSpells = new SpellManager(List.of(
				new SpellTpBehindPlayer(plugin, boss, p.COOLDOWN, p.RANGE, p.DELAY, p.STUN, p.RANDOM)
			));
		}

		super.constructBoss(activeSpells, Collections.emptyList(), p.DETECTION, null);
	}
}
