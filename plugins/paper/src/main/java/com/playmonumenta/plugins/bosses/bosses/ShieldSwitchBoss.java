package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.SpellShieldSwitch;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

public final class ShieldSwitchBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_shieldswitch";

	public static class Parameters extends BossParameters {
		public int DETECTION = 35;
		public int DELAY = 5 * 20;
	}

	public ShieldSwitchBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob mob)) {
			throw new Exception(identityTag + " only works on mobs! Entity name='" + boss.getName() + "', tags=[" + String.join(",", boss.getScoreboardTags()) + "]");
		}

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		super.constructBoss(new SpellShieldSwitch(mob, plugin), p.DETECTION, null, p.DELAY);
	}
}
