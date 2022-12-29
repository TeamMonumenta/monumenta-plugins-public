package com.playmonumenta.plugins.bosses.bosses;

import com.playmonumenta.plugins.Plugin;
import com.playmonumenta.plugins.bosses.SpellManager;
import java.util.Collections;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.metadata.FixedMetadataValue;

public class BlockLockBoss extends BossAbilityGroup {
	public static final String identityTag = "boss_lock_block";

	public static class Parameters extends BossParameters {
		public double X = 0;
		public double Y = 0;
		public double Z = 0;
	}

	final Parameters mParam;
	final Location mLoc;

	public BlockLockBoss(Plugin plugin, LivingEntity boss) {
		super(plugin, identityTag, boss);
		mParam = Parameters.getParameters(boss, identityTag, new Parameters());
		mLoc = new Location(mBoss.getWorld(), mParam.X, mParam.Y, mParam.Z);
		Block block = mLoc.getBlock();
		if (!block.hasMetadata("Unbreakable")) {
			block.setMetadata("Unbreakable", new FixedMetadataValue(plugin, true));
		}
		super.constructBoss(SpellManager.EMPTY, Collections.emptyList(), 40, null);
	}

	@Override
	public void unload() {
		mLoc.getBlock().removeMetadata("Unbreakable", mPlugin);
	}

	public static BossAbilityGroup deserialize(Plugin plugin, LivingEntity boss) throws Exception {
		return new BlockLockBoss(plugin, boss);
	}
}

