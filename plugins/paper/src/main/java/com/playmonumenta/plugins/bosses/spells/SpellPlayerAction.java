package com.playmonumenta.plugins.bosses.spells;

import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.Collection;
import java.util.function.Supplier;
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

	private final Supplier<Collection<Player>> mPlayerSupplier;
	private final TickAction mTickAction;
	private int mTicks = 0;

	public SpellPlayerAction(LivingEntity boss, double range, Action action) {
		this(boss, range, (player, tick) -> action.run(player));
	}

	public SpellPlayerAction(LivingEntity boss, double range, TickAction action) {
		this(() -> PlayerUtils.playersInRange(boss.getLocation(), range, true), action);
	}

	public SpellPlayerAction(Supplier<Collection<Player>> supplier, Action action) {
		this(supplier, (player, tick) -> action.run(player));
	}

	public SpellPlayerAction(Supplier<Collection<Player>> supplier, TickAction action) {
		mPlayerSupplier = supplier;
		mTickAction = action;
	}

	@Override
	public void run() {
		for (Player player : mPlayerSupplier.get()) {
			mTicks += 2;
			mTickAction.run(player, mTicks);
		}
	}

	@Override
	public int cooldownTicks() {
		return 0;
	}

}
