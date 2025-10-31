package com.playmonumenta.plugins.bosses.bosses.exalted;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.bosses.BossAbilityGroup;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.exalted.SpellFlamingHavoc;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class FlamingHavocBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_flaminghavoc";
	public static final int detectionRange = 64;


	public FlamingHavocBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		List<Spell> passives = List.of(
			new SpellFlamingHavoc(mPlugin, mBoss, detectionRange)
		);

		super.constructBoss(SpellManager.EMPTY, passives, detectionRange, null);
	}
}
