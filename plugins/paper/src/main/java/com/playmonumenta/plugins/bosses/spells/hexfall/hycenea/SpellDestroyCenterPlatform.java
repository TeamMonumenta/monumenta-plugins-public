package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.plugins.utils.FastUtils;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;

public class SpellDestroyCenterPlatform extends Spell {
	private final int mMinRadius;
	private final int mMaxRadius;
	private final Location mSpawnLoc;
	private final int mCooldown;

	public SpellDestroyCenterPlatform(Location spawnLoc, int minRadius, int maxRadius, int cooldown) {
		mMinRadius = minRadius;
		mMaxRadius = maxRadius;
		mSpawnLoc = spawnLoc;
		mCooldown = cooldown;
	}

	@Override
	public void run() {
		List<Block> blocksToRemove = new ArrayList<>();
		for (double deg = 0; deg < 360; deg += 1) {
			double cos = FastUtils.cosDeg(deg);
			double sin = FastUtils.sinDeg(deg);
			for (double rad = mMinRadius; rad < mMaxRadius; rad += 1) {
				Location l = mSpawnLoc.clone().add(cos * rad, -1, sin * rad);
				Block block = l.getBlock();
				Material material = block.getType();
				if (material != Material.BARRIER && material != Material.BEDROCK && material != Material.LIGHT) {
					blocksToRemove.add(l.getBlock());
				}
			}
		}

		for (Block block : blocksToRemove) {
			block.setType(Material.WATER, false);
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
