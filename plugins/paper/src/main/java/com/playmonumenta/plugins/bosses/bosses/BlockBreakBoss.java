package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

import java.util.Arrays;
import java.util.List;

public class BlockBreakBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_blockbreak";
	public static final int detectionRange = 40;

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BlockBreakBoss(plugin, boss);
	}

	public BlockBreakBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		List<Spell> passiveSpells = Arrays.asList(new SpellBlockBreak(boss));

		super.constructBoss(null, passiveSpells, detectionRange, null);
	}
}
