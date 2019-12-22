package com.playmonumenta.plugins.bosses.spells;

import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.playmonumenta.plugins.bosses.utils.Utils;

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

	private LivingEntity mBoss;
	private double mRange;
	private Action mAction;

	public SpellPlayerAction(LivingEntity boss, double range, Action action) {
		mBoss = boss;
		mRange = range;
		mAction = action;
	}

	@Override
	public void run() {
		for (Player player : Utils.playersInRange(mBoss.getLocation(), mRange)) {
			mAction.run(player);
		}
	}

	@Override
	public int duration() {
		// TODO Auto-generated method stub
		return 0;
	}

}
