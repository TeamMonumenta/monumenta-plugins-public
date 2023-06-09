package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public class BlockBreakBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_blockbreak";
	public static final int detectionRange = 40;

	public static class Parameters extends BossParameters {
		@BossParam(help = "Whether or not the mob's bounding box affects the range of where blocks are destroyed")
		public boolean ADAPT_TO_BOUNDING_BOX = false;

		@BossParam(help = "Whether or not blocks at the same level as where the mob is standing will be destroyed")
		public boolean ALLOW_FOOTLEVEL_BREAK = false;
	}

	public BlockBreakBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		Parameters p = Parameters.getParameters(boss, identityTag, new Parameters());
		List<Spell> passiveSpells = List.of(new SpellBlockBreak(boss, p.ADAPT_TO_BOUNDING_BOX, p.ALLOW_FOOTLEVEL_BREAK));

		super.constructBoss(SpellManager.EMPTY, passiveSpells, detectionRange, null);
	}
}
