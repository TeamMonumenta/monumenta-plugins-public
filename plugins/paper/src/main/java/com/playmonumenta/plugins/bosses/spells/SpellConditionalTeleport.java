package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.Location;
import org.bukkit.entity.Entity;

import java.util.function.Predicate;

public class SpellConditionalTeleport extends Spell {
	private Entity mLauncher;
	private Location mDest;
	private Predicate<Entity> mPredicate;

	public SpellConditionalTeleport(Entity launcher, Location dest, Predicate<Entity> predicate) {
		mLauncher = launcher;
		mDest = dest;
		mPredicate = predicate;
	}

	@Override
	public void run() {
		if (mPredicate.test(mLauncher)) {
			/* TODO: This needs to be able to load the destination chunk if needed */
			mLauncher.teleport(mDest);
		}
	}

	@Override
	public int cooldownTicks() {
		return 1;
	}
}
