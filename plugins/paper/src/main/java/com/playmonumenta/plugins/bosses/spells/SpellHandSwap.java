package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.Mob;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;

public class SpellHandSwap extends Spell {
	private final Mob mBoss;
	private final int mCooldown;
	private final boolean mSwapOnRange;
	private final int mSwapRange;

	private State mState;

	public enum State {
		CLOSE_RANGE,
		LONG_RANGE,
	}

	public SpellHandSwap(Mob boss, int cooldown, boolean swapOnRange, int swapRange) {
		mBoss = boss;
		mCooldown = cooldown;
		mSwapOnRange = swapOnRange;
		mSwapRange = swapRange;

		mState = State.CLOSE_RANGE;
	}

	@Override
	public void run() {

		if (!mSwapOnRange) {
			swap();
			return;
		}

		if (mBoss.getTarget() == null) {
			return;
		}

		double playerDistance = mBoss.getTarget().getLocation().distanceSquared(mBoss.getLocation());

		if (playerDistance > mSwapRange * mSwapRange && mState == State.CLOSE_RANGE) {
			mState = State.LONG_RANGE;
			swap();
		}
		if (playerDistance < mSwapRange * mSwapRange && mState == State.LONG_RANGE) {
			mState = State.CLOSE_RANGE;
			swap();
		}
	}

	private void swap() {

		EntityEquipment equipment = mBoss.getEquipment();

		if (equipment.getItemInMainHand().isEmpty() && equipment.getItemInOffHand().isEmpty()) {
			return;
		}

		ItemStack curItem = equipment.getItemInMainHand();
		ItemStack offItem = equipment.getItemInOffHand();
		equipment.setItemInMainHand(offItem);
		equipment.setItemInOffHand(curItem);
	}

	@Override
	public int cooldownTicks() {
		return mCooldown;
	}
}
