package com.playmonumenta.plugins.bosses.spells.hexfall.hycenea;

import com.playmonumenta.plugins.bosses.spells.Spell;
import com.playmonumenta.scriptedquests.growables.GrowableAPI;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.util.Vector;

public class SpellGrowableAtMarker extends Spell {
	private final LivingEntity mBoss;
	private final String mTargetTag;
	private final String mGrowableName;
	private final int mRange;
	private final Vector mOffset;
	private final int mCooldown;
	private final int mBlocksPerTick;

	public SpellGrowableAtMarker(LivingEntity boss, String targetTag, String growableName, int range, Vector offset, int cooldown, int blocksPerTick) {
		this.mBoss = boss;
		this.mTargetTag = targetTag;
		this.mGrowableName = growableName;
		this.mRange = range;
		this.mOffset = offset;
		this.mCooldown = cooldown;
		mBlocksPerTick = blocksPerTick;
	}

	@Override
	public void run() {
		for (Entity e : mBoss.getNearbyEntities(mRange, mRange, mRange)) {
			if (e.getScoreboardTags().contains(mTargetTag)) {
				GrowableAPI.grow(mGrowableName, e.getLocation().add(mOffset), 1, mBlocksPerTick, true);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
