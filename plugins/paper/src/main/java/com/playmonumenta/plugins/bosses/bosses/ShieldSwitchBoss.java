package com.playmonumenta.plugins.bosses.bosses;

import java.util.Arrays;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.plugin.Plugin;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellShieldSwitch;
import com.playmonumenta.plugins.utils.BossUtils;

public class ShieldSwitchBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_shieldswitch";

	public static class Parameters extends BossParameters {
		public int DETECTION = 35;
		public int DELAY = 5 * 20;
	}

	Mob mBoss;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new ShieldSwitchBoss(plugin, boss);
	}

	public ShieldSwitchBoss(Plugin plugin, LivingEntity boss) throws Exception {
		super(plugin, identityTag, boss);
		if (!(boss instanceof Mob)) {
			throw new Exception("boss_shieldswitch only works on mobs!");
		}

		mBoss = (Mob)boss;

		Parameters p = BossParameters.getParameters(boss, identityTag, new Parameters());

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellShieldSwitch(mBoss, plugin)
		));

		super.constructBoss(activeSpells, null, p.DETECTION, null, p.DELAY);
	}
}
