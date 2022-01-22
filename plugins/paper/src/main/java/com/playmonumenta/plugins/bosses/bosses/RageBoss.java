package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.SpellRage;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;

public class RageBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_rage";
	public static final int detectionRange = 20;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new RageBoss(plugin, boss);
	}

	public RageBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);

		SpellManager activeSpells = new SpellManager(Arrays.asList(
			new SpellRage(plugin, boss, 12, 30)
		));

		super.constructBoss(activeSpells, null, detectionRange, null);
	}
}
