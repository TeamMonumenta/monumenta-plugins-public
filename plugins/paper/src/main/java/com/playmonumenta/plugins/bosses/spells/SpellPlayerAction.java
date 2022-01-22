package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.PlayerUtils;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class SpellPlayerAction extends Spell {
	@FunctionalInterface
	public interface Action {
		/**
		 * User function called once every two ticks while the spell is active
		 *
		 * @param player The targeted player
		 */
		void run(Player player);
	}

	@FunctionalInterface
	public interface TickAction {
		/**
		 * User function called once every two ticks while the spell is active
		 *
		 * @param player The targeted player
		 * @param tick   Number of ticks since start of attack
		 *               NOTE - Only even numbers are returned here!
		 */
		void run(Player player, int tick);
	}

	private final LivingEntity mBoss;
	private final double mRange;
	private final TickAction mTickAction;
	private int mTicks = 0;

	public SpellPlayerAction(LivingEntity boss, double range, Action action) {
		mBoss = boss;
		mRange = range;
		mTickAction = (player, tick) -> action.run(player);
	}

	public SpellPlayerAction(LivingEntity boss, double range, TickAction action) {
		mBoss = boss;
		mRange = range;
		mTickAction = action;
	}

	@Override
	public void run() {
		for (Player player : PlayerUtils.playersInRange(mBoss.getLocation(), mRange, true)) {
			mTicks += 2;
			mTickAction.run(player, mTicks);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
