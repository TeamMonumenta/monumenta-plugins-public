package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.utils.PlayerUtils;

public class SpellPlayerAction extends Spell {
	@FunctionalInterface
	public interface Action {
		/**
		 * User function called once every two ticks while bolt is charging
		 * @param entity  The entity charging the bolt
		 * @param tick    Number of ticks since start of attack
		 *      NOTE - Only even numbers are returned here!
		 */
		void run(Player player);
	}

	@FunctionalInterface
	public interface TickAction {
		/**
		 * User function called once every two ticks while bolt is charging
		 * @param entity  The entity charging the bolt
		 * @param tick    Number of ticks since start of attack
		 *      NOTE - Only even numbers are returned here!
		 */
		void run(Player player, int tick);
	}

	private LivingEntity mBoss;
	private double mRange;
	private Action mAction;
	private TickAction mTickAction;
	private int mTicks;

	public SpellPlayerAction(LivingEntity boss, double range, Action action) {
		mBoss = boss;
		mRange = range;
		mAction = action;
		mTickAction = null;
	}

	public SpellPlayerAction(LivingEntity boss, double range, TickAction action) {
		mBoss = boss;
		mRange = range;
		mTickAction = action;
		mTicks = 0;
	}

	@Override
	public void run() {
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), mRange)) {
			if (mTickAction != null) {
				mTicks += 2;
				mTickAction.run(player, mTicks);
			} else {
				mAction.run(player);
			}
		}
	}

	@Override
	public int cooldownTicks() {
		// TODO Auto-generated method stub
		return 0;
	}

}
