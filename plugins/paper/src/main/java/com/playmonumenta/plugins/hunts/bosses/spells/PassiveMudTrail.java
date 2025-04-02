package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;

public class PassiveMudTrail extends Spell {
	private static final int MUD_TIME = 10 * 20;
	private static final double HITBOX_SIZE_MULT = 0.7;

	private final LivingEntity mBoss;
	private final ExperimentSeventyOne mExperimentSeventyOne;

	private int mTicks = 0;

	public PassiveMudTrail(LivingEntity boss, ExperimentSeventyOne experimentSeventyOne) {
		mBoss = boss;
		mExperimentSeventyOne = experimentSeventyOne;
	}

	@Override
	public void run() {
		if (mBoss.isDead()) {
			return;
		}

		if (mTicks == 5) {
			mTicks = 0;

			double hitboxSizeX = mBoss.getBoundingBox().getWidthX() * HITBOX_SIZE_MULT;
			double hitboxSizeZ = mBoss.getBoundingBox().getWidthZ() * HITBOX_SIZE_MULT;
			for (int x = 0; x <= Math.ceil(hitboxSizeX); x++) {
				for (int z = 0; z <= Math.ceil(hitboxSizeZ); z++) {
					Block mudBlock = mBoss.getLocation().clone().add(-hitboxSizeX / 2 + x, -0.5, -hitboxSizeZ / 2 + z).getBlock();
					if (mudBlock.getType().isSolid() && !mExperimentSeventyOne.isWormSpawner(mudBlock)) {
						mExperimentSeventyOne.placeMudBlock(mudBlock, MUD_TIME);
					}
				}
			}
		}
		mTicks++;
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}
}
