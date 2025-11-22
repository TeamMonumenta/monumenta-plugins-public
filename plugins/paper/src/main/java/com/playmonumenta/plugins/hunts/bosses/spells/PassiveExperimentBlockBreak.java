package com.playmonumenta.plugins.hunts.bosses.spells;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.hunts.bosses.ExperimentSeventyOne;
import com.playmonumenta.plugins.particle.PartialParticle;
import com.playmonumenta.plugins.utils.BlockUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.LivingEntity;
import org.bukkit.inventory.ItemStack;

public class PassiveExperimentBlockBreak extends Spell {

	private final LivingEntity mBoss;
	private final World mWorld;
	private final ExperimentSeventyOne mExperimentSeventyOne;

	private int mTicks = 0;

	public PassiveExperimentBlockBreak(LivingEntity boss, ExperimentSeventyOne experimentSeventyOne) {
		mBoss = boss;
		mWorld = boss.getWorld();
		mExperimentSeventyOne = experimentSeventyOne;
	}

	@Override
	public void run() {
		if (mTicks == 5) {
			mTicks = 0;

			List<Block> blocksToBreak = new ArrayList<>();
			double hitboxSizeX = mBoss.getBoundingBox().getWidthX();
			double hitboxSizeZ = mBoss.getBoundingBox().getWidthZ();
			for (int x = 0; x <= Math.ceil(hitboxSizeX); x++) {
				for (int z = 0; z <= Math.ceil(hitboxSizeZ); z++) {
					Block contactBlock = mBoss.getLocation().clone().add(-hitboxSizeX / 2 + x, 0.25, -hitboxSizeZ / 2 + z).getBlock();
					if (!contactBlock.isSolid() && !contactBlock.isEmpty() && !mExperimentSeventyOne.isPlacedMud(contactBlock)) {
						blocksToBreak.add(contactBlock);
					}
				}
			}
			if (!blocksToBreak.isEmpty()) {
				for (Block block : blocksToBreak) {
					if (BlockUtils.isValuableBlock(block.getType())) {
						block.breakNaturally(new ItemStack(Material.IRON_PICKAXE));
					} else {
						block.setType(Material.AIR);
					}

					mWorld.playSound(mBoss.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, SoundCategory.BLOCKS, 0.2f, 1.1f);
					new PartialParticle(Particle.EXPLOSION_NORMAL, mBoss.getLocation())
						.count(3)
						.delta(1)
						.extra(0.03)
						.spawnAsBoss();
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
