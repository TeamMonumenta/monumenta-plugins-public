package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import com.playmonumenta.plugins.bosses.parameters.BossParam;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;

public final class BlockLockBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lock_block";

	public static class Parameters extends BossParameters {
		@BossParam(help = "Range in blocks that players must be in before this passive spell is instantiated. This spell does not run periodically")
		public int DETECTION = 40;

		@BossParam(help = "X coordinate of the block to lock")
		public double X = 0;

		@BossParam(help = "Y coordinate of the block to lock")
		public double Y = 0;

		@BossParam(help = "Z coordinate of the block to lock")
		public double Z = 0;
	}

	private static final String METADATA_KEY = "Unbreakable";
	private final Location mLoc;

	public BlockLockBoss(final Plugin plugin, final LivingEntity boss) {
		super(plugin, identityTag, boss);
		final Parameters param = Parameters.getParameters(mBoss, identityTag, new Parameters());
		mLoc = new Location(mBoss.getWorld(), param.X, param.Y, param.Z);
		final Block block = mLoc.getBlock();
		if (!block.hasMetadata(METADATA_KEY)) {
			block.setMetadata(METADATA_KEY, new FixedMetadataValue(mPlugin, true));
		}
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), param.DETECTION, null);
	}

	@Override
	public void unload() {
		super.unload();
		mLoc.getBlock().removeMetadata(METADATA_KEY, mPlugin);
	}
}
