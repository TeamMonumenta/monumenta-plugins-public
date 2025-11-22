package com.playmonumenta.plugins.bosses.parameters.phases;

import com.playmonumenta.plugins.utils.PlayerUtils;
import java.util.List;
import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public class NearbyPlayersTrigger extends Trigger {
	public static final String IDENTIFIER = "NEARBY_PLAYERS";

	private final double mRadius;
	private final CompareOperation mOperation;
	private final int mNumberOfPlayers;
	private final boolean mLineOfSight;

	public NearbyPlayersTrigger(double radius, CompareOperation opp, int numPlayers, boolean lineOfSight) {
		mRadius = radius;
		mOperation = opp;
		mNumberOfPlayers = numPlayers;
		mLineOfSight = lineOfSight;
	}


	@Override
	public boolean test(LivingEntity boss) {
		return testPlayersInRange(boss.getLocation());
	}

	@Override
	public boolean tick(LivingEntity boss, int ticks) {
		return testPlayersInRangeWithNegation(boss.getLocation());
	}

	@Override
	public void reset(LivingEntity boss) {

	}

	private boolean testPlayersInRangeWithNegation(Location loc) {
		boolean result = testPlayersInRange(loc);
		return (result && !isNegated()) || (!result && isNegated());
	}

	private boolean testPlayersInRange(Location loc) {
		List<Player> players = PlayerUtils.playersInRange(loc, mRadius, true);
		if (mLineOfSight) {
			players.removeIf(player -> !player.hasLineOfSight(loc));
		}

		if (players.size() == mNumberOfPlayers && (mOperation == CompareOperation.EQUAL || mOperation == CompareOperation.LEQ || mOperation == CompareOperation.GEQ)) {
			return true;
		} else if (players.size() > mNumberOfPlayers && (mOperation == CompareOperation.GREATER || mOperation == CompareOperation.GEQ)) {
			return true;
		} else {
			return players.size() < mNumberOfPlayers && (mOperation == CompareOperation.LOWER || mOperation == CompareOperation.LEQ);
		}
	}

	public enum CompareOperation {
		EQUAL, GREATER, LOWER, LEQ, GEQ
	}

}
