package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.bosses.spells.SpellBlockBreak;
import java.util.List;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;

public final class BlockBreakBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_blockbreak";

	@BossParam(help = "The launcher gains the ability to break blocks that obstructs its path. Certain blocks cannot be broken")
	public static class Parameters extends BossParameters {
		@BossParam(help = "Range in blocks that players must be in before this passive spell will run")
		public int DETECTION = 40;

		@BossParam(help = "Whether or not the mob's bounding box affects the range of where blocks are destroyed")
		public boolean ADAPT_TO_BOUNDING_BOX = false;

		@BossParam(help = "Whether or not blocks at the same level as where the mob is standing will be destroyed")
		public boolean ALLOW_FOOTLEVEL_BREAK = false;

		@BossParam(help = "")
		public int X_RADIUS = 1;

		@BossParam(help = "")
		public int Y_RADIUS = 3;

		@BossParam(help = "")
		public int Z_RADIUS = 1;

		@BossParam(help = "Vertical offset for block break volume")
		public int Y_OFFSET = 0;

		@BossParam(help = "How often the block break is casted")
		public int COOLDOWN = PASSIVE_RUN_INTERVAL_DEFAULT;
	}

	public BlockBreakBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		final Parameters p = Parameters.getParameters(mBoss, identityTag, new Parameters());
		final List<Spell> passiveSpells = List.of(
			new SpellBlockBreak(mBoss, p.X_RADIUS, p.Y_RADIUS, p.Z_RADIUS, p.Y_OFFSET, mBoss.getWorld().getMinHeight() - 100, p.ADAPT_TO_BOUNDING_BOX, true, p.ALLOW_FOOTLEVEL_BREAK)
		);

		super.constructBoss(SpellManager.EMPTY, passiveSpells, p.DETECTION, null, 0, p.COOLDOWN);
	}
}
