package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import java.util.Arrays;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class BlockBreakBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_blockbreak";
	public static final int detectionRange = 40;

	public static class Parameters extends BossParameters {
		public boolean ADAPT_TO_BOUNDING_BOX = false;
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BlockBreakBoss(plugin, boss);
	}

	public BlockBreakBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		List<Spell> passiveSpells = Arrays.asList(p.ADAPT_TO_BOUNDING_BOX ? new SpellBlockBreak(boss, true) : new SpellBlockBreak(boss));

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}
